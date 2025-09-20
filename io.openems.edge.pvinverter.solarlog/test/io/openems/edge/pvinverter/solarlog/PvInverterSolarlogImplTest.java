package io.openems.edge.pvinverter.solarlog;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class PvInverterSolarlogImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new PvInverterSolarlogImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("meter0") //
						.setModbusId("modbus0") //
						.build()) //
		;
	}
}