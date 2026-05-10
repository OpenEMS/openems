package io.openems.edge.sma.ess.stpxx3se.batteryinverter;

import org.junit.jupiter.api.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class BatteryInverterSmaStpSeImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new BatteryInverterSmaStpSeImpl()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("batteryInverter0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(126) //
						.setControlMode(ControlMode.SMART) //
						.build())
				.next(new TestCase()) //
				.deactivate();
	}
}
