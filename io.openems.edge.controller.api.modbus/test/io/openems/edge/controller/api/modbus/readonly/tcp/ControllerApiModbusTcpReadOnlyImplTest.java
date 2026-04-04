package io.openems.edge.controller.api.modbus.readonly.tcp;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.controller.api.modbus.CommonConfig.Tcp.DEFAULT_PORT;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyMeta;
import io.openems.edge.controller.api.modbus.LogVerbosity;
import io.openems.edge.controller.api.modbus.MyTcpConfig;
import io.openems.edge.controller.test.ControllerTest;

public class ControllerApiModbusTcpReadOnlyImplTest {

	@Test
	public void test() throws Exception {
		final var clock = createDummyClock();
		new ControllerTest(new ControllerApiModbusTcpReadOnlyImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("metaComponent", new DummyMeta()) //
				.activate(MyTcpConfig.create(io.openems.edge.controller.api.modbus.readonly.tcp.Config.class) //
						.setId("ctrl0") //
						.setEnabled(false) // do not actually start server
						.setComponentIds() //
						.setMaxConcurrentConnections(5) //
						.setPort(DEFAULT_PORT) //
						.setLogVerbosity(LogVerbosity.NONE) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}
}
