package wpn.hdri.ss.writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wpn.hdri.ss.configuration.MariaDbConfiguration;
import wpn.hdri.ss.event.*;

import static wpn.hdri.ss.event.AvailabilityState.*;

import java.sql.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persists domain events to MariaDB in ERPNext-compatible tables.
 *
 * <ul>
 *   <li>{@code AvailabilityTransitioned} → INSERT into {@code tabState Transition}
 *                                          + UPSERT into {@code tabCurrent State}</li>
 *   <li>{@code DowntimeOpened}           → INSERT into {@code tabDowntime Interval} (closed_at = NULL)</li>
 *   <li>{@code DowntimeClosed}           → UPDATE {@code tabDowntime Interval} SET closed_at, duration_seconds</li>
 * </ul>
 *
 * <p>Uses a single JDBC connection with automatic reconnect on failure.
 * Call {@link #registerAttribute(int, String)} for each monitored attribute after
 * the engine starts so that human-readable names are stored alongside IDs.
 */
public class MariaDbSink implements EventSink<DomainEvent>, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(MariaDbSink.class);

    private static final String UPSERT_CURRENT_STATE =
            "INSERT INTO `tabCurrent State` " +
            "(name, creation, modified, modified_by, owner, docstatus, idx, " +
            " attribute_id, attribute_name, state, since) " +
            "VALUES (?, NOW(6), NOW(6), 'status-server', 'status-server', 1, 0, ?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE " +
            "  state = VALUES(state), since = VALUES(since), modified = NOW(6)";

    private static final String INSERT_TRANSITION =
            "INSERT INTO `tabState Transition` " +
            "(name, creation, modified, modified_by, owner, docstatus, idx, " +
            " attribute_id, attribute_name, from_state, to_state, transitioned_at) " +
            "VALUES (?, NOW(6), NOW(6), 'status-server', 'status-server', 1, 0, ?, ?, ?, ?, ?)";

    private static final String INSERT_DOWNTIME =
            "INSERT INTO `tabDowntime Interval` " +
            "(name, creation, modified, modified_by, owner, docstatus, idx, " +
            " attribute_id, attribute_name, opened_at) " +
            "VALUES (?, NOW(6), NOW(6), 'status-server', 'status-server', 1, 0, ?, ?, ?)";

    private static final String UPDATE_DOWNTIME =
            "UPDATE `tabDowntime Interval` SET closed_at = ?, duration_seconds = ?, modified = NOW(6) " +
            "WHERE attribute_id = ? AND closed_at IS NULL " +
            "ORDER BY opened_at DESC LIMIT 1";

    private final MariaDbConfiguration config;
    private final Map<Integer, String> attributeNames = new ConcurrentHashMap<>();

    private Connection connection;

    public MariaDbSink(MariaDbConfiguration config) {
        this.config = config;
    }

    /**
     * Reads all rows from {@code tabCurrent State} and returns them keyed by attribute_id.
     * Used at startup to restore {@link wpn.hdri.ss.engine2.AvailabilityAnalyzer} state.
     */
    public Map<Integer, CurrentState> loadCurrentStates() throws SQLException {
        Map<Integer, CurrentState> result = new HashMap<>();
        String sql = "SELECT attribute_id, state, since FROM `tabCurrent State`";
        try (PreparedStatement ps = connection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("attribute_id");
                AvailabilityState state = AvailabilityState.valueOf(rs.getString("state"));
                Instant since = rs.getTimestamp("since").toInstant();
                result.put(id, new CurrentState(id, state, since));
            }
        }
        logger.info("Loaded {} persisted attribute state(s) from tabCurrent State", result.size());
        return result;
    }

    /** Snapshot of a persisted attribute availability state. */
    public record CurrentState(int attributeId, AvailabilityState state, Instant since) {}

    /** Called after engine start so attribute names appear in DB records. */
    public void registerAttribute(int id, String fullName) {
        attributeNames.put(id, fullName);
    }

    @Override
    public void onEvent(DomainEvent event) {
        try {
            switch (event) {
                case AvailabilityTransitioned t -> insertTransition(t);
                case DowntimeOpened o           -> insertDowntime(o);
                case DowntimeClosed c           -> closeDowntime(c);
            }
        } catch (SQLException e) {
            logger.error("Failed to persist {}: {}", event.getClass().getSimpleName(), e.getMessage(), e);
            resetConnection();
        }
    }

    @Override
    public void close() {
        if (connection != null) {
            try { connection.close(); } catch (SQLException ignored) {}
        }
    }

    @Override
    public String name() { return "MariaDb"; }

    // --- private ---

    private void insertTransition(AvailabilityTransitioned t) throws SQLException {
        String attrName = attributeNames.getOrDefault(t.attributeId(), "attr-" + t.attributeId());
        Timestamp ts = Timestamp.from(t.timestamp());
        Connection conn = connection();

        try (PreparedStatement ps = conn.prepareStatement(INSERT_TRANSITION)) {
            ps.setString(1, newName());
            ps.setInt(2, t.attributeId());
            ps.setString(3, attrName);
            ps.setString(4, t.from().name());
            ps.setString(5, t.to().name());
            ps.setTimestamp(6, ts);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement(UPSERT_CURRENT_STATE)) {
            ps.setString(1, "CS-" + t.attributeId());
            ps.setInt(2, t.attributeId());
            ps.setString(3, attrName);
            ps.setString(4, t.to().name());
            ps.setTimestamp(5, ts);
            ps.executeUpdate();
        }
    }

    private void insertDowntime(DowntimeOpened o) throws SQLException {
        try (PreparedStatement ps = connection().prepareStatement(INSERT_DOWNTIME)) {
            ps.setString(1, newName());
            ps.setInt(2, o.attributeId());
            ps.setString(3, attributeNames.getOrDefault(o.attributeId(), "attr-" + o.attributeId()));
            ps.setTimestamp(4, Timestamp.from(o.timestamp()));
            ps.executeUpdate();
        }
    }

    private void closeDowntime(DowntimeClosed c) throws SQLException {
        try (PreparedStatement ps = connection().prepareStatement(UPDATE_DOWNTIME)) {
            ps.setTimestamp(1, Timestamp.from(c.timestamp()));
            ps.setBigDecimal(2, java.math.BigDecimal.valueOf(c.duration().toMillis() / 1000.0));
            ps.setInt(3, c.attributeId());
            int updated = ps.executeUpdate();
            if (updated == 0) {
                logger.warn("DowntimeClosed for attr {} found no open interval to close", c.attributeId());
            }
        }
    }

    private Connection connection() throws SQLException {
        if (connection == null || !connection.isValid(1)) {
            logger.info("Connecting to MariaDB at {}", config.jdbcUrl());
            connection = DriverManager.getConnection(config.jdbcUrl(), config.getUser(), config.getPassword());
            connection.setAutoCommit(true);
            logger.info("MariaDB connection established");
        }
        return connection;
    }

    private void resetConnection() {
        if (connection != null) {
            try { connection.close(); } catch (SQLException ignored) {}
            connection = null;
        }
    }

    private static String newName() {
        return UUID.randomUUID().toString();
    }
}
