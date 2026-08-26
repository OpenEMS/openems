package io.openems.edge.controller.asymmetric.fixreactivepower;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedAsymmetricEss;

public class ControllerAsymmetricFixReactivePowerImplTest {

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerAsymmetricFixReactivePowerImpl()) //
				.addComponent(new DummyManagedAsymmetricEss("ess0")) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setPowerL1(0) //
						.setPowerL2(0) //
						.setPowerL3(0) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

}
