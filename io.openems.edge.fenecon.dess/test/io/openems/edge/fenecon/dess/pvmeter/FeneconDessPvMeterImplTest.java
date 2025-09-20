package io.openems.edge.fenecon.dess.pvmeter;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class FeneconDessPvMeterImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new FeneconDessPvMeterImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("meter0") //
						.setModbusId("modbus0") //
						.build()) //
		;
	}
}