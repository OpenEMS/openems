package io.openems.edge.ess.sinexcel;

import org.junit.Test;

import io.openems.edge.battery.test.DummyBattery;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.ess.test.ManagedSymmetricEssTest;

public class EssSinexcelImplTest {

	private static final String ESS_ID = "ess0";
	private static final String MODBUS_ID = "modbus0";
	private static final String BATTERY_ID = "battery0";

	@Test
	public void test() throws Exception {
		new ManagedSymmetricEssTest(new EssSinexcelImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("power", new DummyPower()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.addReference("setBattery", new DummyBattery(BATTERY_ID)) //
				.activate(MyConfig.create() //
						.setId(ESS_ID) //
						.setModbusId(MODBUS_ID) //
						.setBatteryId(BATTERY_ID) //
						.setInverterState(InverterState.ON) //
						.setToppingCharge(0) //
						.build()) //
		;
	}

}
