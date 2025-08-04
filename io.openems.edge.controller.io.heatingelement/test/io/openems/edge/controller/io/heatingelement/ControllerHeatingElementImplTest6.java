package io.openems.edge.controller.io.heatingelement;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.common.sum.Sum.ChannelId.GRID_ACTIVE_POWER;
import static io.openems.edge.controller.io.heatingelement.ControllerIoHeatingElement.ChannelId.LEVEL;
import static java.time.temporal.ChronoUnit.MINUTES;
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
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.io.test.DummyInputOutput;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.test.DummyElectricityMeter;

/**
 * A test to check if the controller has the correct behavior with different
 * grid power values and a reference to a meter.
 */
public class ControllerHeatingElementImplTest6 {

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
						.setWorkMode(WorkMode.NONE) //
						.setDefaultLevel(Level.LEVEL_1) //
						.setMeterid("meter3") //
						.setEndTime("00:00") //
						.setMinTime(1) //
						.setMinimumSwitchingTime(60) //
						.setMinEnergylimit(5000) //
						.setEndTimeWithMeter("12:00") //
						.setScheduler("") //
						.build()); //
	}

	@Test
	public void testWithFixedValues() throws Exception {
		prepareTest() //
				.next(new TestCase() //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 5200) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 1700) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 1800) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 1700) //
						.output(LEVEL, Level.LEVEL_3), 5) //
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, -5300) //
						.timeleap(CLOCK, 5, SECONDS) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 0) //
						.output(LEVEL, Level.LEVEL_3)) //
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, -4000) //
						.timeleap(CLOCK, 6 * 60, MINUTES) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 0) //
						.output(LEVEL, Level.LEVEL_2)) //
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, -500) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 3500) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 1700) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 1800) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 0) //
						.output(LEVEL, Level.LEVEL_2)) //
				.next(new TestCase() //
						.timeleap(CLOCK, 2, MINUTES) //
						.input(GRID_ACTIVE_POWER, -2100) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 3500) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 1700) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 1800) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 0) //
						.output(LEVEL, Level.LEVEL_3)) //
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, -500) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 5200) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 1700) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 1800) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 1700) //
						.output(LEVEL, Level.LEVEL_3)) //
				.next(new TestCase() //
						.timeleap(CLOCK, 2, MINUTES) //
						.input(GRID_ACTIVE_POWER, 200) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 5200) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 1700) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 1800) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 1700) //
						.output(LEVEL, Level.LEVEL_2)) //
				.next(new TestCase() //
						.timeleap(CLOCK, 2, MINUTES) //
						.input(GRID_ACTIVE_POWER, 20000) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 3500) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 1700) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 1800) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 0) //
						.output(LEVEL, Level.LEVEL_0)) //
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 0) //
						.output(LEVEL, Level.LEVEL_0)) //
				.deactivate();

	}
}
