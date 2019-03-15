package io.openems.edge.controller.byd.alarm;

import java.util.ArrayList;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentConfig;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.io.test.DummyInputOutput;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;

public class BydAlarmTest {

	@SuppressWarnings("all")
	private static class MyConfig extends AbstractComponentConfig implements Config {

		private final String[] inputChannelAddress;
		private final String outputChannelAddress;

		public MyConfig(String id, String[] inputChannelAddress, String outputChannelAddress) {

			super(Config.class, id);
			this.inputChannelAddress = inputChannelAddress;
			this.outputChannelAddress = outputChannelAddress;

		}

		@Override
		public String[] inputChannelAddress() {
			return this.inputChannelAddress;
		}

		@Override
		public String outputChannelAddress() {
			return this.outputChannelAddress;
		}

	}

	@Test
	public void test() throws Exception {
		// initialize the controller
		BydAlarm controller = new BydAlarm();
		// Add referenced services
		DummyComponentManager componentManager = new DummyComponentManager();
		controller.componentManager = componentManager;

		ArrayList<ChannelAddress> channelAddress = new ArrayList<ChannelAddress>();
		ChannelAddress ess0 = new ChannelAddress("ess0", "State1");
		//ChannelAddress ess1 = new ChannelAddress("ess0", "State2");
		channelAddress.add(ess0);
		//channelAddress.add(ess1);
		ChannelAddress output0 = new ChannelAddress("io0", "InputOutput0");
		String[] inAddress = new String[channelAddress.size()];

		for (int i = 0; i < channelAddress.size(); i++) {
			inAddress[i] = channelAddress.get(i).toString();
		}

		MyConfig myconfig = new MyConfig("ctrl1", inAddress, output0.toString());

		controller.activate(null, myconfig);
		controller.activate(null, myconfig);

		DummyComponent ess = new DummyComponent("ess0");
		DummyInputOutput io = new DummyInputOutput("io0");

		new ControllerTest(controller, componentManager, ess, io)//
				.next(new TestCase() //
						.input(ess0, "true") //
						.input(ess0, "false") //
						.output(output0, true)) //
				.run();

	}

}
