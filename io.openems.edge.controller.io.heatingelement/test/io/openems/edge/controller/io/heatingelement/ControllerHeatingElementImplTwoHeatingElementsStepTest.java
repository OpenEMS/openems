package io.openems.edge.controller.io.heatingelement;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.common.sum.Sum.ChannelId.GRID_ACTIVE_POWER;
import static io.openems.edge.controller.io.heatingelement.ControllerIoHeatingElement.ChannelId.LEVEL;
import static io.openems.edge.io.test.DummyInputOutput.ChannelId.INPUT_OUTPUT0;
import static io.openems.edge.io.test.DummyInputOutput.ChannelId.INPUT_OUTPUT1;
import static io.openems.edge.io.test.DummyInputOutput.ChannelId.INPUT_OUTPUT2;
import static io.openems.edge.io.test.DummyInputOutput.ChannelId.INPUT_OUTPUT3;
import static io.openems.edge.io.test.DummyInputOutput.ChannelId.INPUT_OUTPUT4;
import static io.openems.edge.io.test.DummyInputOutput.ChannelId.INPUT_OUTPUT5;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.io.heatingelement.enums.Distribution;
import io.openems.edge.controller.io.heatingelement.enums.Level;
import io.openems.edge.controller.io.heatingelement.enums.Mode;
import io.openems.edge.controller.io.heatingelement.enums.NumberOfHeatingElements;
import io.openems.edge.controller.io.heatingelement.enums.WorkMode;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.io.test.DummyInputOutput;

/**
 * Verifies that {@code buildSteps()} builds the six relay Steps of two heating
 * elements in the order described by the configured {@link Distribution}.
 */
class ControllerHeatingElementImplTwoHeatingElementsStepTest {

	private static final int POWER_A = 2000;
	private static final int POWER_B = 1000;

	private static ControllerTest prepareTest(Distribution distribution) throws Exception {
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
						.setPowerOfPhase(POWER_A) //
						.setMode(Mode.MANUAL_ON) //
						.setDefaultLevel(Level.LEVEL_2) //
						.setWorkMode(WorkMode.NONE) //
						.setMinTime(1) //
						.setMinimumSwitchingTime(0) //
						.setMinEnergylimit(5000) //
						.setEndTimeWithMeter("00:00") //
						.setMeterid("") //
						.setScheduler("") //
						.setNumberOfHeatingElements(NumberOfHeatingElements.TWO) //
						.setPowerPerPhaseHeatingElementB(POWER_B) //
						.setOutputChannelPhaseL1HeatingElementB("io0/InputOutput3") //
						.setOutputChannelPhaseL2HeatingElementB("io0/InputOutput4") //
						.setOutputChannelPhaseL3HeatingElementB("io0/InputOutput5") //
						.setDistribution(distribution) //
						.build());
	}

	@Test
	void testSequentialLevel2SwitchesOnlyFirstTwoStepsOfHeatingElementA() throws Exception {
		// SEQUENTIAL order: A-L1, A-L2, A-L3, B-L1, B-L2, B-L3.
		prepareTest(Distribution.SEQUENTIAL) //
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, 0) //
						.output(LEVEL, Level.LEVEL_2) //
						.output("io0", INPUT_OUTPUT0, true) // A-L1
						.output("io0", INPUT_OUTPUT1, true) // A-L2
						.output("io0", INPUT_OUTPUT2, false) // A-L3
						.output("io0", INPUT_OUTPUT3, false) // B-L1
						.output("io0", INPUT_OUTPUT4, false) // B-L2
						.output("io0", INPUT_OUTPUT5, false)) // B-L3
				.deactivate();
	}

	@Test
	void testParallelLevel2SwitchesFirstStepOfBothHeatingElements() throws Exception {
		// PARALLEL order: A-L1, B-L1, A-L2, B-L2, A-L3, B-L3.
		prepareTest(Distribution.PARALLEL) //
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, 0) //
						.output(LEVEL, Level.LEVEL_2) //
						.output("io0", INPUT_OUTPUT0, true) // A-L1
						.output("io0", INPUT_OUTPUT3, true) // B-L1
						.output("io0", INPUT_OUTPUT1, false) // A-L2
						.output("io0", INPUT_OUTPUT4, false) // B-L2
						.output("io0", INPUT_OUTPUT2, false) // A-L3
						.output("io0", INPUT_OUTPUT5, false)) // B-L3
				.deactivate();
	}

	@Test
	void testSequentialNominalLevelPowerForAllLevels() throws Exception {
		// SEQUENTIAL: A-L1, A-L2, A-L3, B-L1, B-L2, B-L3 -> cumulated 0, 2000, 4000,
		// 6000, 7000, 8000, 9000
		var sut = buildSutWithDistribution(Distribution.SEQUENTIAL);

		assertNominalLevelPower(sut, Level.LEVEL_0, 0);
		assertNominalLevelPower(sut, Level.LEVEL_1, 2000);
		assertNominalLevelPower(sut, Level.LEVEL_2, 4000);
		assertNominalLevelPower(sut, Level.LEVEL_3, 6000);
		assertNominalLevelPower(sut, Level.LEVEL_4, 7000);
		assertNominalLevelPower(sut, Level.LEVEL_5, 8000);
		assertNominalLevelPower(sut, Level.LEVEL_6, 9000);
	}

	@Test
	void testParallelNominalLevelPowerForAllLevels() throws Exception {
		// PARALLEL: A-L1, B-L1, A-L2, B-L2, A-L3, B-L3 -> cumulated 0, 2000, 3000,
		// 5000, 6000, 8000, 9000
		var sut = buildSutWithDistribution(Distribution.PARALLEL);

		assertNominalLevelPower(sut, Level.LEVEL_0, 0);
		assertNominalLevelPower(sut, Level.LEVEL_1, 2000);
		assertNominalLevelPower(sut, Level.LEVEL_2, 3000);
		assertNominalLevelPower(sut, Level.LEVEL_3, 5000);
		assertNominalLevelPower(sut, Level.LEVEL_4, 6000);
		assertNominalLevelPower(sut, Level.LEVEL_5, 8000);
		assertNominalLevelPower(sut, Level.LEVEL_6, 9000);
	}

	private static ControllerIoHeatingElementImpl buildSutWithDistribution(Distribution distribution) throws Exception {
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
						.setPowerOfPhase(POWER_A) //
						.setMode(Mode.MANUAL_OFF) //
						.setDefaultLevel(Level.LEVEL_0) //
						.setWorkMode(WorkMode.NONE) //
						.setMinTime(1) //
						.setMinimumSwitchingTime(0) //
						.setMinEnergylimit(5000) //
						.setEndTimeWithMeter("00:00") //
						.setMeterid("") //
						.setScheduler("") //
						.setNumberOfHeatingElements(NumberOfHeatingElements.TWO) //
						.setPowerPerPhaseHeatingElementB(POWER_B) //
						.setOutputChannelPhaseL1HeatingElementB("io0/InputOutput3") //
						.setOutputChannelPhaseL2HeatingElementB("io0/InputOutput4") //
						.setOutputChannelPhaseL3HeatingElementB("io0/InputOutput5") //
						.setDistribution(distribution) //
						.build());
		return sut;
	}

	private static void assertNominalLevelPower(ControllerIoHeatingElementImpl sut, Level level, int expected) {
		assertEquals(expected, sut.getNominalLevelPower(level));
	}
}
