package io.openems.edge.controller.io.heatingelement;

import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.test.TimeLeapClock;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.io.heatingelement.enums.Level;
import io.openems.edge.controller.io.heatingelement.enums.Mode;
import io.openems.edge.controller.io.heatingelement.enums.WorkMode;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.io.test.DummyInputOutput;

public class ControllerIoHeatingElementImplTest3 {

	private static final String CTRL_ID = "ctrl0";
	private static final String IO_ID = "io0";

	private static final ChannelAddress IO_OUTPUT1 = new ChannelAddress(IO_ID, "InputOutput1");
	private static final ChannelAddress IO_OUTPUT2 = new ChannelAddress(IO_ID, "InputOutput2");
	private static final ChannelAddress IO_OUTPUT3 = new ChannelAddress(IO_ID, "InputOutput3");

	private static ControllerTest prepareTest(Mode mode, Level level) throws OpenemsNamedException, Exception {
		return new ControllerTest(new ControllerIoHeatingElementImpl()) //
				.addReference("componentManager",
						new DummyComponentManager(new TimeLeapClock(
								Instant.ofEpochSecond(1577836800) /* starts at 1. January 2020 00:00:00 */,
								ZoneOffset.UTC))) //
				.addReference("sum", new DummySum()) //
				.addComponent(new DummyInputOutput("io0")) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setOutputChannelPhaseL1(IO_OUTPUT1.toString()) //
						.setOutputChannelPhaseL2(IO_OUTPUT2.toString()) //
						.setOutputChannelPhaseL3(IO_OUTPUT3.toString()) //
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
						.output(IO_OUTPUT1, false) //
						.output(IO_OUTPUT2, false) //
						.output(IO_OUTPUT3, false) //
				);
	}

	@Test
	public void testOnLevel0() throws Exception {
		prepareTest(Mode.MANUAL_ON, Level.LEVEL_0) //
				.next(new TestCase() //
						.output(IO_OUTPUT1, false) //
						.output(IO_OUTPUT2, false) //
						.output(IO_OUTPUT3, false) //
				);
	}

	@Test
	public void testOnLevel1() throws Exception {
		prepareTest(Mode.MANUAL_ON, Level.LEVEL_1) //
				.next(new TestCase() //
						.output(IO_OUTPUT1, true) //
						.output(IO_OUTPUT2, false) //
						.output(IO_OUTPUT3, false) //
				);
	}

	@Test
	public void testOnLevel2() throws Exception {
		prepareTest(Mode.MANUAL_ON, Level.LEVEL_2) //
				.next(new TestCase() //
						.output(IO_OUTPUT1, true) //
						.output(IO_OUTPUT2, true) //
						.output(IO_OUTPUT3, false) //
				);
	}

	@Test
	public void testOnLevel3() throws Exception {
		prepareTest(Mode.MANUAL_ON, Level.LEVEL_3) //
				.next(new TestCase() //
						.output(IO_OUTPUT1, true) //
						.output(IO_OUTPUT2, true) //
						.output(IO_OUTPUT3, true) //
				);
	}

}
