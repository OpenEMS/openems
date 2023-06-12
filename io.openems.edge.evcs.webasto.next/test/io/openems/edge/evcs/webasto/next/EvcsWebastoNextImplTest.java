package io.openems.edge.evcs.webasto.next;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class EvcsWebastoNextImplTest {

	private static final String EVCS_ID = "evcs0";
	private static final String MODBUS_ID = "modbus0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new EvcsWebastoNextImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setModbusId(MODBUS_ID) //
						.setId(EVCS_ID) //
						.setModbusUnitId(1) //
						.setMaxHwCurrent(32000) //
						.setMinHwCurrent(6000) //
						.build()); //
	}

}