package io.openems.edge.controller.asymmetric.balancingcosphi;

import org.junit.Test;

import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedAsymmetricEss;
import io.openems.edge.meter.test.DummyElectricityMeter;

public class ControllerAsymmetricBalancingCosPhiImplTest {

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerAsymmetricBalancingCosPhiImpl()) //
				.addComponent(new DummyManagedAsymmetricEss("ess0")) //
				.addComponent(new DummyElectricityMeter("meter0")) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setMeterId("meter0") //
						.setCosPhi(0.9) //
						.setDirection(CosPhiDirection.CAPACITIVE) //
						.build()) //
				.deactivate();
	}

}
