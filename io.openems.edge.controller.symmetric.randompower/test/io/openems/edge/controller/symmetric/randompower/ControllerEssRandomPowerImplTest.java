package io.openems.edge.controller.symmetric.randompower;

import org.junit.Test;

import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;

public class ControllerEssRandomPowerImplTest {

	private static final String CTRL_ID = "ctrl0";
	private static final String ESS_ID = "ess0";

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerEssRandomPowerImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setMinPower(0) //
						.setMaxPower(1000) //
						.build()); //
	}

}
