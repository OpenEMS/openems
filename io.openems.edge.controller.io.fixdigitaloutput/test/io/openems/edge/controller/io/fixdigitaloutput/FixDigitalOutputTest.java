package io.openems.edge.controller.io.fixdigitaloutput;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.io.test.DummyInputOutput;

public class FixDigitalOutputTest {

	private static final String CTRL_ID = "ctrl0";

	private static final String IO_ID = "io0";
	private static final ChannelAddress IO_INPUT_OUTPUT0 = new ChannelAddress(IO_ID, "InputOutput0");

	@Test
	public void testOn() throws Exception {
		this.testSwitch(true);
	}

	@Test
	public void testOff() throws Exception {
		this.testSwitch(false);
	}

	private void testSwitch(boolean on) throws Exception {
		new ControllerTest(new FixDigitalOutputImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(new DummyInputOutput(IO_ID)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setOutputChannelAddress(IO_INPUT_OUTPUT0.toString()) //
						.setOn(on) //
						.build())
				.next(new TestCase() //
						.output(IO_INPUT_OUTPUT0, on));
	}

}
