package io.openems.edge.battery.soltaro.single.versiona;

import org.junit.Test;

import io.openems.edge.battery.soltaro.common.enums.BatteryState;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class SingleRackTest {

	private static final String BATTERY_ID = "battery0";
	private static final String MODBUS_ID = "modbus0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new BatterySoltaroSingleRackVersionAImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(BATTERY_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(0) //
						.setErrorLevel2Delay(0) //
						.setMaxStartTime(0) //
						.setPendingTolerance(0) //
						.setMaxStartAppempts(0) //
						.setStartUnsuccessfulDelay(0) //
						.setMinimalCellVoltage(0) //
						.setCapacity(0) //
						.setBatteryState(BatteryState.DEFAULT) //
						.build()) //
		;
	}

}
