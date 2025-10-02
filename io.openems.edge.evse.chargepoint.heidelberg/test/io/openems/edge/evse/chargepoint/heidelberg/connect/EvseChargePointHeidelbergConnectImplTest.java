package io.openems.edge.evse.chargepoint.heidelberg.connect;

import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;
import static io.openems.edge.evse.api.chargepoint.PhaseRotation.L1_L2_L3;
import static io.openems.edge.evse.chargepoint.heidelberg.connect.enums.PhaseSwitching.FORCE_THREE_PHASE;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class EvseChargePointHeidelbergConnectImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new EvseChargePointHeidelbergConnectImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("evseChargePoint0") //
						.setModbusId("modbus0") //
						.setWiring(THREE_PHASE) //
						.setModbusUnitId(1) //
						.setDebugMode(false) //
						.setReadOnly(false) //
						.setPhaseSwitching(FORCE_THREE_PHASE) //
						.setPhaseRotation(L1_L2_L3) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}
}