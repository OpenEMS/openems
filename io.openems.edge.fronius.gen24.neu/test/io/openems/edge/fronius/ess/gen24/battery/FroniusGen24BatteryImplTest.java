package io.openems.edge.fronius.ess.gen24.battery;

import org.junit.jupiter.api.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.fronius.ess.gen24.battery.FroniusGen24BatteryImpl;
import io.openems.edge.common.test.ComponentTest;

public class FroniusGen24BatteryImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new FroniusGen24BatteryImpl()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("batteryInverter0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(3) //
						.build())
				.next(new TestCase()) //
				.deactivate();
	}
}
