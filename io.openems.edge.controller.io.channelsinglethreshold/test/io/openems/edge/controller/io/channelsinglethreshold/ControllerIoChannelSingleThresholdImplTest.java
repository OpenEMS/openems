package io.openems.edge.controller.io.channelsinglethreshold;

import static io.openems.edge.controller.io.channelsinglethreshold.ControllerIoChannelSingleThreshold.ChannelId.AWAITING_HYSTERESIS;
import static io.openems.edge.ess.api.SymmetricEss.ChannelId.SOC;
import static io.openems.edge.io.test.DummyInputOutput.ChannelId.INPUT_OUTPUT0;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.io.test.DummyInputOutput;

public class ControllerIoChannelSingleThresholdImplTest {

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerIoChannelSingleThresholdImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(new DummyManagedSymmetricEss("ess0")) //
				.addComponent(new DummyInputOutput("io0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setMode(Mode.AUTOMATIC) //
						.setInputChannelAddress("ess0/Soc") //
						.setOutputChannelAddress("io0/InputOutput0") //
						.setThreshold(70) //
						.setSwitchedLoadPower(0) //
						.setMinimumSwitchingTime(60).setInvert(false) //
						.build())
				.next(new TestCase() //
						.input("ess0", SOC, 50) //
						.output("io0", INPUT_OUTPUT0, false) //
						.output(AWAITING_HYSTERESIS, false)) //
				.deactivate();
	}

}
