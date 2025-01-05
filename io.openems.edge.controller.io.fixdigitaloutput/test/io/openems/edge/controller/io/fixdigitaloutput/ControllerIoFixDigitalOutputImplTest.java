package io.openems.edge.controller.io.fixdigitaloutput;

import static io.openems.edge.io.test.DummyInputOutput.ChannelId.INPUT_OUTPUT0;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.io.test.DummyInputOutput;

public class ControllerIoFixDigitalOutputImplTest {

	@Test
	public void testOn() throws Exception {
		this.testSwitch(true);
	}

	@Test
	public void testOff() throws Exception {
		this.testSwitch(false);
	}

	private void testSwitch(boolean on) throws Exception {
		new ControllerTest(new ControllerIoFixDigitalOutputImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(new DummyInputOutput("io0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setOutputChannelAddress("io0/InputOutput0") //
						.setOn(on) //
						.build())
				.next(new TestCase() //
						.output("io0", INPUT_OUTPUT0, on)) //
				.deactivate();
	}
}
