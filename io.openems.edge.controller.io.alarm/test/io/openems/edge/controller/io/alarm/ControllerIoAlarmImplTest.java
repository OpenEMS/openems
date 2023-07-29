package io.openems.edge.controller.io.alarm;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.io.test.DummyInputOutput;

public class ControllerIoAlarmImplTest {

	private static final String CTRL_ID = "ctrl0";

	private static final String DUMMY_ID = "dummy0";
	private static final ChannelAddress DUMMY_STATE0 = new ChannelAddress(DUMMY_ID, "State0");
	private static final ChannelAddress DUMMY_STATE1 = new ChannelAddress(DUMMY_ID, "State1");

	private static final String IO_ID = "io0";
	private static final ChannelAddress IO_INPUT_OUTPUT0 = new ChannelAddress(IO_ID, "InputOutput0");

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerIoAlarmImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(new DummyComponent(DUMMY_ID)) //
				.addComponent(new DummyInputOutput(IO_ID)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setInputChannelAddresses(//
								DUMMY_STATE0.toString(), //
								DUMMY_STATE1.toString())
						.setOutputChannelAddress(IO_INPUT_OUTPUT0.toString()) //
						.build())
				.next(new TestCase() //
						.input(DUMMY_STATE0, true) //
						.input(DUMMY_STATE1, false) //
						.output(IO_INPUT_OUTPUT0, true)) //
				.next(new TestCase() //
						.input(DUMMY_STATE0, false) //
						.input(DUMMY_STATE1, true) //
						.output(IO_INPUT_OUTPUT0, true)) //
				.next(new TestCase() //
						.input(DUMMY_STATE0, true) //
						.input(DUMMY_STATE1, true) //
						.output(IO_INPUT_OUTPUT0, true))
				.next(new TestCase() //
						.input(DUMMY_STATE0, false) //
						.input(DUMMY_STATE1, false) //
						.output(IO_INPUT_OUTPUT0, false));
	}

}
