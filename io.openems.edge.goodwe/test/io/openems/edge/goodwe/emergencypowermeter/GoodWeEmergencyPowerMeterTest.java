package io.openems.edge.goodwe.emergencypowermeter;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class GoodWeEmergencyPowerMeterTest {

	private static final String MODBUS_ID = "modbus0";

	private static final String METER_ID = "meter2";

	@Test
	public void test() throws Exception {
		new ComponentTest(new GoodWeEmergencyPowerMeterImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(METER_ID) //
						.setModbusId(MODBUS_ID) //
						.build());
	}
}
