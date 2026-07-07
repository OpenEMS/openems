package io.openems.edge.fronius.pvinverter;

import org.junit.jupiter.api.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class PvInverterFroniusImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new PvInverterFroniusImpl()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("pvInverter0") //
						.setReadOnly(true) //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}
}