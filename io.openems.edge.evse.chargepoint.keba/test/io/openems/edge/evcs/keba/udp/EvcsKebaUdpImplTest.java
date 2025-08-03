package io.openems.edge.evcs.keba.udp;

import static io.openems.edge.evcs.api.PhaseRotation.L2_L3_L1;
import static io.openems.edge.evse.chargepoint.keba.common.CommonNaturesTest.testDeprecatedEvcsChannels;
import static io.openems.edge.evse.chargepoint.keba.common.CommonNaturesTest.testElectricityMeterChannels;
import static io.openems.edge.evse.chargepoint.keba.common.CommonNaturesTest.testEvcsChannels;
import static io.openems.edge.evse.chargepoint.keba.common.CommonNaturesTest.testManagedEvcsChannels;
import static io.openems.edge.evse.chargepoint.keba.common.EvcsKebaTest.testEvcsKebaChannels;
import static io.openems.edge.evse.chargepoint.keba.common.KebaTest.testKebaChannels;
import static io.openems.edge.evse.chargepoint.keba.common.KebaUdpTest.prepareKebaUdp;
import static io.openems.edge.evse.chargepoint.keba.common.KebaUdpTest.testKebaUdpChannels;
import static io.openems.edge.evse.chargepoint.keba.common.enums.LogVerbosity.DEBUG_LOG;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;

public class EvcsKebaUdpImplTest {

	@Test
	public void test() throws Exception {
		final var sut = new EvcsKebaUdpImpl();
		final var tc = new TestCase() //
				.activateStrictMode();
		testEvcsChannels(tc);
		testManagedEvcsChannels(tc);
		testDeprecatedEvcsChannels(tc);
		testElectricityMeterChannels(tc);
		testKebaChannels(tc);
		testKebaUdpChannels(sut, tc);
		testEvcsKebaChannels(tc);

		prepareKebaUdp(sut) //
				.activate(MyConfig.create() //
						.setId("evcs0") //
						.setReadOnly(false) //
						.setIp("172.0.0.1") //
						.setPhaseRotation(L2_L3_L1) //
						.setMinHwCurrent(6000) //
						// .setUseDisplay(false) //
						.setLogVerbosity(DEBUG_LOG) //
						.build()) //
				.next(tc) //
				.deactivate();

		assertEquals("L:5678 W|SetCurrent:UNDEFINED|SetEnable:-1:Undefined", sut.debugLog());
	}
}
