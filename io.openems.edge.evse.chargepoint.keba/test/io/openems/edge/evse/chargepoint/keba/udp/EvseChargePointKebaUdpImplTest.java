package io.openems.edge.evse.chargepoint.keba.udp;

import static io.openems.edge.evse.api.SingleThreePhase.THREE_PHASE;
import static io.openems.edge.evse.api.chargepoint.PhaseRotation.L2_L3_L1;
import static io.openems.edge.evse.chargepoint.keba.common.enums.P30S10PhaseSwitching.NOT_AVAILABLE;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.evse.chargepoint.keba.common.EvseChargePointKeba;
import io.openems.edge.evse.chargepoint.keba.common.enums.CableState;
import io.openems.edge.evse.chargepoint.keba.udp.core.EvseChargePointKebaUdpCoreImpl;
import io.openems.edge.meter.api.ElectricityMeter;

public class EvseChargePointKebaUdpImplTest {

	@Test
	public void test() throws Exception {
		var sut = new EvseChargePointKebaUdpImpl();
		var rh = sut.readHandler;
		new ComponentTest(sut) //
				.addReference("kebaUdpCore", new EvseChargePointKebaUdpCoreImpl()) //
				.activate(MyConfig.create() //
						.setId("evcs0") //
						.setDebugMode(false) //
						.setIp("172.0.0.1") //
						.setWiring(THREE_PHASE) //
						.setP30S10PhaseSwitching(NOT_AVAILABLE) //
						.setPhaseRotation(L2_L3_L1) //
						// .setUseDisplay(false) //
						.build()) //

				.next(new TestCase() //
						.onBeforeProcessImage(() -> rh.accept(REPORT_1)) //
						.output(EvseChargePointKebaUdp.ChannelId.SERIAL, "12345678") //
						.output(EvseChargePointKeba.ChannelId.FIRMWARE, "P30 v 3.10.57 (240521-093236)") //
						.output(EvseChargePointKebaUdp.ChannelId.COM_MODULE, "0") //
						.output(EvseChargePointKebaUdp.ChannelId.DIP_SWITCH_1, "00100101") //
						.output(EvseChargePointKebaUdp.ChannelId.DIP_SWITCH_2, "00000010") //
						.output(EvseChargePointKebaUdp.ChannelId.PRODUCT, "KC-P30-EC240422-E00")) //

				.next(new TestCase() //
						.onBeforeProcessImage(() -> rh.accept(REPORT_2)) //
						.output(EvseChargePointKebaUdp.ChannelId.ERROR_1, 0) //
						.output(EvseChargePointKebaUdp.ChannelId.ERROR_2, 0) //
						.output(EvseChargePointKeba.ChannelId.CABLE_STATE, CableState.PLUGGED_AND_LOCKED) //
						.output(EvseChargePointKebaUdp.ChannelId.ENABLE_SYS, false) //
						.output(EvseChargePointKebaUdp.ChannelId.ENABLE_USER, false) //
						.output(EvseChargePointKebaUdp.ChannelId.MAX_CURR_PERCENT, 1_000) //
						.output(EvseChargePointKebaUdp.ChannelId.CURR_FAILSAFE, 0) //
						.output(EvseChargePointKebaUdp.ChannelId.TIMEOUT_FAILSAFE, 0) //
						.output(EvseChargePointKebaUdp.ChannelId.CURR_TIMER, 0) //
						.output(EvseChargePointKebaUdp.ChannelId.TIMEOUT_CT, 0) //
						.output(EvseChargePointKebaUdp.ChannelId.OUTPUT, false) //
						.output(EvseChargePointKebaUdp.ChannelId.INPUT, false) //
						.output(EvseChargePointKebaUdp.ChannelId.MAX_CURR, 32_000) //
						.output(EvseChargePointKebaUdp.ChannelId.CURR_USER, 1_0000)) //

				.next(new TestCase() //
						.onBeforeProcessImage(() -> rh.accept(REPORT_3)) //
						.output(ElectricityMeter.ChannelId.VOLTAGE, 227_500) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, null) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, 228_000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, 227_000) //
						.output(ElectricityMeter.ChannelId.CURRENT, 9_075) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, 0) //
						.output(ElectricityMeter.ChannelId.CURRENT_L2, 9_075) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, 0) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 1_866) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 0) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 1_866) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 0) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, 7747834L) //
						// .output(EvseChargePointKebaUdp.ChannelId.ENERGY_SESSION, 6530) //
						.output(EvseChargePointKebaUdp.ChannelId.COS_PHI, 905) //

				);
	}

	private static final String REPORT_1 = """
			{
			"ID": "1",
			"Product": "KC-P30-EC240422-E00",
			"Serial": "12345678",
			"Firmware":"P30 v 3.10.57 (240521-093236)",
			"COM-module": 0,
			"Backend": 0,
			"timeQ": 3,
			"setBoot": 0,
			"DIP-Sw1": "0x25",
			"DIP-Sw2": "0x02",
			"Sec": 530786
			}
			""";
	private static final String REPORT_2 = """
			{
			"ID": "2",
			"State": 5,
			"Error1": 0,
			"Error2": 0,
			"Plug": 7,
			"AuthON": 0,
			"Authreq": 0,
			"Enable sys": 0,
			"Enable user": 0,
			"Max curr": 0,
			"Max curr %": 1000,
			"Curr HW": 32000,
			"Curr user": 10000,
			"Curr FS": 0,
			"Tmo FS": 0,
			"Curr timer": 0,
			"Tmo CT": 0,
			"Setenergy": 0,
			"Output": 0,
			"Input": 0,
			"X2 phaseSwitch source": 0,
			"X2 phaseSwitch": 0,
			"Serial": "22054282",
			"Sec": 530786
			}
			""";
	private static final String REPORT_3 = """
			{
			"ID": "3",
			"U1": 228,
			"U2": 227,
			"U3": 0,
			"I1": 9075,
			"I2": 0,
			"I3": 0,
			"P": 1866156,
			"PF": 905,
			"E pres": 65302,
			"E total": 77478335,
			"Serial": "22054282",
			"Sec": 534926
			}
			""";

}
