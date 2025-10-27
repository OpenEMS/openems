package io.openems.edge.controller.ess.acisland;

import org.junit.Test;

import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;

public class ControllerEssAcIslandImplTest {

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerEssAcIslandImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setInvertOffGridOutput(false) //
						.setInvertOnGridOutput(false) //
						.setMaxSoc(90) //
						.setMinSoc(4) //
						.setOffGridOutputChannelAddress("io0/Output0") //
						.setOnGridOutputChannelAddress("io0/Output1") //
						.build()) //
				.deactivate();
	}

}
