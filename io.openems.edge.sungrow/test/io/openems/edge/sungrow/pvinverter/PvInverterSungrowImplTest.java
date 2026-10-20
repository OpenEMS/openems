package io.openems.edge.sungrow.pvinverter;

import org.junit.jupiter.api.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class PvInverterSungrowImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new PvInverterSungrowImpl()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("pvInverter0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

}
