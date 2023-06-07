package io.openems.edge.meter.phoenixcontact;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.meter.api.MeterType;

public class PhoenixContactMeterImplTest {

	private static final String COMPONENT_ID = "meter0";
	private static final String MODBUS_ID = "modbus0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new PhoenixContactMeterImpl()) //
				.addReference("cm", new DummyConfigurationAdmin())
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setModbusId(MODBUS_ID) //
						.setMeterType(MeterType.PRODUCTION) //
						.build())
				.next(new TestCase());
	}

}
