package io.openems.edge.controller.io.heatingelement;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.common.sum.Sum.ChannelId.GRID_ACTIVE_POWER;
import static io.openems.edge.controller.io.heatingelement.ControllerIoHeatingElement.ChannelId.LEVEL;
import static io.openems.edge.controller.io.heatingelement.ControllerIoHeatingElement.ChannelId.STATUS;
import static java.time.temporal.ChronoUnit.MINUTES;

import org.junit.jupiter.api.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.io.heatingelement.enums.Distribution;
import io.openems.edge.controller.io.heatingelement.enums.Level;
import io.openems.edge.controller.io.heatingelement.enums.Mode;
import io.openems.edge.controller.io.heatingelement.enums.NumberOfHeatingElements;
import io.openems.edge.controller.io.heatingelement.enums.Status;
import io.openems.edge.controller.io.heatingelement.enums.WorkMode;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.io.test.DummyInputOutput;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.test.DummyElectricityMeter;

/**
 * Verifies that with {@link NumberOfHeatingElements#TWO} the automatic
 * calibration is skipped, even if a meter is configured: the required Level is
 * always derived from the configured Step powers (2000 W / 1000 W) instead of
 * the calibrated average phase power, and the controller never enters
 * {@link Status#CALIBRATION}.
 */
public class ControllerHeatingElementImplCalibrationTest {

	private static final TimeLeapClock CLOCK = createDummyClock();

	private static ControllerTest prepareTest() throws Exception {
		return new ControllerTest(new ControllerIoHeatingElementImpl()) //
				.addReference("componentManager", new DummyComponentManager(CLOCK)) //
				.addReference("sum", new DummySum()) //
				.addReference("meter", new DummyElectricityMeter("meter3")) //
				.addComponent(new DummyInputOutput("io0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setOutputChannelPhaseL1("io0/InputOutput0") //
						.setOutputChannelPhaseL2("io0/InputOutput1") //
						.setOutputChannelPhaseL3("io0/InputOutput2") //
						.setPowerOfPhase(2000) //
						.setNumberOfHeatingElements(NumberOfHeatingElements.TWO) //
						.setDistribution(Distribution.SEQUENTIAL) //
						.setPowerPerPhaseHeatingElementB(1000) //
						.setOutputChannelPhaseL1HeatingElementB("io0/InputOutput3") //
						.setOutputChannelPhaseL2HeatingElementB("io0/InputOutput4") //
						.setOutputChannelPhaseL3HeatingElementB("io0/InputOutput5") //
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
	void testCalibrationIsSkippedForTwoHeatingElements() throws Exception {
		prepareTest() //
				// Even in the very first cycle - which would trigger CALIBRATION for a
				// single heating element - the required Level must be derived from the
				// configured Step powers (2000/1000 W per Step); Status must never become
				// CALIBRATION.
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, -6000) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 0) //
						.output(STATUS, Status.ACTIVE) //
						.output(LEVEL, Level.LEVEL_1)) //
				.next(new TestCase() //
						.timeleap(CLOCK, 2, MINUTES) //
						.input(GRID_ACTIVE_POWER, -6000) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 0) //
						.output(STATUS, Status.ACTIVE) //
						.output(LEVEL, Level.LEVEL_2)) //
				.next(new TestCase() //
						.timeleap(CLOCK, 2, MINUTES) //
						.input(GRID_ACTIVE_POWER, -6000) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 0) //
						.output(STATUS, Status.ACTIVE) //
						.output(LEVEL, Level.LEVEL_3)) //
				.next(new TestCase() //
						.timeleap(CLOCK, 2, MINUTES) //
						.input(GRID_ACTIVE_POWER, 0) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 0) //
						.output(STATUS, Status.INACTIVE) //
						.output(LEVEL, Level.LEVEL_2)) //
				.deactivate();
	}
}
