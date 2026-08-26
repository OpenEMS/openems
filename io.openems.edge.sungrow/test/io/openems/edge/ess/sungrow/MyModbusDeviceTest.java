package io.openems.edge.ess.sungrow;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class MyModbusDeviceTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new SungrowEssImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setModbusId("modbus0") //
						.setReadOnly(false) //
						.build())
				.next(new TestCase()) //
				.deactivate();
	}

}
