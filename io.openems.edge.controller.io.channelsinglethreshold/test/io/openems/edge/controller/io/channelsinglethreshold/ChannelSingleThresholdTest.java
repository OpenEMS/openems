package io.openems.edge.controller.io.channelsinglethreshold;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentConfig;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.io.test.DummyInputOutput;

public class ChannelSingleThresholdTest {

	@SuppressWarnings("all")
	private static class MyConfig extends AbstractComponentConfig implements Config {

		private final Mode mode;
		private final String inputChannelAddress;
		private final String outputChannelAddress;
		private final int threshold;
		private final int switchedLoadPower;
		private final int minimumSwitchingTime;
		private final boolean invert;

		public MyConfig(String id, Mode mode, String inputChannelAddress, String outputChannelAddress, int threshold,
				int switchedLoadPower, int minimumSwitchingTime, boolean invert) {
			super(Config.class, id);
			this.mode = mode;
			this.inputChannelAddress = inputChannelAddress;
			this.outputChannelAddress = outputChannelAddress;
			this.threshold = threshold;
			this.switchedLoadPower = switchedLoadPower;
			this.minimumSwitchingTime = minimumSwitchingTime;
			this.invert = invert;
		}

		@Override
		public Mode mode() {
			return this.mode;
		}

		@Override
		public String inputChannelAddress() {
			return this.inputChannelAddress;
		}

		@Override
		public String outputChannelAddress() {
			return this.outputChannelAddress;
		}

		@Override
		public int threshold() {
			return this.threshold;
		}

		@Override
		public int switchedLoadPower() {
			return this.switchedLoadPower;
		}

		@Override
		public int minimumSwitchingTime() {
			return this.minimumSwitchingTime;
		}

		@Override
		public boolean invert() {
			return this.invert;
		}

	}

	@Test
	public void test() throws Exception {
		// Initialize mocked Clock
		final TimeLeapClock clock = new TimeLeapClock();

		// Initialize Controller
		ChannelSingleThreshold controller = new ChannelSingleThreshold(clock);

		// Add referenced services
		DummyComponentManager componentManager = new DummyComponentManager();
		controller.componentManager = componentManager;

		// Prepare Channels
		ChannelAddress input0 = new ChannelAddress("ess0", "Soc");
		ChannelAddress output0 = new ChannelAddress("io0", "InputOutput0");
		ChannelAddress ctrl0AwaitingHysteresis = new ChannelAddress("ctrl0", "AwaitingHysteresis");

		// Activate (twice, so that reference target is set)
		MyConfig config = new MyConfig("ctrl0", Mode.AUTOMATIC, input0.toString(), output0.toString(), 70, 0, 60,
				false);
		controller.activate(null, config);
		controller.activate(null, config);

		ManagedSymmetricEss essComponent = new DummyManagedSymmetricEss("ess0");
		DummyInputOutput ioComponent = new DummyInputOutput("io0");

		// Build and run test
		new ControllerTest(controller, componentManager, controller, essComponent, ioComponent) //
				.next(new TestCase() //
						.input(input0, 50) //
						.output(output0, false).output(ctrl0AwaitingHysteresis, false)) //
				// TODO this test requires a mocked clock for Channel.setNextValue()
//				.next(new TestCase() //
//						.timeleap(clock, 71, ChronoUnit.SECONDS) //
//						.input(input0, 71) //
//						.output(output0, true).output(ctrl0AwaitingHysteresis, false)) //
				.run();
	}

}
