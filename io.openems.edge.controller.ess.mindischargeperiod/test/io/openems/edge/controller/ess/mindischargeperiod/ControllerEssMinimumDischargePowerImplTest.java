package io.openems.edge.controller.ess.mindischargeperiod;

import org.junit.Test;

import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;

public class ControllerEssMinimumDischargePowerImplTest {

	private static final String CTRL_ID = "ctrl0";
	private static final String ESS_ID = "ess0";

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerEssMinimumDischargePowerImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setActivateDischargePower(0) //
						.setDischargeTime(0) //
						.setMinDischargePower(0) //
						.build()); //
		;
	}

}
