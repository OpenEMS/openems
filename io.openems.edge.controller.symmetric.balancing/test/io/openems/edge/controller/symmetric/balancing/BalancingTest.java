package io.openems.edge.controller.symmetric.balancing;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.test.AbstractComponentConfig;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.meter.test.DummySymmetricMeter;

public class BalancingTest {

	@SuppressWarnings("all")
	private static class MyConfig extends AbstractComponentConfig implements Config {

		private final String essId;
		private final String meterId;

		public MyConfig(String id, String essId, String meterId) {
			super(Config.class, id);
			this.essId = essId;
			this.meterId = meterId;
		}

		@Override
		public String ess_id() {
			return this.essId;
		}

		@Override
		public String meter_id() {
			return this.meterId;
		}

		@Override
		public int targetGridSetpoint() {
			return 0;
		}
	}

	@Test
	public void test() throws Exception {
		// Initialize Controller
		Balancing controller = new Balancing();
		// Add referenced services
		DummyComponentManager componentManager = new DummyComponentManager();
		controller.componentManager = componentManager;
		// Activate (twice, so that reference target is set)
		MyConfig config = new MyConfig("ctrl0", "ess0", "meter0");
		controller.activate(null, config);
		controller.activate(null, config);
		// Prepare Channels
		ChannelAddress ess0GridMode = new ChannelAddress("ess0", "GridMode");
		ChannelAddress ess0ActivePower = new ChannelAddress("ess0", "ActivePower");
		ChannelAddress meter0ActivePower = new ChannelAddress("meter0", "ActivePower");
		ChannelAddress ess0SetActivePowerEquals = new ChannelAddress("ess0", "SetActivePowerEquals");
		// Build and run test
		ManagedSymmetricEss ess = new DummyManagedSymmetricEss("ess0");
		SymmetricMeter meter = new DummySymmetricMeter("meter0");
		new ControllerTest(controller, componentManager, ess, meter) //
				.next(new TestCase() //
						.input(ess0GridMode, GridMode.ON_GRID) //
						.input(ess0ActivePower, 1000).input(meter0ActivePower, 2000) //
						.output(ess0SetActivePowerEquals, 3000)) //
				.next(new TestCase() //
						.input(ess0ActivePower, 1500).input(meter0ActivePower, 2500) //
						.output(ess0SetActivePowerEquals, 3600)) //
				.next(new TestCase() //
						.input(ess0ActivePower, 1500).input(meter0ActivePower, 2500) //
						.output(ess0SetActivePowerEquals, 4000)) //
				.run();
	}

}
