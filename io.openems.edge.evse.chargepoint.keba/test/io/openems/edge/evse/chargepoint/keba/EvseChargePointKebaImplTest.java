package io.openems.edge.evse.chargepoint.keba;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class EvseChargePointKebaImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new EvseChargePointKebaImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("evseChargePoint0") //
						.setModbusId("modbus0") //
						.setPhase(Phase.THREE) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}
}