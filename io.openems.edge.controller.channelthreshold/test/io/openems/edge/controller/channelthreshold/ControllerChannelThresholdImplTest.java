package io.openems.edge.controller.channelthreshold;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;

public class ControllerChannelThresholdImplTest {

	private static final String CTRL_ID = "ctrl0";
	private static final ChannelAddress IO0_INPUT = new ChannelAddress("io0", "Input0");
	private static final ChannelAddress IO0_OUTPUT = new ChannelAddress("io0", "Output0");

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerChannelThresholdImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setInputChannelAddress(IO0_INPUT.toString()) //
						.setOutputChannelAddress(IO0_OUTPUT.toString()) //
						.setLowThreshold(40) //
						.setHighThreshold(80) //
						.build()); //
	}

}
