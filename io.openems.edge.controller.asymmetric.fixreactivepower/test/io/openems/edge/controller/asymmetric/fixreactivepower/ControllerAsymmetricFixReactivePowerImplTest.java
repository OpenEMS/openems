package io.openems.edge.controller.asymmetric.fixreactivepower;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedAsymmetricEss;

public class ControllerAsymmetricFixReactivePowerImplTest {

	private static final String CTRL_ID = "ctrl0";
	private static final String ESS_ID = "ess0";

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerAsymmetricFixReactivePowerImpl()) //
				.addComponent(new DummyManagedAsymmetricEss(ESS_ID)) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setPowerL1(0) //
						.setPowerL2(0) //
						.setPowerL3(0) //
						.build()) //
				.next(new TestCase()); //
		;
	}

}
