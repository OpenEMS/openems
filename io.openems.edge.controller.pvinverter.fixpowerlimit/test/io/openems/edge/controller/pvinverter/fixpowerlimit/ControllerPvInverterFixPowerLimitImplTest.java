package io.openems.edge.controller.pvinverter.fixpowerlimit;

import org.junit.Test;

import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;

public class ControllerPvInverterFixPowerLimitImplTest {

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerPvInverterFixPowerLimitImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setPvInverterId("pvInverter0") //
						.setPowerLimit(10000) //
						.build()) //
				.deactivate();
	}

}
