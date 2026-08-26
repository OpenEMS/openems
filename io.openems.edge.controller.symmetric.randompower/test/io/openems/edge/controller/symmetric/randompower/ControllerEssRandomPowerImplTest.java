package io.openems.edge.controller.symmetric.randompower;

import org.junit.Test;

import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;

public class ControllerEssRandomPowerImplTest {

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerEssRandomPowerImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setMinPower(0) //
						.setMaxPower(1000) //
						.build()) //
				.deactivate();
	}

}
