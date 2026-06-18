package wpn.hdri.ss.engine2;

import org.junit.Before;
import org.junit.Test;
import wpn.hdri.ss.data.Method;
import wpn.hdri.ss.data2.Attribute;
import wpn.hdri.ss.data2.Interpolation;
import wpn.hdri.ss.data2.SingleRecord;
import wpn.hdri.ss.event.AboveMaxClosed;
import wpn.hdri.ss.event.AboveMaxOpened;
import wpn.hdri.ss.event.BelowMinClosed;
import wpn.hdri.ss.event.BelowMinOpened;
import wpn.hdri.ss.event.DomainEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class RangeAnalyzerTest {

    private static final double MIN = 200.0;
    private static final double MAX = 100.0;
    private static final double BOTH_MIN = 10.0;
    private static final double BOTH_MAX = 50.0;

    private static final int BEAM_CURRENT_ID = 7;
    private static final int TEMPERATURE_ID = 8;
    private static final int PRESSURE_ID = 9;
    private static final int PRESSURE_2_ID = 10;
    private static final int OTHER_ID = 99;

    private static final Attribute<Double> BEAM_CURRENT = attr(BEAM_CURRENT_ID, "BeamCurrent");
    private static final Attribute<Double> TEMPERATURE = attr(TEMPERATURE_ID, "Temperature");
    private static final Attribute<Double> PRESSURE = attr(PRESSURE_ID, "Pressure");
    private static final Attribute<Double> PRESSURE_2 = attr(PRESSURE_2_ID, "Pressure");
    private static final Attribute<Double> OTHER_ATTR = attr(OTHER_ID, "Other");

    private static Attribute<Double> attr(int id, String name) {
        return new Attribute<>(id, null, 0L, Method.EventType.NONE, Double.class,
                name, "/PETRA/Globals/#keyword/" + name, name, Interpolation.LAST);
    }

    private List<DomainEvent> emitted;
    private RangeAnalyzer analyzer;

    @Before
    public void setUp() {
        emitted = new ArrayList<>();
        Map<String, RangeAnalyzer.Bounds> bounds = Map.of(
                "BeamCurrent", new RangeAnalyzer.Bounds(MIN, null),
                "Temperature", new RangeAnalyzer.Bounds(null, MAX),
                "Pressure", new RangeAnalyzer.Bounds(BOTH_MIN, BOTH_MAX));
        analyzer = new RangeAnalyzer(bounds, emitted::add);
    }

    // --- helpers ---

    private void reading(Attribute<Double> attribute, double value, long timestampMs) {
        analyzer.onEvent(new SingleRecord<>(attribute, timestampMs, timestampMs, value));
    }

    // --- min-only bound (BeamCurrent) ---

    @Test
    public void staysSilentWhileAboveMin() {
        reading(BEAM_CURRENT, 250.0, 1000L);
        assertTrue(emitted.isEmpty());
    }

    @Test
    public void emitsBelowMinOpenedOnCrossingBelowMin() {
        reading(BEAM_CURRENT, 250.0, 1000L);
        reading(BEAM_CURRENT, 150.0, 2000L);

        assertEquals(1, emitted.size());
        BelowMinOpened opened = (BelowMinOpened) emitted.get(0);
        assertEquals(BEAM_CURRENT_ID, opened.attributeId());
        assertEquals(150.0, opened.value(), 0.0001);
    }

    @Test
    public void doesNotReEmitBelowMinWhileStayingBelowMin() {
        reading(BEAM_CURRENT, 150.0, 1000L);
        reading(BEAM_CURRENT, 140.0, 2000L);
        reading(BEAM_CURRENT, 100.0, 3000L);

        assertEquals(1, emitted.size());
        assertTrue(emitted.get(0) instanceof BelowMinOpened);
    }

    @Test
    public void emitsBelowMinClosedOnRecovery() {
        reading(BEAM_CURRENT, 150.0, 1000L);
        emitted.clear();

        reading(BEAM_CURRENT, 250.0, 5000L);

        assertEquals(1, emitted.size());
        BelowMinClosed closed = (BelowMinClosed) emitted.get(0);
        assertEquals(BEAM_CURRENT_ID, closed.attributeId());
        assertEquals(1000L, closed.openedAt().toEpochMilli());
        assertEquals(5000L, closed.timestamp().toEpochMilli());
        assertFalse(closed.duration().isNegative());
    }

    @Test
    public void valueExactlyAtMinIsNotABreach() {
        reading(BEAM_CURRENT, MIN, 1000L);
        assertTrue(emitted.isEmpty());
    }

    @Test
    public void recoveryAtExactMinClosesBreach() {
        reading(BEAM_CURRENT, 150.0, 1000L);
        emitted.clear();

        reading(BEAM_CURRENT, MIN, 2000L);

        assertEquals(1, emitted.size());
        assertTrue(emitted.get(0) instanceof BelowMinClosed);
    }

    // --- max-only bound (Temperature) ---

    @Test
    public void emitsAboveMaxOpenedOnCrossingAboveMax() {
        reading(TEMPERATURE, 50.0, 1000L);
        reading(TEMPERATURE, 150.0, 2000L);

        assertEquals(1, emitted.size());
        AboveMaxOpened opened = (AboveMaxOpened) emitted.get(0);
        assertEquals(TEMPERATURE_ID, opened.attributeId());
        assertEquals(150.0, opened.value(), 0.0001);
    }

    @Test
    public void emitsAboveMaxClosedOnRecovery() {
        reading(TEMPERATURE, 150.0, 1000L);
        emitted.clear();

        reading(TEMPERATURE, 50.0, 5000L);

        assertEquals(1, emitted.size());
        AboveMaxClosed closed = (AboveMaxClosed) emitted.get(0);
        assertEquals(TEMPERATURE_ID, closed.attributeId());
        assertFalse(closed.duration().isNegative());
    }

    @Test
    public void valueExactlyAtMaxIsNotABreach() {
        reading(TEMPERATURE, MAX, 1000L);
        assertTrue(emitted.isEmpty());
    }

    // --- both bounds on the same attribute (Pressure) ---

    @Test
    public void emitsBelowMinThenAboveMaxIndependently() {
        reading(PRESSURE, 5.0, 1000L);
        assertEquals(1, emitted.size());
        assertTrue(emitted.get(0) instanceof BelowMinOpened);

        reading(PRESSURE, 30.0, 2000L);
        assertEquals(2, emitted.size());
        assertTrue(emitted.get(1) instanceof BelowMinClosed);

        reading(PRESSURE, 80.0, 3000L);
        assertEquals(3, emitted.size());
        assertTrue(emitted.get(2) instanceof AboveMaxOpened);

        reading(PRESSURE, 30.0, 4000L);
        assertEquals(4, emitted.size());
        assertTrue(emitted.get(3) instanceof AboveMaxClosed);
    }

    // --- per-attribute-id isolation across attributes sharing a name ---

    @Test
    public void tracksBreachStatePerAttributeIdNotPerName() {
        reading(PRESSURE, 5.0, 1000L);
        reading(PRESSURE_2, 30.0, 1000L);

        assertEquals(1, emitted.size());
        BelowMinOpened opened = (BelowMinOpened) emitted.get(0);
        assertEquals(PRESSURE_ID, opened.attributeId());

        reading(PRESSURE_2, 5.0, 2000L);
        assertEquals(2, emitted.size());
        assertEquals(PRESSURE_2_ID, ((BelowMinOpened) emitted.get(1)).attributeId());
    }

    // --- attributes without configured bounds ---

    @Test
    public void ignoresAttributesWithoutConfiguredBounds() {
        reading(OTHER_ATTR, 1.0, 1000L);
        assertTrue(emitted.isEmpty());
    }

    @Test
    public void ignoresNullValues() {
        analyzer.onEvent(new SingleRecord<>(BEAM_CURRENT, 1000L, 1000L, null, "ReadFailure", null));
        assertTrue(emitted.isEmpty());
    }
}
