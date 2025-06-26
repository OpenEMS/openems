package io.openems.edge.controller.symmetric.fixreactivepower;

import org.junit.Test;

import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;

public class ControllerEssFixReactivePowerImplTest {

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerEssFixReactivePowerImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setPower(1000) //
						.build()) //
				.deactivate();
	}

}
