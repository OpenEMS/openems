package io.openems.edge.controller.ess.sohcycle.statemachine;

import static io.openems.edge.ess.api.SymmetricEss.ChannelId.MAX_CELL_VOLTAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.openems.edge.common.test.TestUtils;
import io.openems.edge.controller.ess.sohcycle.Config;
import io.openems.edge.controller.ess.sohcycle.ControllerEssSohCycleImpl;
import io.openems.edge.controller.ess.sohcycle.MyConfig;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

public class ContextRefreshMaxVoltageParamTest {

    private static final String ESS_ID = "ess0";
    private static final String CONTROLLER_ID = "ctrl0";
    private static final int DEFAULT_MAX_APPARENT_POWER = 10_000;
    private static final int DEFAULT_CAPACITY = 10_000;

    private ControllerEssSohCycleImpl controller;
    private DummyManagedSymmetricEss ess;
    private Config config;
    private Clock clock;

	static Stream<Arguments> data() {
		return Stream.of(
				// initial, update, expected stored value after refresh
				Arguments.of(3300, 3350, 3350), // increases
				Arguments.of(3350, 3300, 3350), // decreases -> keep max
				Arguments.of(3300, 3300, 3300), // same -> keep
				Arguments.of(null, 3280, 3280) // start undefined -> set
		);
    }

	@BeforeEach
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

	/**
	 * Tests refreshMeasurementChargingVoltageRange with different initial and
	 * updated max voltage values. Verifies that the context correctly updates or
	 * retains the max voltage based on the defined logic.
	 * 
	 * @param initial  Initial max voltage value (can be null to indicate undefined)
	 * @param update   Updated max voltage value to refresh with
	 * @param expected Expected max voltage value in the context after refresh
	 */
	@ParameterizedTest(name = "{index}: initial={0}, update={1}, expected={2}")
	@MethodSource("data")
	public void testRefreshMaxVoltageSequence(Integer initial, Integer update, Integer expected) {
        var context = new Context(this.controller, this.config, this.clock, this.ess);
		// Initial
		if (initial != null) {
			TestUtils.withValue(this.ess, MAX_CELL_VOLTAGE, initial);
            context.refreshMeasurementChargingVoltageRange();
			assertEquals(initial, context.getMeasurementChargingMaxVoltage());
        } else {
            // explicitly ensure it's null
            assertNull(context.getMeasurementChargingMaxVoltage());
        }
		// Update
		TestUtils.withValue(this.ess, MAX_CELL_VOLTAGE, update);
        context.refreshMeasurementChargingVoltageRange();
		assertEquals(expected, context.getMeasurementChargingMaxVoltage());
    }
}
