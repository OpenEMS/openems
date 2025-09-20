package io.openems.edge.pvinverter.kaco.blueplanet;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class PvInverterKacoBlueplanetImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new PvInverterKacoBlueplanetImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("pvInverter0") //
						.setReadOnly(true) //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.build()) //
		;
	}
}