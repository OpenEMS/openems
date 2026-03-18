package io.openems.edge.controller.ess.sohcycle.statemachine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import io.openems.edge.controller.ess.sohcycle.Config;
import io.openems.edge.controller.ess.sohcycle.ControllerEssSohCycleImpl;
import io.openems.edge.controller.ess.sohcycle.MyConfig;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

@RunWith(Parameterized.class)
public class ContextCellVoltageDeltaParamTest {

    private static final String ESS_ID = "ess0";
    private static final String CONTROLLER_ID = "ctrl0";
    private static final int DEFAULT_MAX_APPARENT_POWER = 10_000;
    private static final int DEFAULT_CAPACITY = 10_000;

    private ControllerEssSohCycleImpl controller;
    private DummyManagedSymmetricEss ess;
    private Config config;
    private Clock clock;

    @Parameters(name = "{index}: max={0}, storedMin={1} => expectedDelta={2}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            // max, storedMin, expectedDelta
            { null, 3200, null },      // undefined max
            { 3300, null, null },      // undefined min
            { 3300, 3200, 100 },       // positive delta
            { 3200, 3250, null },      // negative delta treated as undefined
            { 3230, 3230, 0 },         // zero delta
        });
    }

    private final Integer max;
    private final Integer storedMin;
    private final Integer expected;

    public ContextCellVoltageDeltaParamTest(Integer max, Integer storedMin, Integer expected) {
        this.max = max;
        this.storedMin = storedMin;
        this.expected = expected;
    }

    @Before
    public void setup() {
        this.controller = new ControllerEssSohCycleImpl();
        this.ess = new DummyManagedSymmetricEss(ESS_ID) //
                .withMaxApparentPower(DEFAULT_MAX_APPARENT_POWER) //
                .withCapacity(DEFAULT_CAPACITY);
        this.clock = Clock.fixed(Instant.parse("2000-01-01T00:00:00.00Z"), ZoneOffset.UTC);
        this.config = MyConfig.create() //
                .setId(CONTROLLER_ID) //
                .setEssId(ESS_ID) //
                .setRunning(true) //
                .build();
    }

    @Test
    public void testDelta() {
        var context = new Context(this.controller, this.config, this.clock, this.ess);

        if (this.storedMin != null) {
            context.setMeasurementChargingMaxMinVoltage(this.storedMin);
        }

        if (this.max != null) {
            context.setMeasurementChargingMaxVoltage(this.max);
        }

        var delta = context.calculateCellVoltageDelta();
        if (this.expected == null) {
            assertNull(delta);
        } else {
            assertEquals(this.expected, delta);
        }
    }
}

