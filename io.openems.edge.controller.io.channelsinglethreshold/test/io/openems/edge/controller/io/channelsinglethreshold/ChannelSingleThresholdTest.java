package io.openems.edge.controller.io.channelsinglethreshold;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.io.test.DummyInputOutput;

public class ChannelSingleThresholdTest {

	private final static String CTRL_ID = "ctrl0";
	private final static ChannelAddress CTRL_AWAITING_HYSTERESIS = new ChannelAddress(CTRL_ID, "AwaitingHysteresis");

	private final static String ESS_ID = "ess0";
	private final static ChannelAddress ESS_SOC = new ChannelAddress(ESS_ID, "Soc");

	private final static String IO_ID = "io0";
	private final static ChannelAddress IO_INPUT_OUTPUT0 = new ChannelAddress(IO_ID, "InputOutput0");

	@Test
	public void test() throws Exception {
		final TimeLeapClock clock = new TimeLeapClock();
		new ControllerTest(new ChannelSingleThreshold()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addComponent(new DummyManagedSymmetricEss(ESS_ID)) //
				.addComponent(new DummyInputOutput(IO_ID)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setMode(Mode.AUTOMATIC) //
						.setInputChannelAddress(ESS_SOC.toString()) //
						.setOutputChannelAddress(IO_INPUT_OUTPUT0.toString()) //
						.setThreshold(70) //
						.setSwitchedLoadPower(0) //
						.setMinimumSwitchingTime(60).setInvert(false) //
						.build())
				.next(new TestCase() //
						.input(ESS_SOC, 50) //
						.output(IO_INPUT_OUTPUT0, false) //
						.output(CTRL_AWAITING_HYSTERESIS, false)); //
		// TODO this test requires a mocked clock for Channel.setNextValue()
//				.next(new TestCase() //
//						.timeleap(clock, 71, ChronoUnit.SECONDS) //
//						.input(input0, 71) //
//						.output(output0, true).output(ctrl0AwaitingHysteresis, false)) //
	}

}
