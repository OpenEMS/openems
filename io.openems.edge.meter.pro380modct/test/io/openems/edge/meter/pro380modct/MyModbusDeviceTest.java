package io.openems.edge.meter.pro380modct;

import io.openems.common.types.MeterType;
import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.common.test.DummyConfigurationAdmin;

public class MyModbusDeviceTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new Pro380modctImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("component0") //
						.setModbusId("modbus0") //
                        .setType(MeterType.GRID)
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

}
