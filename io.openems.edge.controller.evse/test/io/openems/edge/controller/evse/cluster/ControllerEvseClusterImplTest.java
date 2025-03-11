package io.openems.edge.controller.evse.cluster;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.test.ControllerTest;

public class ControllerEvseClusterImplTest {

	@Test
	public void test() throws OpenemsException, Exception {
		new ControllerTest(new ControllerEvseClusterImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setDebugMode(true) //
						.setCtrlIds("ctrlEvseSingle0") //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

}
