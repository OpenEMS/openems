package io.openems.edge.evse.chargepoint.keba.udp;

import static io.openems.edge.evse.api.chargepoint.PhaseRotation.L2_L3_L1;
import static io.openems.edge.evse.chargepoint.keba.common.CommonNaturesTest.testElectricityMeterChannels;
import static io.openems.edge.evse.chargepoint.keba.common.EvseKebaTest.testEvseKebaChannels;
import static io.openems.edge.evse.chargepoint.keba.common.KebaTest.testKebaChannels;
import static io.openems.edge.evse.chargepoint.keba.common.KebaUdpTest.prepareKebaUdp;
import static io.openems.edge.evse.chargepoint.keba.common.KebaUdpTest.testKebaUdpChannels;
import static io.openems.edge.evse.chargepoint.keba.common.enums.LogVerbosity.DEBUG_LOG;
import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.type.Phase.SingleOrThreePhase;

public class EvseChargePointKebaUdpImplTest {

	// Ignored, because this test sometimes fails for unknown reason
	@Ignore
	@Test
	public void test() throws Exception {
		final var sut = new EvseKebaUdpImpl();
		final var tc = new TestCase() //
				.activateStrictMode();
		testElectricityMeterChannels(tc);
		testKebaChannels(tc);
		testKebaUdpChannels(sut, tc);
		testEvseKebaChannels(tc);

		prepareKebaUdp(sut) //
				.activate(MyConfig.create() //
						.setId("evcs0") //
						.setReadOnly(false) //
						.setIp("172.0.0.1") //
						.setWiring(SingleOrThreePhase.THREE_PHASE) //
						.setP30hasS10PhaseSwitching(false) //
						.setPhaseRotation(L2_L3_L1) //
						// .setUseDisplay(false) //
						.setLogVerbosity(DEBUG_LOG) //
						.build()) //
				.next(tc) //
				.deactivate();

		assertEquals("L:5678 W|SetCurrent:UNDEFINED|SetEnable:-1:Undefined", sut.debugLog());
	}
}
