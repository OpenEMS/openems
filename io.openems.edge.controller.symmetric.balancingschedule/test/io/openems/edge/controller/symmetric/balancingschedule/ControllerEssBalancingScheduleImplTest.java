package io.openems.edge.controller.symmetric.balancingschedule;

import static io.openems.common.utils.JsonUtils.buildJsonArray;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.edge.controller.symmetric.balancingschedule.ControllerEssBalancingSchedule.ChannelId.GRID_ACTIVE_POWER_SET_POINT;
import static io.openems.edge.controller.symmetric.balancingschedule.ControllerEssBalancingSchedule.ChannelId.NO_ACTIVE_SETPOINT;
import static io.openems.edge.ess.api.ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS_WITH_PID;
import static io.openems.edge.ess.api.SymmetricEss.ChannelId.GRID_MODE;
import static java.time.temporal.ChronoUnit.SECONDS;

import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.test.DummyElectricityMeter;

public class ControllerEssBalancingScheduleImplTest {

	@Test
	public void test() throws Exception {
		final var start = 1577836800L;
		final var clock = new TimeLeapClock(Instant.ofEpochSecond(start) /* starts at 1. January 2020 00:00:00 */,
				ZoneOffset.UTC);
		new ControllerTest(new ControllerEssBalancingScheduleImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0")) //
				.addReference("meter", new DummyElectricityMeter("meter0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setMeterId("meter0") //
						.setSchedule(buildJsonArray()//
								.add(buildJsonObject()//
										.addProperty("startTimestamp", start + 500) //
										.addProperty("duration", 900) //
										.addProperty("activePowerSetPoint", 0) //
										.build()) //
								.add(buildJsonObject()//
										.addProperty("startTimestamp", start + 500 + 800) //
										.addProperty("duration", 900) //
										.addProperty("activePowerSetPoint", 3000) //
										.build()) //
								.build().toString()) //
						.build()) //
				.next(new TestCase("No active setpoint") //
						.input("ess0", GRID_MODE, GridMode.ON_GRID) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 4000) //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 1000) //
						.output(NO_ACTIVE_SETPOINT, true)) //
				.next(new TestCase("Balance to 0") //
						.timeleap(clock, 500, SECONDS) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 4000) //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 1000) //
						.output(NO_ACTIVE_SETPOINT, false) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS_WITH_PID, 5000)) //
				.next(new TestCase("Balance to -2000 via Channel") //
						.input(GRID_ACTIVE_POWER_SET_POINT, -2000) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 4000) //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 1000) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS_WITH_PID, 7000)) //
				.next(new TestCase("Balance to 3000") //
						.timeleap(clock, 800, SECONDS) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 4000) //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 1000) //
						.output(NO_ACTIVE_SETPOINT, false) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS_WITH_PID, 2000)) //
				.deactivate();
	}
}
