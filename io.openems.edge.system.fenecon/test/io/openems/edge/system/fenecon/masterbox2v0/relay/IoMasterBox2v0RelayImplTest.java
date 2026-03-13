package io.openems.edge.system.fenecon.masterbox2v0.relay;

import io.openems.edge.system.fenecon.masterbox2v0.relay.IoMasterBox2v0RelayImpl;
import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;

public class IoMasterBox2v0RelayImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new IoMasterBox2v0RelayImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("io0") //
						.setModbusId("modbus0") //
						.build()) //
		;
	}
}
