package io.openems.edge.meter.DSMR_Modbus;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class MyModbusDeviceTest {

	private static final String COMPONENT_ID = "component0";
	private static final String MODBUS_ID = "modbus0";


	
	@Test
	public void test() throws Exception {
		new ComponentTest(new DSMR_ModbusImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setModbusId(MODBUS_ID)//
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

}
