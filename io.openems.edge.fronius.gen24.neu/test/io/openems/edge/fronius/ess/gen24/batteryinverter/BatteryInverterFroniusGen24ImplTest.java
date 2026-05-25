package io.openems.edge.fronius.ess.gen24.batteryinverter;

import org.junit.jupiter.api.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.fronius.ess.gen24.batteryinverter.BatteryInverterFroniusGen24Impl;
import io.openems.edge.fronius.ess.gen24.batteryinverter.ControlMode;
import io.openems.edge.common.test.ComponentTest;

public class BatteryInverterFroniusGen24ImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new BatteryInverterFroniusGen24Impl()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("batteryInverter0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setControlMode(ControlMode.REMOTE) //
						.build())
				.next(new TestCase()) //
				.deactivate();
	}
}
