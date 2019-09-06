package io.openems.edge.controller.HeatingElementController;

import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentConfig;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.io.test.DummyInputOutput;

public class HeatingElementTest {

	@SuppressWarnings("all")
	private static class MyConfig extends AbstractComponentConfig implements Config {

		private final String inputChannelAddress;
		private final String inputChannelAddress1;
		private final String outputChannelAddress1;
		private final String outputChannelAddress2;
		private final String outputChannelAddress3;
		private final int powerOfPhase;

		public MyConfig(String id, String inputChannelAddress, String inputChannelAddress1,
				String outputChannelAddress1, String outputChannelAddress2, String outputChannelAddress3,
				int powerOfPhase) {
			super(Config.class, id);
			this.inputChannelAddress = inputChannelAddress;
			this.inputChannelAddress1 = inputChannelAddress1;
			this.outputChannelAddress1 = outputChannelAddress1;
			this.outputChannelAddress2 = outputChannelAddress2;
			this.outputChannelAddress3 = outputChannelAddress3;
			this.powerOfPhase = powerOfPhase;
		}

		@Override
		public String inputChannelAddress() {
			return this.inputChannelAddress;
		}

		@Override
		public int powerOfPhase() {
			return this.powerOfPhase;
		}

		@Override
		public String outputChannelAddress1() {
			return this.outputChannelAddress1;
		}

		@Override
		public String outputChannelAddres2() {
			return this.outputChannelAddress2;
		}

		@Override
		public String outputChannelAddres3() {
			return this.outputChannelAddress3;
		}

		@Override
		public String inputChannelAddress1() {
			return this.inputChannelAddress1;
		}
	}

	@Test
	public void test() throws OpenemsNamedException {
		// initialize the controller
		TimeLeapClock clock = new TimeLeapClock(ZoneOffset.UTC);
		System.out.println(clock.instant());
		ControllerHeatingElement controller = new ControllerHeatingElement(clock);
		// Add referenced services
		DummyComponentManager componentManager = new DummyComponentManager();
		controller.componentManager = componentManager;

		ChannelAddress ess0 = new ChannelAddress("ess0", "ActivePower");
		ChannelAddress ess1 = new ChannelAddress("ess1", "Soc");
		ChannelAddress output1 = new ChannelAddress("io0", "InputOutput1");
		ChannelAddress output2 = new ChannelAddress("io0", "InputOutput2");
		ChannelAddress output3 = new ChannelAddress("io0", "InputOutput3");

		MyConfig myconfig = new MyConfig("ctrl1", ess0.toString(), ess1.toString(), output1.toString(),
				output2.toString(), output3.toString(), 2000);
		controller.activate(null, myconfig);
		controller.activate(null, myconfig);

		ManagedSymmetricEss ess = new DummyManagedSymmetricEss("ess0");
		ManagedSymmetricEss ess11 = new DummyManagedSymmetricEss("ess1");
		DummyInputOutput io = new DummyInputOutput("io0");

		// Build and run test
		try {
			new ControllerTest(controller, componentManager, ess, ess11, io)//
					.next(new TestCase() //
							.input(ess0, 0) //
							.input(ess1, 60) // soc
							.output(output1, false) //
							.output(output2, false) //
							.output(output3, false)) //
					.next(new TestCase() //
							.input(ess0, 0) //
							.input(ess1, 92) // soc
							.output(output1, false) //
							.output(output2, false) //
							.output(output3, false)) //
					.next(new TestCase() //
							.input(ess0, -2000) //
							.input(ess1, 93) // soc
							.output(output1, true) //
							.output(output2, false) //
							.output(output3, false)) //
					.next(new TestCase() //
							.timeleap(clock, 6, ChronoUnit.MINUTES)//
							.input(ess0, -4000) //
							.input(ess1, 95) // soc
							.output(output1, true) //
							.output(output2, true) //
							.output(output3, false)) //
					.next(new TestCase() //
							.timeleap(clock, 6, ChronoUnit.MINUTES)//
							.input(ess0, -6000) //
							.input(ess1, 97) // soc
							.output(output1, true) //
							.output(output2, true) //
							.output(output3, true)) //
					.next(new TestCase() //
							.input(ess0, -7000) //
							.input(ess1, 99) // soc
							.output(output1, true) //
							.output(output2, true) //
							.output(output3, true)) //
					.next(new TestCase() //
							.timeleap(clock, 6, ChronoUnit.MINUTES)//
							.input(ess0, 0) //
							.input(ess1, 60) // soc
							.output(output1, false) //
							.output(output2, false) //
							.output(output3, false)) //
					.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// .timeleap(clock, 6, ChronoUnit.MINUTES)
}
