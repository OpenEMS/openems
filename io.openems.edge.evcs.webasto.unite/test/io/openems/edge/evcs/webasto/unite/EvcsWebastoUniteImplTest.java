package io.openems.edge.evcs.webasto.unite;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class EvcsWebastoUniteImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new EvcsWebastoUniteImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setModbusId("modbus0") //
						.setId("evcs0") //
						.setModbusUnitId(255) //
						.setMaxHwCurrent(32_000) //
						.setMinHwCurrent(6_000) //
						.build()) //
		;
	}
}
