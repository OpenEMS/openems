package io.openems.edge.meter.phoenixcontact;

import static io.openems.common.types.MeterType.PRODUCTION;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class PhoenixContactMeterImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new PhoenixContactMeterImpl()) //
				.addReference("cm", new DummyConfigurationAdmin())
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("meter0") //
						.setModbusId("modbus0") //
						.setMeterType(PRODUCTION) //
						.build())
				.next(new TestCase());
	}

}
