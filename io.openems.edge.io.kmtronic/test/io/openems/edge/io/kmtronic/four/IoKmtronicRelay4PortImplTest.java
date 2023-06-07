package io.openems.edge.io.kmtronic.four;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class IoKmtronicRelay4PortImplTest {

	private static final String IO_ID = "io0";
	private static final String MODBUS_ID = "modbus0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new IoKmtronicRelay4PortImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(IO_ID) //
						.setModbusId(MODBUS_ID) //
						.build()) //
		;
	}
}
