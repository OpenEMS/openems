package io.openems.edge.controller.ess.sohcycle.statemachine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

/**
 * Parameterized test for calculateSoh scenarios.
 */
@RunWith(Parameterized.class)
public class ContextCalculateSohParamTest {

	private static final String ESS_ID = "ess0";
	private static final String CONTROLLER_ID = "ctrl0";
	private static final int DEFAULT_MAX_APPARENT_POWER = 10_000;
	private static final int DEFAULT_CAPACITY = 10_000;

	@Parameters(name = "{0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
				{ "Valid capacity 95%", 9500L, DEFAULT_CAPACITY, 95, 95.0f },
				{ "Valid capacity 80%", 8000L, DEFAULT_CAPACITY, 80, 80.0f },
				{ "Valid capacity 50%", 5000L, DEFAULT_CAPACITY, 50, 50.0f },
				{ "Valid capacity 100% new battery", 10500L, DEFAULT_CAPACITY, 100,  105.0f },
				{ "Zero capacity returns null", 0L, DEFAULT_CAPACITY, null, null },
				{ "Null capacity returns null", null, DEFAULT_CAPACITY, null, null },
				{ "Null ESS capacity returns null", 9500L, null, null, null }
		});
	}

	private final Long measuredCapacityWh;
	private final Integer essCapacity;
	private final Integer expectedSoh;
	private final Float expectedSohRaw;

	private ControllerEssSohCycleImpl controller;
	private DummyManagedSymmetricEss ess;
	private Config config;
	private Clock clock;

	public ContextCalculateSohParamTest(@SuppressWarnings("unused") String testName, Long measuredCapacityWh,
			Integer essCapacity, Integer expectedSoh, Float expectedSohRaw) {
		this.measuredCapacityWh = measuredCapacityWh;
		this.essCapacity = essCapacity;
		this.expectedSoh = expectedSoh;
		this.expectedSohRaw = expectedSohRaw;
	}

	@Before
	public void setup() {
		this.controller = new ControllerEssSohCycleImpl();
		this.ess = new DummyManagedSymmetricEss(ESS_ID)
				.withMaxApparentPower(DEFAULT_MAX_APPARENT_POWER)
				.withCapacity(this.essCapacity);
		this.clock = Clock.fixed(Instant.parse("2000-01-01T00:00:00.00Z"), ZoneOffset.UTC);
		this.config = MyConfig.create()
				.setId(CONTROLLER_ID)
				.setEssId(ESS_ID)
				.setRunning(true)
				.build();
	}

	@Test
	public void testCalculateSoh() {
		// Arrange
		var context = new Context(this.controller, this.config, this.clock, this.ess);

		// Act
		var sohResult = context.calculateSoh(this.measuredCapacityWh);

		// Assert
		if (this.expectedSoh == null) {
			assertTrue(sohResult.isEmpty());
		} else {
			assertTrue(sohResult.isPresent());
			var result = sohResult.get();
			assertEquals(this.expectedSoh, result.soh(), 0);
			assertEquals(this.expectedSohRaw, result.sohRaw(), 0.01f);
		}
	}
}
