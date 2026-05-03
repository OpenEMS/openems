package io.openems.edge.controller.api.modbus.readwrite.rtu;

import org.junit.jupiter.api.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.test.TestUtils;
import io.openems.edge.bridge.modbus.api.Parity;
import io.openems.edge.bridge.modbus.api.Stopbit;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyMeta;
import io.openems.edge.controller.api.modbus.LogVerbosity;
import io.openems.edge.controller.api.modbus.MyRtuConfig;
import io.openems.edge.controller.test.ControllerTest;

public class ControllerApiModbusRtuReadWriteImplTest {
	@Test
	public void test() throws Exception {
		final var clock = TestUtils.createDummyClock();
		new ControllerTest(new ControllerApiModbusRtuReadWriteImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("metaComponent", new DummyMeta()) //
				.activate(MyRtuConfig.create(io.openems.edge.controller.api.modbus.readonly.rtu.Config.class) //
						.setId("ctrlApiModbusTcp0") //
						.setEnabled(false) // do not actually start server
						.setParity(Parity.NONE).setStopbit(Stopbit.ONE) //
						.setBaudrate(9600) //
						.setComponentIds() //
						.setMaxConcurrentConnections(5) //
						.setPortName("/dev/ttyUSB0") //
						.setLogVerbosity(LogVerbosity.NONE) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
		;
	}
}
