package wpn.hdri.ss.writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wpn.hdri.ss.configuration.MariaDbConfiguration;
import wpn.hdri.ss.event.*;

import java.sql.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persists domain events to MariaDB in ERPNext-compatible tables.
 *
 * <ul>
 *   <li>{@code AvailabilityTransitioned} → INSERT into {@code tabState Transition}</li>
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
        try (PreparedStatement ps = connection().prepareStatement(INSERT_TRANSITION)) {
            ps.setString(1, newName());
            ps.setInt(2, t.attributeId());
            ps.setString(3, attributeNames.getOrDefault(t.attributeId(), "attr-" + t.attributeId()));
            ps.setString(4, t.from().name());
            ps.setString(5, t.to().name());
            ps.setTimestamp(6, Timestamp.from(t.timestamp()));
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
