package io.openems.edge.controller.api.modbus.readonly;

import static io.openems.edge.controller.api.modbus.AbstractModbusTcpApi.DEFAULT_PORT;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.test.ControllerTest;

public class ControllerApiModbusTcpReadOnlyImplTest {

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerApiModbusTcpReadOnlyImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEnabled(false) // do not actually start server
						.setComponentIds() //
						.setMaxConcurrentConnections(5) //
						.setPort(DEFAULT_PORT) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}
}
