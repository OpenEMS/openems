package io.openems.edge.battery.soltaro.single.versiona;

import org.junit.Test;

import io.openems.edge.battery.soltaro.common.enums.BatteryState;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class BatterySoltaroSingleRackVersionAImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new BatterySoltaroSingleRackVersionAImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("battery0") //
						.setModbusId("modbus0") //
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
