package io.openems.edge.controller.ess.sohcycle.statemachine;

import static io.openems.edge.ess.api.SymmetricEss.ChannelId.MIN_CELL_VOLTAGE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.Before;
import org.junit.Test;

import io.openems.edge.common.test.TestUtils;
import io.openems.edge.controller.ess.sohcycle.Config;
import io.openems.edge.controller.ess.sohcycle.ControllerEssSohCycleImpl;
import io.openems.edge.controller.ess.sohcycle.Mode;
import io.openems.edge.controller.ess.sohcycle.MyConfig;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

public class ContextTest {

	private static final String ESS_ID = "ess0";
	private static final String CONTROLLER_ID = "ctrl0";
	private static final int DEFAULT_MAX_APPARENT_POWER = 10_000;
	private static final int DEFAULT_CAPACITY = 10_000;

	private ControllerEssSohCycleImpl controller;
	private DummyManagedSymmetricEss ess;
	private Config config;
	private Clock clock;

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
				.setMode(Mode.MANUAL_ON) //
				.build();
	}

	@Test
	public void testRefreshMeasurementChargingMinVoltageFromEss_Reset() {
		// Arrange
		var context = new Context(this.controller, this.config, this.clock, this.ess);
		TestUtils.withValue(this.ess, MIN_CELL_VOLTAGE, 3250);
		context.refreshMeasurementChargingMinVoltageFromEss();
		assertEquals(Integer.valueOf(3250), context.getMeasurementChargingMaxMinVoltage());

		// Act - reset (simulate end of measurement cycle)
		context.resetMeasurementChargingMaxMinVoltage();

		// Assert
		assertNull(context.getMeasurementChargingMaxMinVoltage());

		// Act - start new measurement cycle
		TestUtils.withValue(this.ess, MIN_CELL_VOLTAGE, 3180);
		context.refreshMeasurementChargingMinVoltageFromEss();

		// Assert - should start fresh with new value
		assertEquals(Integer.valueOf(3180), context.getMeasurementChargingMaxMinVoltage());
	}
}
