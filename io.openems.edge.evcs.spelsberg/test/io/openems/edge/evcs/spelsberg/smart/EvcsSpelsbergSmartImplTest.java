package io.openems.edge.evcs.spelsberg.smart;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class EvcsSpelsbergSmartImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new EvcsSpelsbergSmartImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setModbusId("modbus0") //
						.setId("evcs0") //
						.setModbusUnitId(255) //
						.setMaxHwCurrent(16000) //
						.setMinHwCurrent(6000) //
						.build()); //
	}
}
