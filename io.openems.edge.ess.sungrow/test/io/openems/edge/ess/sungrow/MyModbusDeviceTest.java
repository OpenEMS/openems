package io.openems.edge.ess.sungrow;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class MyModbusDeviceTest {

    private static final String COMPONENT_ID = "ess0";
    private static final String MODBUS_ID = "modbus0";

    @Test
    public void test() throws Exception {
	new ComponentTest(new SungrowEssImpl()) //
		.addReference("cm", new DummyConfigurationAdmin()) //
		.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
		.activate(MyConfig.create() //
			.setId(COMPONENT_ID) //
			.setModbusId(MODBUS_ID) //
			.setReadOnly(false) //
			.build())
		.next(new TestCase());
    }

}
