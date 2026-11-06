package io.openems.edge.evse.chargepoint.smartfox;

import static io.openems.edge.meter.api.PhaseRotation.L1_L2_L3;

import org.junit.Test;

import io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class EvseChargePointSmartfoxImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new EvseChargePointSmartfoxImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("httpBridgeFactory", DummyBridgeHttpFactory.ofDummyBridge()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("evseChargePoint0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setChargePointNumber(1) //
						.setIp("192.168.0.50") //
						.setMaxChargingPower(11000) //
						.setReadOnly(false) //
						.setPhaseRotation(L1_L2_L3) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}
}
