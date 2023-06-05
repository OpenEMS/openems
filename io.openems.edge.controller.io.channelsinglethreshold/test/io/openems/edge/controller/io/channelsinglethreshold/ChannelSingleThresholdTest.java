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

	private static final String CTRL_ID = "ctrl0";
	private static final ChannelAddress CTRL_AWAITING_HYSTERESIS = new ChannelAddress(CTRL_ID, "AwaitingHysteresis");

	private static final String ESS_ID = "ess0";
	private static final ChannelAddress ESS_SOC = new ChannelAddress(ESS_ID, "Soc");

	private static final String IO_ID = "io0";
	private static final ChannelAddress IO_INPUT_OUTPUT0 = new ChannelAddress(IO_ID, "InputOutput0");

	@Test
	public void test() throws Exception {
		final var clock = new TimeLeapClock();
		new ControllerTest(new ControllerIoChannelSingleThresholdImpl()) //
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
	}

}
