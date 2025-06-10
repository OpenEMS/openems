package io.openems.edge.controller.symmetric.limitactivepower;

import org.junit.Test;

import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;

public class ControllerEssLimitActivePowerImplTest {

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerEssLimitActivePowerImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setMaxChargePower(1000) //
						.setMaxDischargePower(1000) //
						.setValidatePowerConstraints(false) //
						.build()) //
				.deactivate();
	}

}
