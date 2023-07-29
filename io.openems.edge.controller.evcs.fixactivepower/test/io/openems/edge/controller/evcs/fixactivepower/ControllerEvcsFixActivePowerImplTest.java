package io.openems.edge.controller.evcs.fixactivepower;

import org.junit.Test;

import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;

public class ControllerEvcsFixActivePowerImplTest {

	private static final String CTRL_ID = "ctrl0";
	private static final String EVCS_ID = "evcs0";

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerEvcsFixActivePowerImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEvcsId(EVCS_ID) //
						.setPower(0) //
						.build()); //
		;
	}
}
