package io.openems.edge.controller.debug.detailedlog;

import org.junit.Test;

import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;

public class ControllerDebugDetailedLogImplTest {

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerDebugDetailedLogImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setComponentIds() //
						.build()) //
				.deactivate();
	}

}
