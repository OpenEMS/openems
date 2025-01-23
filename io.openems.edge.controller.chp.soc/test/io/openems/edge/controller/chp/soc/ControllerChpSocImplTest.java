package io.openems.edge.controller.chp.soc;

import static io.openems.edge.ess.api.SymmetricEss.ChannelId.SOC;
import static io.openems.edge.io.test.DummyInputOutput.ChannelId.INPUT_OUTPUT0;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.io.test.DummyInputOutput;

public class ControllerChpSocImplTest {

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerChpSocImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(new DummyManagedSymmetricEss("ess0")) //
				.addComponent(new DummyInputOutput("io0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setInputChannelAddress("ess0/Soc") //
						.setOutputChannelAddress("io0/InputOutput0") //
						.setLowThreshold(15) //
						.setHighThreshold(85) //
						.setMode(Mode.AUTOMATIC) //
						.setInvert(false) //
						.build())
				.next(new TestCase() //
						.input("ess0", SOC, 14) //
						.output("io0", INPUT_OUTPUT0, true)) //
				.next(new TestCase() //
						.input("ess0", SOC, 50) //
						.output("io0", INPUT_OUTPUT0, null)) //
				.next(new TestCase() //
						.input("ess0", SOC, 90) //
						.output("io0", INPUT_OUTPUT0, false)) //
				.next(new TestCase() //
						.input("ess0", SOC, 50) //
						.output("io0", INPUT_OUTPUT0, null)) //
				.next(new TestCase() //
						.input("ess0", SOC, 15) //
						.output("io0", INPUT_OUTPUT0, true)) //
				.next(new TestCase() //
						.input("ess0", SOC, 85) //
						.output("io0", INPUT_OUTPUT0, false)) //
				.next(new TestCase() //
						.input("ess0", SOC, 86) //
						.output("io0", INPUT_OUTPUT0, false)) //
				.next(new TestCase() //
						.input("ess0", SOC, 14) //
						.output("io0", INPUT_OUTPUT0, true)) //
				.next(new TestCase() //
						.input("ess0", SOC, 45) //
						.output("io0", INPUT_OUTPUT0, null)) //
				.deactivate();
	}

}