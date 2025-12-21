package io.openems.edge.sma.ess.stpxx3se.battery;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class SmaBatteryImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new SmaBatteryImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
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
