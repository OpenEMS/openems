package io.openems.edge.controller.chp.soc;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.io.test.DummyInputOutput;

public class ChpSocTest {

	private static final String CTRL_ID = "ctrl0";

	private static final String ESS_ID = "ess0";
	private static final ChannelAddress ESS_SOC = new ChannelAddress(ESS_ID, "Soc");

	private static final String IO_ID = "io0";
	private static final ChannelAddress IO_OUTPUT0 = new ChannelAddress(IO_ID, "InputOutput0");

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerChpSocImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(new DummyManagedSymmetricEss(ESS_ID)) //
				.addComponent(new DummyInputOutput(IO_ID)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setInputChannelAddress(ESS_SOC.toString()) //
						.setOutputChannelAddress(IO_OUTPUT0.toString()) //
						.setLowThreshold(15) //
						.setHighThreshold(85) //
						.setMode(Mode.AUTOMATIC) //
						.setInvert(false) //
						.build())
				.next(new TestCase() //
						.input(ESS_SOC, 14) //
						.output(IO_OUTPUT0, true)) //
				.next(new TestCase() //
						.input(ESS_SOC, 50) //
						.output(IO_OUTPUT0, null)) //
				.next(new TestCase() //
						.input(ESS_SOC, 90) //
						.output(IO_OUTPUT0, false)) //
				.next(new TestCase() //
						.input(ESS_SOC, 50) //
						.output(IO_OUTPUT0, null)) //
				.next(new TestCase() //
						.input(ESS_SOC, 15) //
						.output(IO_OUTPUT0, true)) //
				.next(new TestCase() //
						.input(ESS_SOC, 85) //
						.output(IO_OUTPUT0, false)) //
				.next(new TestCase() //
						.input(ESS_SOC, 86) //
						.output(IO_OUTPUT0, false)) //
				.next(new TestCase() //
						.input(ESS_SOC, 14) //
						.output(IO_OUTPUT0, true)) //
				.next(new TestCase() //
						.input(ESS_SOC, 45) //
						.output(IO_OUTPUT0, null));
	}

}