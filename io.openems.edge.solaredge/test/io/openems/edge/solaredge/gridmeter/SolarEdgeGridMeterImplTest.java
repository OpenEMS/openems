package io.openems.edge.solaredge.gridmeter;

import static io.openems.common.types.MeterType.GRID;

import org.junit.jupiter.api.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class SolarEdgeGridMeterImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new SolarEdgeGridMeterImpl()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("meter0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setType(GRID) //
						.setInvert(false) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}
}