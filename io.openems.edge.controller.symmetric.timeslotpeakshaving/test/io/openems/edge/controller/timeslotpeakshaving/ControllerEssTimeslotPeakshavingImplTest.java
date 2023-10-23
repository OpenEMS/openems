package io.openems.edge.controller.timeslotpeakshaving;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.common.test.TimeLeapClock;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.meter.test.DummyElectricityMeter;

public class ControllerEssTimeslotPeakshavingImplTest {

	private static final String CTRL_ID = "ctrl0";

	private static final String ESS_ID = "ess0";
	private static final ChannelAddress ESS_SOC = new ChannelAddress(ESS_ID, "Soc");
	private static final ChannelAddress ESS_ACTIVE_POWER = new ChannelAddress(ESS_ID, "ActivePower");
	private static final ChannelAddress ESS_SET_ACTIVE_POWER_EQUALS = new ChannelAddress(ESS_ID,
			"SetActivePowerEquals");

	private static final String METER_ID = "meter0";
	private static final ChannelAddress METER_ACTIVE_POWER = new ChannelAddress(METER_ID, "ActivePower");

	@Test
	public void test() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-02-03T08:30:00.00Z"), ZoneOffset.UTC);
		new ControllerTest(new ControllerEssTimeslotPeakshavingImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addComponent(new DummyManagedSymmetricEss(ESS_ID, new DummyPower(0.3, 0.3, 0.1)) //
						.withGridMode(GridMode.ON_GRID)) //
				.addComponent(new DummyElectricityMeter(METER_ID)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setMeterId(METER_ID) //
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
						.input(ESS_ACTIVE_POWER, 0) //
						.input(ESS_SOC, 90) //
						.input(METER_ACTIVE_POWER, 120000) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, null)) //
				.next(new TestCase() //
						.timeleap(clock, 31, ChronoUnit.MINUTES)/* current time is 09:31, run in slow charge state */
						.input(ESS_SOC, 96) //
						.input(ESS_ACTIVE_POWER, 5000) //
						.input(METER_ACTIVE_POWER, 120000) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 13500)) //
				.next(new TestCase() //
						.timeleap(clock, 31, ChronoUnit.MINUTES)/* current time is 09:31, run in hysterisis state */
						.input(ESS_SOC, 100) //
						.input(ESS_ACTIVE_POWER, 5000) //
						.input(METER_ACTIVE_POWER, 120000)) // nothing set on, Ess's setActivePower
				.next(new TestCase() //
						.input(ESS_SOC, 94) //
						.input(ESS_ACTIVE_POWER, 5000) //
						.input(METER_ACTIVE_POWER, 120000) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 27000)) //
				.next(new TestCase() //
						.timeleap(clock, 75, ChronoUnit.MINUTES)/* current time is 10:47, run in high threshold state */
						.input(ESS_ACTIVE_POWER, 5000) //
						.input(METER_ACTIVE_POWER, 120000) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 33000)) //
				.next(new TestCase() //
						.timeleap(clock, 75, ChronoUnit.MINUTES)/* current time is 12:02 run in normal state */
						.input(ESS_ACTIVE_POWER, 5000) //
						.input(METER_ACTIVE_POWER, 120000)); // nothing set on, Ess's setActivePower
	}
}
