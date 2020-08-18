package io.openems.edge.controller.symmetric.balancingschedule;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.meter.test.DummySymmetricMeter;

public class BalancingScheduleImplTest {

	private final static String CTRL_ID = "ctrl0";
	private final static String ESS_ID = "ess0";
	private final static String METER_ID = "meter0";

	private final static ChannelAddress CTRL_NO_ACTIVE_SETPOINT = new ChannelAddress(CTRL_ID, "NoActiveSetpoint");

	private final static ChannelAddress ESS_ACTIVE_POWER = new ChannelAddress(ESS_ID, "ActivePower");
	private final static ChannelAddress SET_ACTIVE_POWER_EQUALS_WITH_PID = new ChannelAddress(ESS_ID,
			"SetActivePowerEqualsWithPid");

	private final static ChannelAddress GRID_ACTIVE_POWER = new ChannelAddress(METER_ID, "ActivePower");

	@Test
	public void test() throws Exception {
		final long start = 1577836800L;
		final TimeLeapClock clock = new TimeLeapClock(
				Instant.ofEpochSecond(start) /* starts at 1. January 2020 00:00:00 */, ZoneOffset.UTC);
		new ControllerTest(new BalancingScheduleImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)) //
				.addReference("meter", new DummySymmetricMeter(METER_ID)) //
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
						.input(GRID_ACTIVE_POWER, 4000) //
						.input(ESS_ACTIVE_POWER, 1000) //
						.output(CTRL_NO_ACTIVE_SETPOINT, true)) //
				.next(new TestCase("Balance to 0") //
						.timeleap(clock, 500, ChronoUnit.SECONDS) //
						.input(GRID_ACTIVE_POWER, 4000) //
						.input(ESS_ACTIVE_POWER, 1000) //
						.output(CTRL_NO_ACTIVE_SETPOINT, false) //
						.output(SET_ACTIVE_POWER_EQUALS_WITH_PID, 5000)) //
				.next(new TestCase("Balance to 3000") //
						.timeleap(clock, 800, ChronoUnit.SECONDS) //
						.input(GRID_ACTIVE_POWER, 4000) //
						.input(ESS_ACTIVE_POWER, 1000) //
						.output(CTRL_NO_ACTIVE_SETPOINT, false) //
						.output(SET_ACTIVE_POWER_EQUALS_WITH_PID, 2000)) //
		;
	}
}
