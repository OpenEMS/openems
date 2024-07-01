package io.openems.edge.controller.pvinverter.fixpowerlimit;

import org.junit.Test;

import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;

public class ControllerPvInverterFixPowerLimitImplTest {

	private static final String CTRL_ID = "ctrl0";
	private static final String PV_INVERTER_ID = "pvInverter0";

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerPvInverterFixPowerLimitImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setPvInverterId(PV_INVERTER_ID) //
						.setPowerLimit(10000) //
						.build()); //
		;
	}

}
