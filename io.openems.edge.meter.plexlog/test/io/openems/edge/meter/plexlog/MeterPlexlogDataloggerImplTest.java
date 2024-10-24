package io.openems.edge.meter.plexlog;

import static io.openems.common.types.MeterType.PRODUCTION;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class MeterPlexlogDataloggerImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new MeterPlexlogDataloggerImpl()) //
				.addReference("cm", new DummyConfigurationAdmin())
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("meter0") //
						.setMeterType(PRODUCTION) //
						.setModbusId("modbus0") //
						.build())
				.next(new TestCase());
	}

}
