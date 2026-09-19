package io.openems.edge.goodwe.emergencypowermeter;

import org.junit.jupiter.api.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;

public class GoodWeEmergencyPowerMeterTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new GoodWeEmergencyPowerMeterImpl()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("meter2") //
						.setModbusId("modbus0") //
						.build());
	}
}
