package io.openems.edge.goodwe.batteryinverter;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.goodwe.GoodWeConstants;
import io.openems.edge.goodwe.charger.GoodWeChargerPv1;

public class GoodWeBatteryInverterImplTest {

	private static final String MODBUS_ID = "modbus0";

	private static final String BATTERY_INVERTER_ID = "batteryInverter0";

	private static final String CHARGER_ID = "charger0";

	@Test
	public void testEt() throws Exception {
		GoodWeChargerPv1 charger = new GoodWeChargerPv1();
		new ComponentTest(charger) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(io.openems.edge.goodwe.charger.MyConfig.create() //
						.setId(CHARGER_ID) //
						.setBatteryInverterId(BATTERY_INVERTER_ID) //
						.setModbusId(MODBUS_ID) //
						.setUnitId(GoodWeConstants.DEFAULT_UNIT_ID) //
						.build());

		GoodWeBatteryInverterImpl ess = new GoodWeBatteryInverterImpl();
		ess.addCharger(charger);
		new ComponentTest(ess) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.addComponent(charger) //
				.activate(MyConfig.create() //
						.setId(BATTERY_INVERTER_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(GoodWeConstants.DEFAULT_UNIT_ID) //
						.build()) //
		;
	}

}
