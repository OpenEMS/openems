package io.openems.edge.controller.io.heatingelement;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.common.sum.Sum.ChannelId.GRID_ACTIVE_POWER;
import static io.openems.edge.controller.io.heatingelement.ControllerIoHeatingElement.ChannelId.LEVEL;
import static io.openems.edge.io.test.DummyInputOutput.ChannelId.INPUT_OUTPUT0;
import static io.openems.edge.io.test.DummyInputOutput.ChannelId.INPUT_OUTPUT1;
import static io.openems.edge.io.test.DummyInputOutput.ChannelId.INPUT_OUTPUT2;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.io.heatingelement.enums.Level;
import io.openems.edge.controller.io.heatingelement.enums.Mode;
import io.openems.edge.controller.io.heatingelement.enums.WorkMode;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.io.test.DummyInputOutput;

/**
 * Verifies that the refactored, step-based {@code applyLevel()} still switches
 * exactly the same relays as the previous per-Level implementation and that the
 * nominal Level power still equals {@code Level * powerPerPhase} for the
 * current equal-power 3-relay setup.
 */
class ControllerHeatingElementImplStepTest {

	private static final int POWER_PER_PHASE = 2000;

	private static ControllerTest prepareTest(Level defaultLevel) throws Exception {
		final var clock = createDummyClock();
		return new ControllerTest(new ControllerIoHeatingElementImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("sum", new DummySum()) //
				.addComponent(new DummyInputOutput("io0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setOutputChannelPhaseL1("io0/InputOutput0") //
						.setOutputChannelPhaseL2("io0/InputOutput1") //
						.setOutputChannelPhaseL3("io0/InputOutput2") //
						.setEndTime("15:45:00") //
						.setPowerOfPhase(POWER_PER_PHASE) //
						.setMode(Mode.MANUAL_ON) //
						.setDefaultLevel(defaultLevel) //
						.setWorkMode(WorkMode.NONE) //
						.setMinTime(1) //
						.setMinimumSwitchingTime(0) //
						.setMinEnergylimit(5000) //
						.setEndTimeWithMeter("00:00") //
						.setMeterid("") //
						.setScheduler("") //
						.build());
	}

	@Test
	void testLevel0SwitchesAllRelaysOff() throws Exception {
		prepareTest(Level.LEVEL_0) //
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, 0) //
						.output(LEVEL, Level.LEVEL_0) //
						.output("io0", INPUT_OUTPUT0, false) //
						.output("io0", INPUT_OUTPUT1, false) //
						.output("io0", INPUT_OUTPUT2, false)) //
				.deactivate();
	}

	@Test
	void testLevel1SwitchesOnlyFirstRelayOn() throws Exception {
		prepareTest(Level.LEVEL_1) //
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, 0) //
						.output(LEVEL, Level.LEVEL_1) //
						.output("io0", INPUT_OUTPUT0, true) //
						.output("io0", INPUT_OUTPUT1, false) //
						.output("io0", INPUT_OUTPUT2, false)) //
				.deactivate();
	}

	@Test
	void testLevel2SwitchesFirstTwoRelaysOn() throws Exception {
		prepareTest(Level.LEVEL_2) //
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, 0) //
						.output(LEVEL, Level.LEVEL_2) //
						.output("io0", INPUT_OUTPUT0, true) //
						.output("io0", INPUT_OUTPUT1, true) //
						.output("io0", INPUT_OUTPUT2, false)) //
				.deactivate();
	}

	@Test
	void testLevel3SwitchesAllRelaysOn() throws Exception {
		prepareTest(Level.LEVEL_3) //
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, 0) //
						.output(LEVEL, Level.LEVEL_3) //
						.output("io0", INPUT_OUTPUT0, true) //
						.output("io0", INPUT_OUTPUT1, true) //
						.output("io0", INPUT_OUTPUT2, true)) //
				.deactivate();
	}

	@Test
	void testNominalLevelPowerMatchesLevelTimesPowerPerPhase() throws Exception {
		var sut = new ControllerIoHeatingElementImpl();
		new ControllerTest(sut) //
				.addReference("componentManager", new DummyComponentManager(createDummyClock())) //
				.addReference("sum", new DummySum()) //
				.addComponent(new DummyInputOutput("io0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setOutputChannelPhaseL1("io0/InputOutput0") //
						.setOutputChannelPhaseL2("io0/InputOutput1") //
						.setOutputChannelPhaseL3("io0/InputOutput2") //
						.setEndTime("15:45:00") //
						.setPowerOfPhase(POWER_PER_PHASE) //
						.setMode(Mode.MANUAL_OFF) //
						.setDefaultLevel(Level.LEVEL_0) //
						.setWorkMode(WorkMode.NONE) //
						.setMinTime(1) //
						.setMinimumSwitchingTime(0) //
						.setMinEnergylimit(5000) //
						.setEndTimeWithMeter("00:00") //
						.setMeterid("") //
						.setScheduler("") //
						.build());

		assertEquals(0, sut.getNominalLevelPower(Level.LEVEL_0));
		assertEquals(1 * POWER_PER_PHASE, sut.getNominalLevelPower(Level.LEVEL_1));
		assertEquals(2 * POWER_PER_PHASE, sut.getNominalLevelPower(Level.LEVEL_2));
		assertEquals(3 * POWER_PER_PHASE, sut.getNominalLevelPower(Level.LEVEL_3));
	}
}
