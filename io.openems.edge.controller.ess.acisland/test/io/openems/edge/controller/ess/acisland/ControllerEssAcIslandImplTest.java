package io.openems.edge.controller.ess.acisland;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;

public class ControllerEssAcIslandImplTest {

	private static final String CTRL_ID = "ctrl0";

	private static final String ESS_ID = "ess0";

	private static final String IO_ID = "io0";
	private static final ChannelAddress IO_OUTPUT0 = new ChannelAddress(IO_ID, "Output0");
	private static final ChannelAddress IO_OUTPUT1 = new ChannelAddress(IO_ID, "Output1");

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerEssAcIslandImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setInvertOffGridOutput(false) //
						.setInvertOnGridOutput(false) //
						.setMaxSoc(90) //
						.setMinSoc(4) //
						.setOffGridOutputChannelAddress(IO_OUTPUT0.toString()) //
						.setOnGridOutputChannelAddress(IO_OUTPUT1.toString()) //
						.build()) //
		;
	}

}
