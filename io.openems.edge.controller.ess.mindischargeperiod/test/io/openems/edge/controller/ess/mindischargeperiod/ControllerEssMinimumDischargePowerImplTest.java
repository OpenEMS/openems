package io.openems.edge.controller.ess.mindischargeperiod;

import org.junit.Test;

import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;

public class ControllerEssMinimumDischargePowerImplTest {

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerEssMinimumDischargePowerImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setActivateDischargePower(0) //
						.setDischargeTime(0) //
						.setMinDischargePower(0) //
						.build()) //
				.deactivate();
	}

}
