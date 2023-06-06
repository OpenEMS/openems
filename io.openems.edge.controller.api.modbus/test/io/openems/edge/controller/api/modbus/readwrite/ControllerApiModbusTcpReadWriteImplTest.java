package io.openems.edge.controller.api.modbus.readwrite;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.api.modbus.AbstractModbusTcpApi;
import io.openems.edge.controller.test.ControllerTest;

public class ControllerApiModbusTcpReadWriteImplTest {

	private static final String CTRL_ID = "ctrl0";

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerApiModbusTcpReadWriteImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEnabled(false) // do not actually start server
						.setComponentIds() //
						.setMaxConcurrentConnections(5) //
						.setPort(AbstractModbusTcpApi.DEFAULT_PORT) //
						.setApiTimeout(60) //
						.build()) //
				.next(new TestCase()) //
		;
	}
}
