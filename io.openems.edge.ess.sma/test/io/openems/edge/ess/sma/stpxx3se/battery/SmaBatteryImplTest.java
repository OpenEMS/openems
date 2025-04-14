package io.openems.edge.ess.sma.stpxx3se.battery;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class SmaBatteryImplTest {

	private static final String BATTERY_INVERTER_ID = "batteryInverter0";
	private static final String MODBUS_ID = "modbus0";
	private static final int MODBUS_UNIT_ID = 3;

	@Test
	public void test() throws Exception {
		new ComponentTest(new SmaBatteryImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(BATTERY_INVERTER_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(MODBUS_UNIT_ID) //
						.build());
	}

}
