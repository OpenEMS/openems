package io.openems.edge.goodwe.ess;

import static io.openems.edge.goodwe.GoodWeConstants.DEFAULT_UNIT_ID;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.ess.test.ManagedSymmetricEssTest;
import io.openems.edge.goodwe.charger.singlestring.GoodWeChargerPv1;
import io.openems.edge.goodwe.common.enums.ControlMode;

public class GoodWeEssImplTest {

	@Test
	public void testEt() throws Exception {
		var charger = new GoodWeChargerPv1();
		new ComponentTest(charger) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(io.openems.edge.goodwe.charger.singlestring.MyConfig.create() //
						.setId("charger0") //
						.setBatteryInverterId("ess0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(DEFAULT_UNIT_ID) //
						.build());

		var ess = new GoodWeEssImpl();
		ess.addCharger(charger);
		new ManagedSymmetricEssTest(ess) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addComponent(charger) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(DEFAULT_UNIT_ID) //
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
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(DEFAULT_UNIT_ID) //
						.setCapacity(9_000) //
						.setMaxBatteryPower(5_200) //
						.setControlMode(ControlMode.SMART) //
						.build()) //
		;
	}

}
