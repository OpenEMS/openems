package io.openems.edge.goodwe.charger.singlestring;

import org.junit.jupiter.api.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;

public class GoodWeChargerPv1Test {

	@Test
	public void test() throws Exception {
		new ComponentTest(new GoodWeChargerPv1()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("charger0") //
						.setBatteryInverterId("ess0") //
						.setModbusId("modbus0") //
						.build());
	}
}
