package io.openems.edge.controller.asymmetric.phaserectification;

import org.junit.Test;

import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedAsymmetricEss;
import io.openems.edge.meter.test.DummyElectricityMeter;

public class ControllerAsymmetricPhaseRectificationImplTest {

	private static final String CTRL_ID = "ctrl0";
	private static final String METER_ID = "meter0";
	private static final String ESS_ID = "ess0";

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerAsymmetricPhaseRectificationImpl()) //
				.addComponent(new DummyManagedAsymmetricEss(ESS_ID)) //
				.addComponent(new DummyElectricityMeter(METER_ID)) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setMeterId(METER_ID) //
						.build()); //
		;
	}

}
