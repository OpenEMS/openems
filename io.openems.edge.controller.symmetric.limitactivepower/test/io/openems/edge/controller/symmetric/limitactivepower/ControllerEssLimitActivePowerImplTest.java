package io.openems.edge.controller.symmetric.limitactivepower;

import org.junit.Test;

import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;

public class ControllerEssLimitActivePowerImplTest {

	private static final String CTRL_ID = "ctrl0";
	private static final String ESS_ID = "ess0";

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerEssLimitActivePowerImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setMaxChargePower(1000) //
						.setMaxDischargePower(1000) //
						.setValidatePowerConstraints(false) //
						.build()); //
		;
	}

}
