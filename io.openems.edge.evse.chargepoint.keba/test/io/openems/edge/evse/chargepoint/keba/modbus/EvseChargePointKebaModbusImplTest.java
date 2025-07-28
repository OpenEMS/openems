package io.openems.edge.evse.chargepoint.keba.modbus;

import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;
import static io.openems.edge.evse.api.chargepoint.PhaseRotation.L2_L3_L1;
import static io.openems.edge.evse.chargepoint.keba.common.CommonNaturesTest.testElectricityMeterChannels;
import static io.openems.edge.evse.chargepoint.keba.common.EvseKebaTest.testEvseKebaChannels;
import static io.openems.edge.evse.chargepoint.keba.common.KebaModbusTest.prepareKebaModbus;
import static io.openems.edge.evse.chargepoint.keba.common.KebaModbusTest.testKebaModbusChannels;
import static io.openems.edge.evse.chargepoint.keba.common.KebaTest.testKebaChannels;
import static io.openems.edge.evse.chargepoint.keba.common.enums.LogVerbosity.DEBUG_LOG;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;

public class EvseChargePointKebaModbusImplTest {

	@Test
	public void test() throws Exception {
		final var sut = new EvseKebaModbusImpl();
		final var tc = new TestCase();
		testElectricityMeterChannels(tc);
		testKebaChannels(tc);
		testKebaModbusChannels(tc);
		testEvseKebaChannels(tc);

		prepareKebaModbus(sut) //
				.activate(MyConfig.create() //
						.setId("evseChargePoint0") //
						.setModbusId("modbus0") //
						.setWiring(THREE_PHASE) //
						.setP30hasS10PhaseSwitching(false) //
						.setPhaseRotation(L2_L3_L1) //
						.setLogVerbosity(DEBUG_LOG) //
						.build()) //
				.next(new TestCase(), 20) //
				.next(tc) //
				.deactivate();

		assertEquals("L:5678 W|SetCurrent:UNDEFINED|SetEnable:-1:Undefined", sut.debugLog());
	}
}