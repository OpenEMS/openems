package io.openems.edge.edge2edge.meter;

import static io.openems.common.types.MeterType.PRODUCTION;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class Edge2EdgeEssMeterImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new Edge2EdgeMeterImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("meter0") //
						.setModbusId("modbus0") //
						.setRemoteComponentId("meter0") //
						.setMeterType(PRODUCTION) //
						.build())
				.next(new TestCase());
	}

}
