package io.openems.edge.project.controller.enbag.emergencymode;

import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.test.AbstractComponentConfig;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.TimeLeapClock;
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
			return this.pvInverter_id;
		}

		@Override
		public String q1ChannelAddress() {
			return this.q1ChannelAddress;
		}

		@Override
		public String q2ChannelAddress() {
			return this.q2ChannelAddress;
		}

		@Override
		public String q3ChannelAddress() {
			return this.q3ChannelAddress;
		}

		@Override
		public String q4ChannelAddress() {
			return this.q4ChannelAddress;
		}

		@Override
		public String pvMeter_id() {
			return this.pvMeter_id;
		}

		@Override
		public String ess2_id() {
			return this.ess2_id;
		}

		@Override
		public String ess1_id() {
			return this.ess1_id;
		}

		@Override
		public int pvSufficientPower() {
			return 20_000;
		}

	}

	@Test
	public void test() throws Exception {
		// Initialize mocked Clock
		final TimeLeapClock clock = new TimeLeapClock();

		// initialize the controller
		EmergencyClusterMode controller = new EmergencyClusterMode(clock);
		// Add referenced services
		DummyComponentManager componentManager = new DummyComponentManager();
		controller.componentManager = componentManager;

		ManagedSymmetricEss ess1 = new DummyManagedSymmetricEss("ess1");
		ChannelAddress ess1Soc = new ChannelAddress("ess1", "Soc");
		ChannelAddress ess1GridMode = new ChannelAddress("ess1", "GridMode");

		ManagedSymmetricEss ess2 = new DummyManagedSymmetricEss("ess2");
		ChannelAddress ess2Soc = new ChannelAddress("ess2", "Soc");
		ChannelAddress ess2GridMode = new ChannelAddress("ess2", "GridMode");

		SymmetricMeter meter1 = new DummySymmetricMeter("meter1");
		ChannelAddress meter1ActivePower = new ChannelAddress("meter1", "ActivePower");

		SymmetricPvInverter pvInverter0 = new DummySymmetricPvInverter("pvInverter0");
		ChannelAddress pvInverterActivePower = new ChannelAddress("pvInverter0", "ActivePower");

		DummyInputOutput io0 = new DummyInputOutput("io0");
		ChannelAddress io0Q1 = new ChannelAddress("io0", "InputOutput1");
		ChannelAddress io0Q2 = new ChannelAddress("io0", "InputOutput2");
		ChannelAddress io0Q3 = new ChannelAddress("io0", "InputOutput3");
		ChannelAddress io0Q4 = new ChannelAddress("io0", "InputOutput4");

		MyConfig myconfig = new MyConfig("ctrl1", ess1.id(), ess2.id(), io0Q1.toString(), io0Q2.toString(),
				io0Q3.toString(), io0Q4.toString(), pvInverter0.id(), meter1.id());
		controller.activate(null, myconfig);

		// Build and run test
		new ControllerTest(controller, componentManager, pvInverter0, meter1, ess1, ess2, io0) //
				.next(new TestCase()//
						// OffGrid-SwitchTOffGrid-BatteryOkay-PvOkay
						.input(ess1GridMode, GridMode.OFF_GRID) //
						.input(ess2GridMode, GridMode.OFF_GRID) //
						.input(ess1Soc, 50)//
						.input(ess2Soc, 50)//
//						.input(io0Q1, false)//
//						.input(io0Q2, false)//
//						.input(io0Q3, true)//
//						.input(io0Q4, true)//
						.input(pvInverterActivePower, 20000)//
						.input(meter1ActivePower, 5000)//
						.output(io0Q1, false)//
						.output(io0Q2, false)//
						.output(io0Q3, null)//
						.output(io0Q4, false)//
						.output(pvInverterActivePower, 20000))//
//				.next(new TestCase()//
//						.input(io0Q4, false))//
//				.next(new TestCase()//
//						.timeleap(clock, 11, ChronoUnit.SECONDS) //
//						.output(io0Q3, true)) //
				.run();

	}

}