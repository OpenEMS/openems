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

import io.openems.edge.controller.ess.sohcycle.BatteryBalanceError;
import io.openems.edge.controller.ess.sohcycle.Config;
import io.openems.edge.controller.ess.sohcycle.ControllerEssSohCycleImpl;
import io.openems.edge.controller.ess.sohcycle.MyConfig;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

@RunWith(Parameterized.class)
public class ContextCalculateCellVoltageDeltaParamTest {

    private static final String ESS_ID = "ess0";
    private static final String CONTROLLER_ID = "ctrl0";
    private static final int DEFAULT_MAX_APPARENT_POWER = 10_000;
    private static final int DEFAULT_CAPACITY = 10_000;

    private ControllerEssSohCycleImpl controller;
    private DummyManagedSymmetricEss ess;
    private Config config;
    private Clock clock;

    /**
     * Test parameters:
     * minVoltage, maxVoltage, expectedDelta, expectedError.
     *
     * <p>
     * null indicates value not set
     * @return collection of test parameters
     */
    @Parameters(name = "{index}: min={0}, max={1}, delta={2}, error={3}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            // minVoltage, maxVoltage, expectedDelta, expectedError
            { 3200, 3300, 100, BatteryBalanceError.NONE },              // Normal case
            { 3200, 3250, 50, BatteryBalanceError.NONE },               // Smaller delta
            { 3300, 3300, 0, BatteryBalanceError.NONE },                // Zero delta
            { null, 3300, null, BatteryBalanceError.BASELINE_MIN_MISSING }, // Missing min -> check max first
            { 3200, null, null, BatteryBalanceError.MAX_VOLTAGE_UNDEFINED }, // Missing max
            { null, null, null, BatteryBalanceError.MAX_VOLTAGE_UNDEFINED }, // Missing both
            { 3300, 3200, null, BatteryBalanceError.DELTA_NEGATIVE },   // Inverted values
        });
    }

    private final Integer minVoltage;
    private final Integer maxVoltage;
    private final Integer expectedDelta;
    private final BatteryBalanceError expectedError;

    public ContextCalculateCellVoltageDeltaParamTest(Integer minVoltage, Integer maxVoltage,
            Integer expectedDelta, BatteryBalanceError expectedError) {
        this.minVoltage = minVoltage;
        this.maxVoltage = maxVoltage;
        this.expectedDelta = expectedDelta;
        this.expectedError = expectedError;
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
    public void testCalculateCellVoltageDeltaWithReason() {
        var context = new Context(this.controller, this.config, this.clock, this.ess);

        // Set up the stored voltage values
        if (this.minVoltage != null) {
            context.setMeasurementChargingMaxMinVoltage(this.minVoltage);
        }
        if (this.maxVoltage != null) {
            context.setMeasurementChargingMaxVoltage(this.maxVoltage);
        }

        // Execute
        var result = context.calculateCellVoltageDeltaWithReason();

        // Verify delta
        if (this.expectedDelta == null) {
            assertNull("Delta should be null for min=" + this.minVoltage + ", max=" + this.maxVoltage, result.delta());
        } else {
            assertEquals("Delta should match for min=" + this.minVoltage + ", max=" + this.maxVoltage,
                    this.expectedDelta, result.delta());
        }

        // Verify error
        assertEquals("Error should match for min=" + this.minVoltage + ", max=" + this.maxVoltage,
                this.expectedError, result.error());
    }
}
