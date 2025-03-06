package io.openems.edge.controller.api.modbus.readonly.rtu;

import org.junit.Test;

import io.openems.edge.bridge.modbus.api.Parity;
import io.openems.edge.bridge.modbus.api.Stopbit;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.test.ControllerTest;

public class ControllerApiModbusRtuReadOnlyImplTest {

	private static final String CTRL_ID = "ctrl0";

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerApiModbusRtuReadOnlyImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEnabled(false) // do not actually start server
						.setParity(Parity.NONE)
						.setStopbit(Stopbit.ONE)
						.setBaudrate(9600) //
						.setComponentIds() //
						.setMaxConcurrentConnections(5) //
						.setPortName("/dev/ttyUSB0") //
						.build()) //
				.next(new TestCase()) //
		;
	}
}
