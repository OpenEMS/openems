package io.openems.edge.controller.asymmetric.phaserectification;

import org.junit.Test;

import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedAsymmetricEss;
import io.openems.edge.meter.test.DummyElectricityMeter;

public class ControllerAsymmetricPhaseRectificationImplTest {

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerAsymmetricPhaseRectificationImpl()) //
				.addComponent(new DummyManagedAsymmetricEss("ess0")) //
				.addComponent(new DummyElectricityMeter("meter0")) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setMeterId("meter0") //
						.build()) //
				.deactivate();
	}

}
