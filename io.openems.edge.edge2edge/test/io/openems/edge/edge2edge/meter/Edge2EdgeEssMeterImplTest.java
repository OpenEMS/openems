package io.openems.edge.edge2edge.meter;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.meter.api.MeterType;

public class Edge2EdgeEssMeterImplTest {

    private static final String COMPONENT_ID = "meter0";
    private static final String MODBUS_ID = "modbus0";

    @Test
    public void test() throws Exception {
	new ComponentTest(new Edge2EdgeMeterImpl()) //
		.addReference("cm", new DummyConfigurationAdmin()) //
		.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
		.activate(MyConfig.create() //
			.setId(COMPONENT_ID) //
			.setModbusId(MODBUS_ID) //
			.setRemoteComponentId(COMPONENT_ID) //
			.setMeterType(MeterType.PRODUCTION) //
			.build())
		.next(new TestCase());
    }

}
