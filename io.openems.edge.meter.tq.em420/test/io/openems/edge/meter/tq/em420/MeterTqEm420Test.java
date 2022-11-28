package io.openems.edge.meter.tq.em420;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.meter.api.MeterType;



public class MeterTqEm420Test {

	private static final String COMPONENT_ID = "component0";
	private static final String MODBUS_ID = "modbus0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new MeterTqEm420Impl()) //
		.addReference("cm", new DummyConfigurationAdmin()) //
		.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
		.activate(MyConfig.create() //
				.setId(COMPONENT_ID) //
				.setModbusId(MODBUS_ID) //
				.setModbusUnitId(1) //
				.setMeterType(MeterType.GRID) //
				.build()) //
		.next(new TestCase());
	}

}
