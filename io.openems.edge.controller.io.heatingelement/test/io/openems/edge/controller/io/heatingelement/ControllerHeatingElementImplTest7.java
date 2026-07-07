package io.openems.edge.controller.io.heatingelement;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.common.sum.Sum.ChannelId.GRID_ACTIVE_POWER;
import static io.openems.edge.controller.io.heatingelement.ControllerIoHeatingElement.ChannelId.LEVEL;
import static io.openems.edge.controller.io.heatingelement.ControllerIoHeatingElement.ChannelId.STATUS;
import static java.time.temporal.ChronoUnit.SECONDS;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.io.heatingelement.enums.Level;
import io.openems.edge.controller.io.heatingelement.enums.Mode;
import io.openems.edge.controller.io.heatingelement.enums.Status;
import io.openems.edge.controller.io.heatingelement.enums.WorkMode;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.io.test.DummyInputOutput;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.test.DummyElectricityMeter;

/**
 * A test to check if the controller is behaving correctly if the mode
 * unreachable and active_forced_limit is reached.
 */
public class ControllerHeatingElementImplTest7 {

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
	public void testUnreachable() throws Exception {
		prepareTest(19000, "04:00") //
				.next(new TestCase() //
						.timeleap(CLOCK, 1, SECONDS) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 5000) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 1700) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 1700) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 1600) //
						.output(LEVEL, Level.LEVEL_3), 5) //

				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, -2000) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 0) //
						.output(LEVEL, Level.LEVEL_3) //
						.output(STATUS, Status.UNREACHABLE))

				.deactivate();
	}

	@Test
	public void testActiveForce() throws Exception {
		prepareTest(10000, "08:10") //
				.next(new TestCase() //
						.timeleap(CLOCK, 1, SECONDS) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 6000) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 2000) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 2000) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 2000) //
						.output(LEVEL, Level.LEVEL_3), 5) //
				.next(new TestCase() //
						.timeleap(CLOCK, 1, SECONDS) //
						.input(GRID_ACTIVE_POWER, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 0) //
						.output(LEVEL, Level.LEVEL_0)) //
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, 0) //
						.timeleap(CLOCK, 3600 * 3 - 7, SECONDS) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 0) //
						.output(LEVEL, Level.LEVEL_0)) //
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, 0) //
						.timeleap(CLOCK, 1, SECONDS) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 0) //
						.output(LEVEL, Level.LEVEL_1)) //
				.deactivate();
	}

}
