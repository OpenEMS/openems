package io.openems.edge.controller.io.alarm;

import java.util.ArrayList;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentConfig;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.io.test.DummyInputOutput;

public class IoAlarmTest {

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
		IoAlarm controller = new IoAlarm();
		// Add referenced services
		DummyComponentManager componentManager = new DummyComponentManager();
		controller.componentManager = componentManager;

		ArrayList<ChannelAddress> channelAddress = new ArrayList<ChannelAddress>();		
		
		ChannelAddress ess0State0 = new ChannelAddress("ess0", "State0");		
		ChannelAddress ess0State1 = new ChannelAddress("ess0", "State1");			
		
		channelAddress.add(ess0State0);		
		channelAddress.add(ess0State1);				
		
		ChannelAddress output0 = new ChannelAddress("io0", "InputOutput0");
				
		String[] inAddress = new String[channelAddress.size()];

		for (int i = 0; i < channelAddress.size(); i++) {
			inAddress[i] = channelAddress.get(i).toString();
		}
		
		MyConfig myconfig = new MyConfig("ctrl1", inAddress, output0.toString());

		controller.activate(null, myconfig);
		controller.activate(null, myconfig);		
		
		DummyComponent ess0 = new DummyComponent("ess0");
		DummyInputOutput io = new DummyInputOutput("io0");

		new ControllerTest(controller, componentManager, ess0, io)//
				.next(new TestCase() //
						.input(ess0State0, true) //
						.input(ess0State1, false) //
						.output(output0, true)) //
				.next(new TestCase() //
						.input(ess0State0, false) //
						.input(ess0State1, true) //
						.output(output0, true)) //
				.next(new TestCase()
						.input(ess0State0, true) //
				        .input(ess0State1, true) //
				        .output(output0, true))
				.next(new TestCase()
						.input(ess0State0, false) //
				        .input(ess0State1, false) //
				        .output(output0, false))
				.run();

	}

}
