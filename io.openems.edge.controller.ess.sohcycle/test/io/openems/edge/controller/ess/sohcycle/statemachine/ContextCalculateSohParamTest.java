package io.openems.edge.controller.ess.sohcycle.statemachine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.openems.edge.controller.ess.sohcycle.Config;
import io.openems.edge.controller.ess.sohcycle.ControllerEssSohCycleImpl;
import io.openems.edge.controller.ess.sohcycle.MyConfig;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

/**
 * Parameterized test for calculateSoh scenarios.
 */
public class ContextCalculateSohParamTest {

	private static final String ESS_ID = "ess0";
	private static final String CONTROLLER_ID = "ctrl0";
	private static final int DEFAULT_MAX_APPARENT_POWER = 10_000;
	private static final int DEFAULT_CAPACITY = 10_000;

	private ControllerEssSohCycleImpl controller;
	private DummyManagedSymmetricEss ess;
	private Config config;
	private Clock clock;

	static Stream<Arguments> data() {
		return Stream.of(//
				Arguments.of(9500L, DEFAULT_CAPACITY, 95, 95.0f), // Valid capacity 95%
				Arguments.of(8000L, DEFAULT_CAPACITY, 80, 80.0f), // Valid capacity 80%
				Arguments.of(5000L, DEFAULT_CAPACITY, 50, 50.0f), // Valid capacity 50%
				Arguments.of(10500L, DEFAULT_CAPACITY, 100, 105.0f), // Valid capacity 100% new battery
				Arguments.of(0L, DEFAULT_CAPACITY, null, null), // Zero capacity returns null
				Arguments.of(null, DEFAULT_CAPACITY, null, null), // Null capacity returns null
				Arguments.of(9500L, null, null, null) // Null ESS capacity returns null
		);
	}

	@BeforeEach
	public void setup() {
		this.controller = new ControllerEssSohCycleImpl();
		this.clock = Clock.fixed(Instant.parse("2000-01-01T00:00:00.00Z"), ZoneOffset.UTC);
		this.config = MyConfig.create() //
				.setId(CONTROLLER_ID) //
				.setEssId(ESS_ID) //
				.setRunning(true) //
				.build();
	}

	@ParameterizedTest(name = "[{index}] measuredCapacityWh={0}, essCapacity={1}, expectedSoh={2}")
	@MethodSource("data")
	public void testCalculateSoh(Long measuredCapacityWh, Integer essCapacity, Integer expectedSoh,
			Float expectedSohRaw) {
		// Arrange
		this.ess = new DummyManagedSymmetricEss(ESS_ID) //
				.withMaxApparentPower(DEFAULT_MAX_APPARENT_POWER) //
				.withCapacity(essCapacity);
		var context = new Context(this.controller, this.config, this.clock, this.ess);

		// Act
		var sohResult = context.calculateSoh(measuredCapacityWh);

		// Assert
		if (expectedSoh == null) {
			assertTrue(sohResult.isEmpty());
		} else {
			assertTrue(sohResult.isPresent());
			var result = sohResult.get();
			assertEquals(expectedSoh, result.soh(), 0);
			assertEquals(expectedSohRaw, result.sohRaw(), 0.01f);
		}
	}
}
