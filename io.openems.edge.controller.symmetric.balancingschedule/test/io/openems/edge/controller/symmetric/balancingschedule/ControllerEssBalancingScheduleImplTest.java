package io.openems.edge.controller.symmetric.balancingschedule;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.meter.test.DummyElectricityMeter;

public class ControllerEssBalancingScheduleImplTest {

	private static final String CTRL_ID = "ctrl0";
	private static final String ESS_ID = "ess0";
	private static final String METER_ID = "meter0";

	private static final ChannelAddress CTRL_NO_ACTIVE_SETPOINT = new ChannelAddress(CTRL_ID, "NoActiveSetpoint");
	private static final ChannelAddress CTRL_GRID_ACTIVE_POWER_SET_POINT = new ChannelAddress(CTRL_ID,
			"GridActivePowerSetPoint");

	private static final ChannelAddress ESS_GRID_MODE = new ChannelAddress(ESS_ID, "GridMode");
	private static final ChannelAddress ESS_ACTIVE_POWER = new ChannelAddress(ESS_ID, "ActivePower");
	private static final ChannelAddress SET_ACTIVE_POWER_EQUALS_WITH_PID = new ChannelAddress(ESS_ID,
			"SetActivePowerEqualsWithPid");

	private static final ChannelAddress GRID_ACTIVE_POWER = new ChannelAddress(METER_ID, "ActivePower");

	@Test
	public void test() throws Exception {
		final var start = 1577836800L;
		final var clock = new TimeLeapClock(Instant.ofEpochSecond(start) /* starts at 1. January 2020 00:00:00 */,
				ZoneOffset.UTC);
		new ControllerTest(new ControllerEssBalancingScheduleImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)) //
				.addReference("meter", new DummyElectricityMeter(METER_ID)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setMeterId(METER_ID) //
						.setSchedule(JsonUtils.buildJsonArray()//
								.add(JsonUtils.buildJsonObject()//
										.addProperty("startTimestamp", start + 500) //
										.addProperty("duration", 900) //
										.addProperty("activePowerSetPoint", 0) //
										.build()) //
								.add(JsonUtils.buildJsonObject()//
										.addProperty("startTimestamp", start + 500 + 800) //
										.addProperty("duration", 900) //
										.addProperty("activePowerSetPoint", 3000) //
										.build() //
								).build().toString() //
						).build()) //
				.next(new TestCase("No active setpoint") //
						.input(ESS_GRID_MODE, GridMode.ON_GRID) //
						.input(GRID_ACTIVE_POWER, 4000) //
						.input(ESS_ACTIVE_POWER, 1000) //
						.output(CTRL_NO_ACTIVE_SETPOINT, true)) //
				.next(new TestCase("Balance to 0") //
						.timeleap(clock, 500, ChronoUnit.SECONDS) //
						.input(GRID_ACTIVE_POWER, 4000) //
						.input(ESS_ACTIVE_POWER, 1000) //
						.output(CTRL_NO_ACTIVE_SETPOINT, false) //
						.output(SET_ACTIVE_POWER_EQUALS_WITH_PID, 5000)) //
				.next(new TestCase("Balance to -2000 via Channel") //
						.input(CTRL_GRID_ACTIVE_POWER_SET_POINT, -2000) //
						.input(GRID_ACTIVE_POWER, 4000) //
						.input(ESS_ACTIVE_POWER, 1000) //
						.output(SET_ACTIVE_POWER_EQUALS_WITH_PID, 7000)) //
				.next(new TestCase("Balance to 3000") //
						.timeleap(clock, 800, ChronoUnit.SECONDS) //
						.input(GRID_ACTIVE_POWER, 4000) //
						.input(ESS_ACTIVE_POWER, 1000) //
						.output(CTRL_NO_ACTIVE_SETPOINT, false) //
						.output(SET_ACTIVE_POWER_EQUALS_WITH_PID, 2000)) //
		;
	}
}
