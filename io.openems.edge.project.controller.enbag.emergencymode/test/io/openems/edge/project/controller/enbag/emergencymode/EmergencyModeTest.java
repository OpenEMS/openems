package io.openems.edge.project.controller.enbag.emergencymode;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentConfig;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.io.test.DummyInputOutput;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.meter.test.DummySymmetricMeter;

public class EmergencyModeTest {

	@SuppressWarnings("all")
	private static class MyConfig extends AbstractComponentConfig implements Config {
		private final Boolean gridFeedLimitation;
		private final int maxGridFeedPower;
		private final String pvInverter_Id;
		private final String q1ChannelAddress;
		private final String q2ChannelAddress;
		private final String q3ChannelAddress;
		private final String q4ChannelAddress;
		private final String gridMeter_id;
		private final String pvMeter_id;
		private final String ess1_id;
		private final String ess2_id;

		public MyConfig(String id, String ess1_id, String ess2_id, String q1ChannelAddress, String q2ChannelAddress,
				String q3ChannelAddress, String q4ChannelAddress, String pvInverter_Id, String pvMeter_id,
				String gridMeter_id, Boolean gridFeedLimitation, int maxGridFeedPower) {
			super(Config.class, id);
			this.gridFeedLimitation = gridFeedLimitation;
			this.maxGridFeedPower = maxGridFeedPower;
			this.pvInverter_Id = pvInverter_Id;
			this.q1ChannelAddress = q1ChannelAddress;
			this.q2ChannelAddress = q2ChannelAddress;
			this.q3ChannelAddress = q3ChannelAddress;
			this.q4ChannelAddress = q4ChannelAddress;
			this.gridMeter_id = gridMeter_id;
			this.pvMeter_id = pvMeter_id;
			this.ess1_id = ess1_id;
			this.ess2_id = ess2_id;
		}

		@Override
		public boolean gridFeedLimitation() {
			return this.gridFeedLimitation();
		}

		@Override
		public int maxGridFeedPower() {
			return this.maxGridFeedPower();
		}

		@Override
		public String pvInverter_id() {
			return this.pvInverter_id();
		}

		@Override
		public String Q1ChannelAddress() {
			return this.Q1ChannelAddress();
		}

		@Override
		public String Q2ChannelAddress() {
			return this.Q2ChannelAddress();
		}

		@Override
		public String Q3ChannelAddress() {
			return this.Q3ChannelAddress();
		}

		@Override
		public String Q4ChannelAddress() {
			return this.Q4ChannelAddress();
		}

		@Override
		public String gridMeter_id() {
			return this.gridMeter_id;
		}

		@Override
		public String gridMeter_target() {
			return "meter0";
		}

		@Override
		public String pvMeter_id() {
			return this.pvMeter_id;
		}

		@Override
		public String pvMeter_target() {
			return "meter1";
		}

		@Override
		public String ess2_id() {
			return this.ess2_id;
		}

		@Override
		public String ess2_target() {
			return "ess2";
		}

		@Override
		public String ess1_id() {
			return this.ess1_id;
		}

		@Override
		public String ess1_target() {
			return "ess1";
		}

		@Override
		public String pvInverter_target() {
			return "pvInverter0";
		}

	}

	@Test
	public void test() throws Exception {
		// initialize the controller
		EmergencyClusterMode controller = new EmergencyClusterMode();
		// Add referenced services
		DummyComponentManager componentManager = new DummyComponentManager();
		controller.componentManager = componentManager;

		ManagedSymmetricEss ess1 = new DummyManagedSymmetricEss("ess1");
		ChannelAddress ess1Soc = new ChannelAddress("ess1", "Soc");
		
		ManagedSymmetricEss ess2 = new DummyManagedSymmetricEss("ess2");
		ChannelAddress ess2Soc = new ChannelAddress("ess2", "Soc");
		
		ChannelAddress pvInverter0 = new ChannelAddress("pvInverter0", "pvInverter0");
		
		SymmetricMeter meter1 = new DummySymmetricMeter("meter1");
		ChannelAddress meter1ActivePower = new ChannelAddress("meter1", "ActivePower");
		
		
		DummyInputOutput io0 = new DummyInputOutput("io0");
		ChannelAddress io0Q1 = new ChannelAddress("io0", "InputOutput1");
		ChannelAddress io0Q2 = new ChannelAddress("io0", "InputOutput2");
		ChannelAddress io0Q3 = new ChannelAddress("io0", "InputOutput3");
		ChannelAddress io0Q4 = new ChannelAddress("io0", "InputOutput4");

		MyConfig myconfig = new MyConfig("ctrl1", ess1Soc.toString(), ess2Soc.toString(), io0Q1.toString(),
				io0Q2.toString(), io0Q3.toString(), io0Q4.toString(),
				pvInverter0.toString(), meter1ActivePower.toString(), meter0.toString(), true, 4000);
		controller.activate(null, myconfig);
		controller.activate(null, myconfig);

		// Build and run test
		new ControllerTest(controller, componentManager, meter1, ess1, ess2, io0).next(new TestCase()//
				// OffGrid-SwitchTOffGrid-BatteryOkay-PvOkay
				.input(ess1Soc, 50)//
				.input(ess2Soc, 50)//
				.input(io0Q1, false)//
				.input(io0Q2, false)//
				.input(io0Q3, true)//
				.input(io0Q4, false)//
				.input(meter1ActivePower, 5000)//
				.output(io0Q3, false))//
				.run();

	}

}