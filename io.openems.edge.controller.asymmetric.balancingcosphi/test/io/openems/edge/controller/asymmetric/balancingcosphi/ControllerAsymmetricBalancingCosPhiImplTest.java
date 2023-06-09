package io.openems.edge.controller.asymmetric.balancingcosphi;

import org.junit.Test;

import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedAsymmetricEss;
import io.openems.edge.meter.test.DummyAsymmetricMeter;

public class ControllerAsymmetricBalancingCosPhiImplTest {

	private static final String CTRL_ID = "ctrl0";
	private static final String ESS_ID = "ess0";
	private static final String METER_ID = "meter0";

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerAsymmetricBalancingCosPhiImpl()) //
				.addComponent(new DummyManagedAsymmetricEss(ESS_ID)) //
				.addComponent(new DummyAsymmetricMeter(METER_ID)) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setMeterId(METER_ID) //
						.setCosPhi(0.9) //
						.setDirection(CosPhiDirection.CAPACITIVE) //
						.build()); //
		;
	}

}
