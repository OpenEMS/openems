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

public class HeatingElementTestSecond {

	@SuppressWarnings("all")
	private static class MyConfig extends AbstractComponentConfig implements Config {

		private final String inputChannelAddress;
		private final String outputChannelAddress1;
		private final String outputChannelAddress2;
		private final String outputChannelAddress3;
		private final int powerOfPhase;
		private final Mode mode;
		private final Priority priority;
		private final int minTime;
		private final int minKwh;
		private final String endTime;

		public MyConfig(String id, String inputChannelAddress, String outputChannelAddress1,
				String outputChannelAddress2, String outputChannelAddress3, String endtime, int powerOfPhase, Mode mode,
				Priority priority, int minTime, int minKwh) {
			super(Config.class, id);
			this.inputChannelAddress = inputChannelAddress;
			this.outputChannelAddress1 = outputChannelAddress1;
			this.outputChannelAddress2 = outputChannelAddress2;
			this.outputChannelAddress3 = outputChannelAddress3;
			this.powerOfPhase = powerOfPhase;
			this.mode = mode;
			this.priority = priority;
			this.minTime = minTime;
			this.minKwh = minKwh;
			this.endTime = endtime;
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
		public String outputChannelAddress2() {
			return this.outputChannelAddress2;
		}

		@Override
		public String outputChannelAddress3() {
			return this.outputChannelAddress3;
		}

		@Override
		public Mode mode() {
			return this.mode;
		}

		@Override
		public Priority priority() {
			return this.priority;
		}

		@Override
		public double minTime() {
			return minTime;
		}

		@Override
		public int minkwh() {
			return minKwh;
		}

		@Override
		public String endTime() {
			return endTime;
		}
	}

	@Test
	public void test() throws OpenemsNamedException {
		// initialize the controller
		TimeLeapClock clock = new TimeLeapClock(ZoneOffset.UTC);
		ControllerHeatingElement controller = new ControllerHeatingElement(clock);
		// Add referenced services
		DummyComponentManager componentManager = new DummyComponentManager();
		controller.componentManager = componentManager;

		ChannelAddress ess0 = new ChannelAddress("ess0", "ActivePower");
		ChannelAddress output1 = new ChannelAddress("io0", "InputOutput1");
		ChannelAddress output2 = new ChannelAddress("io0", "InputOutput2");
		ChannelAddress output3 = new ChannelAddress("io0", "InputOutput3");

		MyConfig myconfig = new MyConfig("ctrl1", ess0.toString(), output1.toString(), output2.toString(),
				output3.toString(), "15:45:00", 2000, Mode.AUTOMATIC, Priority.TIME, 1, 4000);
		controller.activate(null, myconfig);
		controller.activate(null, myconfig);

		ManagedSymmetricEss ess = new DummyManagedSymmetricEss("ess0");
		DummyInputOutput io = new DummyInputOutput("io0");

		// Build and run test
		try {
			new ControllerTest(controller, componentManager, ess, io)//
.next(new TestCase() //
							.input(ess0, -2000) //
							.output(output1, false) //
) // Grid active power : -2000, Excess power : 2000, from -> UNDEFINED --to--> FIRST_PHASE, no of relais = 1
					.next(new TestCase() //
							.timeleap(clock, 15, ChronoUnit.MINUTES)//
							.input(ess0, -4000) //
							.output(output1, true) //
) // Grid active power : -4000, Excess power : 6000, from ->FIRST_PHASE --to--> THIRD_PHASE, no of relais = 3
					.next(new TestCase() //
							.timeleap(clock, 15, ChronoUnit.MINUTES)//
							.input(ess0, -6000) //
							.output(output1, true) //
) // Grid active power : -6000, Excess power : 12000, from -> THIRD_PHASE --to--> THIRD_PHASE, no of relais = 3
					.next(new TestCase() //
							.input(ess0, -7000) //
							.output(output1, true) //
) // Grid active power : -7000, Excess power : 13000, from -> THIRD_PHASE --to--> THIRD_PHASE, no of relais = 3
					.next(new TestCase() //
							.timeleap(clock, 15, ChronoUnit.MINUTES)//
							.input(ess0, 0) //
							.output(output1, true) //
) // Grid active power : 0, Excess power : 6000, from -> THIRD_PHASE --to--> THIRD_PHASE, no of relais = 3
					.next(new TestCase() //
							.timeleap(clock, 15, ChronoUnit.MINUTES)//
							.input(ess0, 1) //
							.output(output1, true) //
) // Grid active power : 1, Excess power : 0, from -> THIRD_PHASE --to--> UNDEFINED, no of relais = 0
					.next(new TestCase() //
							.timeleap(clock, 6, ChronoUnit.MINUTES)//
							.input(ess0, 20000) //
							.output(output1, true) //
) // Grid active power : 20000, Excess power : 0, from -> UNDEFINED--to--> UNDEFINED, no of relais = 0
					.next(new TestCase() //
							.timeleap(clock, 6, ChronoUnit.MINUTES)//
							.input(ess0, -4000) //
							.output(output1, true) //
) // Grid active power : -4000, Excess power : 10000, from ->FIRST_PHASE --to--> THIRD_PHASE, no of relais = 3

					.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// .timeleap(clock, 6, ChronoUnit.MINUTES)
}
