package io.openems.edge.controller.io.heatingelement;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.common.sum.Sum.ChannelId.GRID_ACTIVE_POWER;
import static io.openems.edge.controller.io.heatingelement.ControllerIoHeatingElement.ChannelId.LEVEL;
import static io.openems.edge.controller.io.heatingelement.ControllerIoHeatingElement.ChannelId.STATUS;

import static java.time.temporal.ChronoUnit.SECONDS;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.io.heatingelement.enums.Level;
import io.openems.edge.controller.io.heatingelement.enums.Mode;
import io.openems.edge.controller.io.heatingelement.enums.Status;
import io.openems.edge.controller.io.heatingelement.enums.WorkMode;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.io.test.DummyInputOutput;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.test.DummyElectricityMeter;

/**
 * A test to simulate three days with the configuration of a task scheduler in
 * the workmode Energy.
 */
public class ControllerHeatingElementImplTest9 {

	private static final TimeLeapClock CLOCK = createDummyClock();

	private static ControllerTest prepareTest() throws OpenemsNamedException, Exception {
		return new ControllerTest(new ControllerIoHeatingElementImpl()) //
				.addReference("componentManager", new DummyComponentManager(CLOCK)) //
				.addReference("sum", new DummySum()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("meter", new DummyElectricityMeter("meter3")) //
				.addComponent(new DummyInputOutput("io0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setOutputChannelPhaseL1("io0/InputOutput0") //
						.setOutputChannelPhaseL2("io0/InputOutput1") //
						.setOutputChannelPhaseL3("io0/InputOutput2") //
						.setPowerOfPhase(2000) //
						.setMode(Mode.AUTOMATIC) //
						.setWorkMode(WorkMode.ENERGY) //
						.setDefaultLevel(Level.LEVEL_1) //
						.setMeterid("meter3") //
						.setEndTime("00:00") //
						.setMinTime(0) //
						.setMinimumSwitchingTime(60) //
						.setMinEnergylimit(10000) //
						.setEndTimeWithMeter("00:00") //
						.setScheduler("""
								[
									{
										"@type":"Task",
										"start":"08:00:00",
										"duration": "PT12H",
										"recurrenceRules":[
											{
												"frequency":"daily"
											}
										],
										"openems.io:payload": {
										"sessionEnergy": 12000
										}
									}
								]
								""") //
						.build()); //
	}

	@Test
	public void testWithSchedule() throws Exception {
		var test = prepareTest();
		var energytracker = new EnergyTracker();

		this.daySimulation(test, energytracker, true);
		this.daySimulation(test, energytracker, false);
		this.daySimulation(test, energytracker, false);
		test.deactivate();
	}

	private void daySimulation(ControllerTest test, EnergyTracker energytracker, boolean firstday) throws Exception {

		int calibrationDuration;
		if (firstday) {
			calibrationDuration = 5;
			test.next(new TestCase() //
					.timeleap(CLOCK, 1, SECONDS) //
					.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 6000) //
					.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 2000) //
					.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 2000) //
					.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 2000) //
					.output(LEVEL, Level.LEVEL_3) //
					.output(STATUS, Status.CALIBRATION), 5); //

		} else {
			calibrationDuration = 0;
		}

		test.next(new TestCase() //
				.input(GRID_ACTIVE_POWER, 0) //
				.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 0) //
				.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 0) //
				.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 0) //
				.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 0) //
				.output(LEVEL, Level.LEVEL_0) //
				.output(STATUS, Status.INACTIVE)) //

				.next(new TestCase() //
						.timeleap(CLOCK, 1, SECONDS) //
						.input(GRID_ACTIVE_POWER, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 0) //
						.output(LEVEL, Level.LEVEL_0) //
						.output(STATUS, Status.INACTIVE), 8 * 3600 - calibrationDuration) //

				// 8:00
				.next(new TestCase() //
						.timeleap(CLOCK, 1, SECONDS) //
						.input(GRID_ACTIVE_POWER, -3000) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, energytracker.add(0, 1)) //
						.output(LEVEL, Level.LEVEL_1) //
						.output(STATUS, Status.ACTIVE)) //

				// 3 h * 2000 W = 6000 Wh
				.next(new TestCase() //
						.timeleap(CLOCK, 1, SECONDS) //
						.input(GRID_ACTIVE_POWER, -1000) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 2000) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 2000) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY,
								energytracker.add(2000, 3 * 3600))
						.output(LEVEL, Level.LEVEL_1) //
						.output(STATUS, Status.ACTIVE), 3 * 3600 - 3) //

				// 11:00
				.next(new TestCase() //
						.timeleap(CLOCK, 1, SECONDS) //
						.input(GRID_ACTIVE_POWER, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 0) //
						.output(LEVEL, Level.LEVEL_0) //
						.output(STATUS, Status.INACTIVE), 6 * 3600 - 599); //

		// 16:50
		test.next(new TestCase() //
				.timeleap(CLOCK, 1, SECONDS) //
				.input(GRID_ACTIVE_POWER, 0) //
				.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 0) //
				.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 0) //
				.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 0) //
				.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 0) //
				.input("meter3", ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, 6000) //
				.output(LEVEL, Level.LEVEL_1) //
				.output(STATUS, Status.ACTIVE_FORCED_LIMIT)); //

		for (int i = 0; i < 3600 * 3 - 2; i++) {
			test.next(new TestCase() //
					.timeleap(CLOCK, 1, SECONDS) //
					.input(GRID_ACTIVE_POWER, 2000) //
					.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 2000) //
					.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 2000) //
					.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 0) //
					.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 0) //
					.input("meter3", ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, energytracker.add(2000, 1))
					.output(LEVEL, Level.LEVEL_1) //
					.output(STATUS, Status.ACTIVE_FORCED_LIMIT)); //
		}

		// 19:50
		test.next(new TestCase() //
				.timeleap(CLOCK, 1, SECONDS) //
				.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 2000) //
				.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 2000) //
				.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 0) //
				.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 0) //
				.input("meter3", ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, energytracker.add(2000, 1))
				.output(LEVEL, Level.LEVEL_0) //
				.output(STATUS, Status.DONE)) //

				.next(new TestCase() //
						.timeleap(CLOCK, 1800, SECONDS) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 0) //
						.output(LEVEL, Level.LEVEL_0) //
						.output(STATUS, Status.DONE)) //

				.next(new TestCase() //
						.timeleap(CLOCK, 3600 * 3 + 2401, SECONDS) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, energytracker.reset())
						.output(LEVEL, Level.LEVEL_0) //
						.output(STATUS, Status.INACTIVE)) //
		;
	}

}
