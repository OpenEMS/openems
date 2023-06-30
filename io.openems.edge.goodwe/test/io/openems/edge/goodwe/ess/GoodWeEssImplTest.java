package io.openems.edge.goodwe.ess;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.ess.test.ManagedSymmetricEssTest;
import io.openems.edge.goodwe.GoodWeConstants;
import io.openems.edge.goodwe.charger.GoodWeChargerPv1;
import io.openems.edge.goodwe.common.enums.ControlMode;

public class GoodWeEssImplTest {

	private static final String ESS_ID = "ess0";
	private static final String MODBUS_ID = "modbus0";
	private static final String CHARGER_ID = "charger0";

	@Test
	public void testEt() throws Exception {
		var charger = new GoodWeChargerPv1();
		new ComponentTest(charger) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(io.openems.edge.goodwe.charger.MyConfig.create() //
						.setId(CHARGER_ID) //
						.setBatteryInverterId(ESS_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(GoodWeConstants.DEFAULT_UNIT_ID) //
						.build());

		var ess = new GoodWeEssImpl();
		ess.addCharger(charger);
		new ManagedSymmetricEssTest(ess) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.addComponent(charger) //
				.activate(MyConfig.create() //
						.setId(ESS_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(GoodWeConstants.DEFAULT_UNIT_ID) //
						.setCapacity(9_000) //
						.setMaxBatteryPower(5_200) //
						.setControlMode(ControlMode.SMART) //
						.build()) //
		;
	}

	@Test
	public void testBt() throws Exception {
		var ess = new GoodWeEssImpl();
		new ManagedSymmetricEssTest(ess) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(ESS_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(GoodWeConstants.DEFAULT_UNIT_ID) //
						.setCapacity(9_000) //
						.setMaxBatteryPower(5_200) //
						.setControlMode(ControlMode.SMART) //
						.build()) //
		;
	}

}
