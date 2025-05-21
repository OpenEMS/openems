package io.openems.edge.controller.evcs.fixactivepower;

import org.junit.Test;

import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;

public class ControllerEvcsFixActivePowerImplTest {

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerEvcsFixActivePowerImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEvcsId("evcs0") //
						.setPower(0) //
						.setUpdateFrequency(1) //
						.build()) //
				.deactivate();
	}
}
