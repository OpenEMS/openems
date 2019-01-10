package io.openems.edge.controller.io.fixdigitaloutput;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentConfig;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.io.test.DummyInputOutput;

public class FixDigitalOutputTest {

	@SuppressWarnings("all")
	private static class MyConfig extends AbstractComponentConfig implements Config {

		private final String outputChannelAddress;
		private final boolean isOn;

		public MyConfig(String id, String outputChannelAddress, boolean isOn) {
			super(Config.class, id);
			this.outputChannelAddress = outputChannelAddress;
			this.isOn = isOn;
		}

		@Override
		public String outputChannelAddress() {
			return this.outputChannelAddress;
		}

		@Override
		public boolean isOn() {
			return this.isOn;
		}

	}

	@Test
	public void testOn() throws Exception {
		this.testSwitch(true);
	}

	@Test
	public void testOff() throws Exception {
		this.testSwitch(false);
	}

	private void testSwitch(boolean on) throws Exception {
		// Initialize Controller
		FixDigitalOutput controller = new FixDigitalOutput();
		// Add referenced services
		controller.cm = new DummyConfigurationAdmin();
		DummyComponentManager componentManager = new DummyComponentManager();
		controller.componentManager = componentManager;
		// Activate (twice, so that reference target is set)s
		ChannelAddress output0 = new ChannelAddress("io0", "InputOutput0");
		MyConfig config = new MyConfig("ctrl0", output0.toString(), on);
		controller.activate(null, config);
		controller.activate(null, config);
		// Build and run test
		DummyInputOutput io0 = new DummyInputOutput("io0");
		new ControllerTest(controller, componentManager, io0) //
				.next(new TestCase().output(output0, on)) //
				.run();
	}

}
