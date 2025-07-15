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
import io.openems.edge.controller.io.heatingelement.enums.WorkMode;
import io.openems.edge.controller.io.heatingelement.enums.Status;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.io.test.DummyInputOutput;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.test.DummyElectricityMeter;


/**
 * A test to simulate a day in the mode Energy with a meter.
 */
public class ControllerHeatingElementImplTest8 {

	private static final TimeLeapClock CLOCK = createDummyClock();

	private static ControllerTest prepareTest(int minEnergy, String endTime) throws OpenemsNamedException, Exception {
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
						.setMinTime(1) //
						.setMinimumSwitchingTime(60) //
						.setMinEnergylimit(minEnergy) //
						.setEndTimeWithMeter(endTime) //
						.setScheduler("") //
						.build()); //
	}

	@Test
	public void testSimulateDay() throws Exception {
		var test = prepareTest(14000, "18:00");
		var energytracker = new EnergyTracker();

		test.next(new TestCase() //
				.timeleap(CLOCK, 1, SECONDS) //
				.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 5300) //
				.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 2000) //
				.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 1500) //
				.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 1800) //
				.output(LEVEL, Level.LEVEL_3), 5) //

				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 0) //
						.output(LEVEL, Level.LEVEL_0)) //

				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, 0) //
						.timeleap(CLOCK, 8 * 3600 - 5, SECONDS) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 0) //
						.output(LEVEL, Level.LEVEL_0)) //

				// Time: 8:00, sun is shining for 2 hours
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, -2000) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, energytracker.add(0, 1)) //
						.output(LEVEL, Level.LEVEL_1))

				// 2 h * 2000 W = 4000 Wh
				.next(new TestCase() //
						.timeleap(CLOCK, 1, SECONDS) //
						.input(GRID_ACTIVE_POWER, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 2000) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 2000) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, energytracker.add(2000, 7200)) //
						.output(LEVEL, Level.LEVEL_1), 7200)

				// Time: 10:00, PV Power goes up for an hour
				// 4000 Wh + 3500 W / 1 h = 7500 Wh
				.next(new TestCase() //
						.timeleap(CLOCK, 1, SECONDS) //
						.input(GRID_ACTIVE_POWER, -500) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 3500) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 2000) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 1500) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, energytracker.add(3500, 3599)) // 
						.output(LEVEL, Level.LEVEL_2), 3599)

				// Time: 11:00, It's cloudy, PV doesnt get energy until 14:45
				.next(new TestCase() //
						.timeleap(CLOCK, 1, SECONDS) //
						.input(GRID_ACTIVE_POWER, 3500) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 3500) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 2000) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 1500) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, energytracker.add(3500, 1)) // 
						.output(LEVEL, Level.LEVEL_0)) //

				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, energytracker.getCurrentWh()) // 
						.output(LEVEL, Level.LEVEL_0)) //

				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, 0) //
						.timeleap(CLOCK, 3600 * 3 + 2100, SECONDS) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, energytracker.add(0, 3600 * 3 + 2100)) // 
						.output(LEVEL, Level.LEVEL_1)); //

				// Time: 14:35, Active Force is now active till the end
				// 2000 W * ((3 * 3600 + 900) / 3600) h = 5500 Wh
				
				for (int i = 0; i < 3 * 3600 + 899; i++) {
					test.next(new TestCase()
				            .timeleap(CLOCK, 1, SECONDS)
				            .input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 2000)
				            .input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 0)
				            .input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 0)
				            .input("meter3", ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, energytracker.add(2000, 1))
				            .input(LEVEL, Level.LEVEL_1)
				            .input(STATUS, Status.ACTIVE_FORCED)
				        );
				}

				// 6500 Wh + 7500 Wh = 14000 Wh
				// Limit reached
				test.next(new TestCase() //
						.timeleap(CLOCK, 1, SECONDS) //
						.input(GRID_ACTIVE_POWER, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 2000) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 2000) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, energytracker.add(2000, 1)) // 
						.output(LEVEL, Level.LEVEL_0) //
						.output(STATUS, Status.DONE))

				.deactivate();

	}
}
