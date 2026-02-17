package io.openems.edge.controller.ess.sohcycle.statemachine;

import static io.openems.edge.ess.api.SymmetricEss.ChannelId.MAX_CELL_VOLTAGE;
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

import io.openems.edge.common.test.TestUtils;
import io.openems.edge.controller.ess.sohcycle.Config;
import io.openems.edge.controller.ess.sohcycle.ControllerEssSohCycleImpl;
import io.openems.edge.controller.ess.sohcycle.MyConfig;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

@RunWith(Parameterized.class)
public class ContextRefreshMaxVoltageParamTest {

    private static final String ESS_ID = "ess0";
    private static final String CONTROLLER_ID = "ctrl0";
    private static final int DEFAULT_MAX_APPARENT_POWER = 10_000;
    private static final int DEFAULT_CAPACITY = 10_000;

    private ControllerEssSohCycleImpl controller;
    private DummyManagedSymmetricEss ess;
    private Config config;
    private Clock clock;

    @Parameters(name = "{index}: initial={0}, update={1}, expected={2}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            // initial, update, expected stored value after refresh
            { 3300, 3350, 3350 }, // increases
            { 3350, 3300, 3350 }, // decreases -> keep max
            { 3300, 3300, 3300 }, // same -> keep
            { null, 3280, 3280 }, // start undefined -> set
        });
    }

    private final Integer initial;
    private final Integer update;
    private final Integer expected;

    public ContextRefreshMaxVoltageParamTest(Integer initial, Integer update, Integer expected) {
        this.initial = initial;
        this.update = update;
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
    public void testRefreshMaxVoltageSequence() {
        var context = new Context(this.controller, this.config, this.clock, this.ess);

        // Initial
        if (this.initial != null) {
            TestUtils.withValue(this.ess, MAX_CELL_VOLTAGE, this.initial);
            context.refreshMeasurementChargingVoltageRange();
            assertEquals(this.initial, context.getMeasurementChargingMaxVoltage());
        } else {
            // explicitly ensure it's null
            assertNull(context.getMeasurementChargingMaxVoltage());
        }

        // Update
        TestUtils.withValue(this.ess, MAX_CELL_VOLTAGE, this.update);
        context.refreshMeasurementChargingVoltageRange();
        assertEquals(this.expected, context.getMeasurementChargingMaxVoltage());
    }
}
