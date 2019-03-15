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
import io.openems.edge.pvinverter.api.SymmetricPvInverter;
import io.openems.edge.pvinverter.test.DummySymmetricPvInverter;

public class EmergencyModeTest {

	@SuppressWarnings("all")
	private static class MyConfig extends AbstractComponentConfig implements Config {
		private final String q1ChannelAddress;
		private final String q2ChannelAddress;
		private final String q3ChannelAddress;
		private final String q4ChannelAddress;
		private final String pvInverter_id;
		private final String pvMeter_id;
		private final String ess1_id;
		private final String ess2_id;

		public MyConfig(String id, String ess1_id, String ess2_id, String q1ChannelAddress, String q2ChannelAddress,
				String q3ChannelAddress, String q4ChannelAddress, String pvInverter_id, String pvMeter_id) {
			super(Config.class, id);
			this.q1ChannelAddress = q1ChannelAddress;
			this.q2ChannelAddress = q2ChannelAddress;
			this.q3ChannelAddress = q3ChannelAddress;
			this.q4ChannelAddress = q4ChannelAddress;
			this.pvMeter_id = pvMeter_id;
			this.pvInverter_id = pvInverter_id;
			this.ess1_id = ess1_id;
			this.ess2_id = ess2_id;
		}

		@Override
		public String pvInverter_id() {
			return this.pvInverter_id();
		}

		@Override
		public String Q1ChannelAddress() {
			return this.q1ChannelAddress;
		}

		@Override
		public String Q2ChannelAddress() {
			return this.q2ChannelAddress;
		}

		@Override
		public String Q3ChannelAddress() {
			return this.q3ChannelAddress;
		}

		@Override
		public String Q4ChannelAddress() {
			return this.q4ChannelAddress;
		}

		@Override
		public String pvMeter_id() {
			return this.pvMeter_id;
		}

		@Override
		public String pvMeter_target() {
			return "";
		}

		@Override
		public String ess2_id() {
			return this.ess2_id;
		}

		@Override
		public String ess2_target() {
			return "";
		}

		@Override
		public String ess1_id() {
			return this.ess1_id;
		}

		@Override
		public String ess1_target() {
			return "";
		}

		@Override
		public String pvInverter_target() {
			return "";
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

		SymmetricMeter meter1 = new DummySymmetricMeter("meter1");
		ChannelAddress meter1ActivePower = new ChannelAddress("meter1", "ActivePower");

		SymmetricPvInverter pvInverter0 = new DummySymmetricPvInverter("pvInverter0");
		ChannelAddress pvInverterActivePower = new ChannelAddress("pvInverter0", "ActivePowerLimit");

		DummyInputOutput io0 = new DummyInputOutput("io0");

		ChannelAddress io0Q1 = new ChannelAddress("io0", "InputOutput1");
		ChannelAddress io0Q2 = new ChannelAddress("io0", "InputOutput2");
		ChannelAddress io0Q3 = new ChannelAddress("io0", "InputOutput3");
		ChannelAddress io0Q4 = new ChannelAddress("io0", "InputOutput4");

		MyConfig myconfig = new MyConfig("ctrl1", ess1Soc.toString(), ess2Soc.toString(), io0Q1.toString(),
				io0Q2.toString(), io0Q3.toString(), io0Q4.toString(), pvInverterActivePower.toString(),
				meter1ActivePower.toString());
		controller.activate(null, myconfig);
		controller.activate(null, myconfig);

		// Build and run test
		new ControllerTest(controller, componentManager, pvInverter0, meter1, ess1, ess2, io0).next(new TestCase()//
				// OffGrid-SwitchTOffGrid-BatteryOkay-PvOkay
				.input(ess1Soc, 50)//
				.input(ess2Soc, 50)//
				.input(io0Q1, false)//
				.input(io0Q2, false)//
				.input(io0Q3, true)//
				.input(io0Q4, true)//
				.input(pvInverterActivePower, 20000)//
				.input(meter1ActivePower, 5000)//
				.output(io0Q3, false)//
				.output(io0Q2, false)//
				.output(io0Q3, true)//
				.output(io0Q4, false)//
				.output(pvInverterActivePower, 20000))//
				.run();

	}

}