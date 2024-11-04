package io.openems.edge.meter.camillebauer.aplus;

import static io.openems.common.types.MeterType.GRID;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class MeterCamillebauerAplusImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new MeterCamillebauerAplusImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("component0") //
						.setModbusId("modbus0") //
						.setMeterType(GRID) //
						.setInvert(false) //
						.build())
				.next(new TestCase());
	}

}
