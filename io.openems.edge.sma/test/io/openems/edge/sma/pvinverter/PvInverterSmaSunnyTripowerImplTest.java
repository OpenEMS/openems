package io.openems.edge.sma.pvinverter;

import static io.openems.edge.common.type.Phase.SingleOrAllPhase.ALL;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class PvInverterSmaSunnyTripowerImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new PvInverterSmaSunnyTripowerImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("pvInverter0") //
						.setReadOnly(true) //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setPhase(ALL) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}
}