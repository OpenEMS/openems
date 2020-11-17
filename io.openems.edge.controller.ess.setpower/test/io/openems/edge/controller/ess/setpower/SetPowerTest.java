package io.openems.edge.controller.ess.setpower;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentConfig;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.meter.test.DummySymmetricMeter;

public class SetPowerTest {

	@SuppressWarnings("all")
	private static class MyConfig extends AbstractComponentConfig implements Config {

		private final String[] inputChannelAddress;
		private final String ess_id;
		private final Boolean invert;

		public MyConfig(String id, String[] inputChannelAddress, String ess_id, Boolean invert) {
			super(Config.class, id);
			this.inputChannelAddress = inputChannelAddress;
			this.ess_id = ess_id;
			this.invert = invert;
		}

		@Override
		public String[] inputChannelAddress() {
			return this.inputChannelAddress;
		}

		@Override
		public String ess_id() {
			return this.ess_id;
		}

		@Override
		public boolean use_pid() {
			return false;
		}

//@Override
//public String grid_meter_id() {
//	// TODO Auto-generated method stub
//	return null;
//}
//
//@Override
//public String pv_meter_id() {
//	// TODO Auto-generated method stub
//	return null;
//}
	}

	@Test
	public void test() throws Exception {
		// Initialize Controller
		SetPower controller = new SetPower();

		// Add referenced services
		DummyComponentManager componentManager = new DummyComponentManager();
		controller.componentManager = componentManager;

		// Prepare Channels
		
		String dev = "meter0";
		String channel = "ActivePower";
		String adressParameter = "+" + dev + "/" + channel;
		
		ChannelAddress input0 = new ChannelAddress(dev, channel);
		ChannelAddress output0 = new ChannelAddress("ess0", "ActivePower");

		// Activate (twice, so that reference target is set)
		MyConfig config = new MyConfig("ctrl0", new String[] {adressParameter }, "ess0", false);
		controller.activate(null, config);
		controller.activate(null, config);
		ManagedSymmetricEss essComponent = new DummyManagedSymmetricEss("ess0");
		SymmetricMeter meter = new DummySymmetricMeter("meter0");

		// Build and run test
		new ControllerTest(controller, componentManager, controller, essComponent, meter) //
				.next(new TestCase() //
						.input(input0, 50) //
						.output(output0, 50)); //
	}

}
