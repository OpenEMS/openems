package io.openems.edge.controller.io.alarm;

import static io.openems.edge.controller.io.alarm.DummyComponent.ChannelId.STATE_0;
import static io.openems.edge.controller.io.alarm.DummyComponent.ChannelId.STATE_1;
import static io.openems.edge.io.test.DummyInputOutput.ChannelId.INPUT_OUTPUT0;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.io.test.DummyInputOutput;

public class ControllerIoAlarmImplTest {

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerIoAlarmImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(new DummyComponent("dummy0")) //
				.addComponent(new DummyInputOutput("io0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setInputChannelAddresses("dummy0/State0", "dummy0/State1")
						.setOutputChannelAddress("io0/InputOutput0") //
						.build())
				.next(new TestCase() //
						.input("dummy0", STATE_0, true) //
						.input("dummy0", STATE_1, false) //
						.output("io0", INPUT_OUTPUT0, true)) //
				.next(new TestCase() //
						.input("dummy0", STATE_0, false) //
						.input("dummy0", STATE_1, true) //
						.output("io0", INPUT_OUTPUT0, true)) //
				.next(new TestCase() //
						.input("dummy0", STATE_0, true) //
						.input("dummy0", STATE_1, true) //
						.output("io0", INPUT_OUTPUT0, true))
				.next(new TestCase() //
						.input("dummy0", STATE_0, false) //
						.input("dummy0", STATE_1, false) //
						.output("io0", INPUT_OUTPUT0, false)) //
				.deactivate();
	}

}
