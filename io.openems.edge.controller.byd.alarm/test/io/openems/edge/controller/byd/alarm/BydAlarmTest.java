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

		/*
		 * Generate the array of input channels and its corresponding values
		 */
		ArrayList<ChannelAddress> channelAddress = new ArrayList<ChannelAddress>();
		ArrayList<Object> value = new ArrayList<Object>();
		
		ChannelAddress ess0 = new ChannelAddress("ess0", "State0");
		Boolean ess0_value = true;
		ChannelAddress ess1 = new ChannelAddress("ess0", "State1");
		Boolean ess1_value = false;
		
		
		channelAddress.add(ess0);
		value.add(ess0_value);
		channelAddress.add(ess1);
		//value.add(ess1_value);
		
		
		/*
		 * Generate the array of Output channels 
		 */
		ChannelAddress output0 = new ChannelAddress("io0", "InputOutput0");
		
		
		/*
		 * Generate the array for constructor
		 */
		String[] inAddress = new String[channelAddress.size()];

		for (int i = 0; i < channelAddress.size(); i++) {
			inAddress[i] = channelAddress.get(i).toString();
		}

		
		MyConfig myconfig = new MyConfig("ctrl1", inAddress, output0.toString());

		controller.activate(null, myconfig);
		controller.activate(null, myconfig);

		DummyComponent ess_0 = new DummyComponent("ess0");
		
	/*
	 * Generate the array of input channels 
	 */
		/*
		 * ChannelAddress[] channel = new ChannelAddress[channelAddress.size()];
		 * Object[] object = new Object[channelAddress.size()];
		 * 
		 * for (int i = 0; i < channelAddress.size(); i++) { channel[i] =
		 * channelAddress.get(i); object[i] = false; }
		 */
		
		
		DummyInputOutput io = new DummyInputOutput("io0");

		new ControllerTest(controller, componentManager, ess_0, io)//
				.next(new TestCase() //
						.input(channelAddress, value) //
						.output(output0, true)) //
				.run();

	}

}
