package io.openems.edge.controller.io.heatingelement;

import static io.openems.edge.io.test.DummyInputOutput.ChannelId.INPUT_OUTPUT0;
import static io.openems.edge.io.test.DummyInputOutput.ChannelId.INPUT_OUTPUT1;
import static io.openems.edge.io.test.DummyInputOutput.ChannelId.INPUT_OUTPUT2;

import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.io.heatingelement.enums.Level;
import io.openems.edge.controller.io.heatingelement.enums.Mode;
import io.openems.edge.controller.io.heatingelement.enums.WorkMode;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.io.test.DummyInputOutput;

public class ControllerIoHeatingElementImplTest3 {

	private static ControllerTest prepareTest(Mode mode, Level level) throws OpenemsNamedException, Exception {
		return new ControllerTest(new ControllerIoHeatingElementImpl()) //
				.addReference("componentManager",
						new DummyComponentManager(new TimeLeapClock(
								Instant.ofEpochSecond(1577836800) /* starts at 1. January 2020 00:00:00 */,
								ZoneOffset.UTC))) //
				.addReference("sum", new DummySum()) //
				.addComponent(new DummyInputOutput("io0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setOutputChannelPhaseL1("io0/InputOutput0") //
						.setOutputChannelPhaseL2("io0/InputOutput1") //
						.setOutputChannelPhaseL3("io0/InputOutput2") //
						.setEndTime("15:45:00") //
						.setPowerOfPhase(2000) //
						.setMode(mode) //
						.setDefaultLevel(level) //
						.setWorkMode(WorkMode.TIME) //
						.setMinTime(1) //
						.setMinimumSwitchingTime(60) //
						.build()); //
	}

	@Test
	public void testOff() throws Exception {
		prepareTest(Mode.MANUAL_OFF, Level.LEVEL_3) //
				.next(new TestCase() //
						.output("io0", INPUT_OUTPUT0, false) //
						.output("io0", INPUT_OUTPUT1, false) //
						.output("io0", INPUT_OUTPUT2, false)) //
				.deactivate();
	}

	@Test
	public void testOnLevel0() throws Exception {
		prepareTest(Mode.MANUAL_ON, Level.LEVEL_0) //
				.next(new TestCase() //
						.output("io0", INPUT_OUTPUT0, false) //
						.output("io0", INPUT_OUTPUT1, false) //
						.output("io0", INPUT_OUTPUT2, false)) //
				.deactivate();
	}

	@Test
	public void testOnLevel1() throws Exception {
		prepareTest(Mode.MANUAL_ON, Level.LEVEL_1) //
				.next(new TestCase() //
						.output("io0", INPUT_OUTPUT0, true) //
						.output("io0", INPUT_OUTPUT1, false) //
						.output("io0", INPUT_OUTPUT2, false)) //
				.deactivate();
	}

	@Test
	public void testOnLevel2() throws Exception {
		prepareTest(Mode.MANUAL_ON, Level.LEVEL_2) //
				.next(new TestCase() //
						.output("io0", INPUT_OUTPUT0, true) //
						.output("io0", INPUT_OUTPUT1, true) //
						.output("io0", INPUT_OUTPUT2, false)) //
				.deactivate();
	}

	@Test
	public void testOnLevel3() throws Exception {
		prepareTest(Mode.MANUAL_ON, Level.LEVEL_3) //
				.next(new TestCase() //
						.output("io0", INPUT_OUTPUT0, true) //
						.output("io0", INPUT_OUTPUT1, true) //
						.output("io0", INPUT_OUTPUT2, true)) //
				.deactivate();
	}

}
