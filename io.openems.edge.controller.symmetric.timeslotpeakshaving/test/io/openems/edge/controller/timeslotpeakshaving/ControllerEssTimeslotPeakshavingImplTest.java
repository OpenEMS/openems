package io.openems.edge.controller.timeslotpeakshaving;

import static io.openems.edge.ess.api.ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS;
import static io.openems.edge.ess.api.SymmetricEss.ChannelId.SOC;
import static java.time.temporal.ChronoUnit.MINUTES;

import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.test.DummyElectricityMeter;

public class ControllerEssTimeslotPeakshavingImplTest {

	@Test
	public void test() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-02-03T08:30:00.00Z"), ZoneOffset.UTC);
		new ControllerTest(new ControllerEssTimeslotPeakshavingImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addComponent(new DummyManagedSymmetricEss("ess0") //
						.setPower(new DummyPower(0.3, 0.3, 0.1)) //
						.withGridMode(GridMode.ON_GRID)) //
				.addComponent(new DummyElectricityMeter("meter0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setMeterId("meter0") //
						.setPeakShavingPower(100_000) //
						.setRechargePower(50_000) //
						.setSlowChargePower(50_000) //
						.setStartDate("01.01.2020") //
						.setEndDate("30.04.2020") //
						.setStartTime("10:00") //
						.setEndTime("11:00") //
						.setSlowChargeStartTime("9:00") //
						.setHysteresisSoc(95) //
						.setMonday(true) //
						.setTuesday(true) //
						.setWednesday(true) //
						.setThursday(true) //
						.setFriday(true) //
						.setSaturday(true) //
						.setSunday(true) //
						.build())
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 0) //
						.input("ess0", SOC, 90) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 120000) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, null)) //
				.next(new TestCase() //
						.timeleap(clock, 31, MINUTES)/* current time is 09:31, run in slow charge state */
						.input("ess0", SOC, 96) //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 5000) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 120000) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 13500)) //
				.next(new TestCase() //
						.timeleap(clock, 31, MINUTES)/* current time is 09:31, run in hysterisis state */
						.input("ess0", SOC, 100) //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 5000) //
						// nothing set on, Ess's setActivePower
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 120000))
				.next(new TestCase() //
						.input("ess0", SOC, 94) //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 5000) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 120000) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 27000)) //
				.next(new TestCase() //
						.timeleap(clock, 75, MINUTES)/* current time is 10:47, run in high threshold state */
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 5000) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 120000) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 33000)) //
				.next(new TestCase() //
						.timeleap(clock, 75, MINUTES)/* current time is 12:02 run in normal state */
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 5000) //
						// nothing set on, Ess's setActivePower
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 120000)) //
				.deactivate();
	}
}
