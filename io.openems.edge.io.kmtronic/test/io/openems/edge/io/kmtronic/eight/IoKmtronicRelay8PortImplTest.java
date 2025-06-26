package io.openems.edge.io.kmtronic.eight;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class IoKmtronicRelay8PortImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new IoKmtronicRelay8PortImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("io0") //
						.setModbusId("modbus0") //
						.build()) //
		;
	}
}
