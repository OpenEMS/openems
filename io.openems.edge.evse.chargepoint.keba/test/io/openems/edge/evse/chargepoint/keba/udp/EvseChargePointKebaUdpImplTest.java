package io.openems.edge.evse.chargepoint.keba.udp;

import static io.openems.edge.evse.api.chargepoint.PhaseRotation.L2_L3_L1;
import static io.openems.edge.evse.chargepoint.keba.common.enums.LogVerbosity.DEBUG_LOG;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.type.Phase.SingleOrThreePhase;
import io.openems.edge.evse.chargepoint.keba.common.EvseChargePointKeba;
import io.openems.edge.evse.chargepoint.keba.common.enums.CableState;
import io.openems.edge.evse.chargepoint.keba.common.enums.ChargingState;
import io.openems.edge.evse.chargepoint.keba.common.enums.PhaseSwitchSource;
import io.openems.edge.evse.chargepoint.keba.common.enums.PhaseSwitchState;
import io.openems.edge.evse.chargepoint.keba.udp.core.EvseChargePointKebaUdpCoreImpl;
import io.openems.edge.meter.api.ElectricityMeter;

public class EvseChargePointKebaUdpImplTest {

	@Test
	public void test() throws Exception {
		final var sut = new EvseChargePointKebaUdpImpl();
		final var rh = sut.readHandler;
		final var config = MyConfig.create() //
				.setId("evcs0") //
				.setReadOnly(false) //
				.setIp("172.0.0.1") //
				.setWiring(SingleOrThreePhase.THREE_PHASE) //
				.setP30hasS10PhaseSwitching(false) //
				.setPhaseRotation(L2_L3_L1) //
				// .setUseDisplay(false) //
				.setLogVerbosity(DEBUG_LOG) //
				.build();
		final var logVerbosity = config.logVerbosity();
		new ComponentTest(sut) //
				.addReference("kebaUdpCore", new EvseChargePointKebaUdpCoreImpl()) //
				.activate(config) //

				.next(new TestCase() //
						.onBeforeProcessImage(() -> rh.accept(REPORT_1, logVerbosity)) //
						.output(EvseChargePointKebaUdp.ChannelId.PRODUCT, "KC-P30-EC240422-E00") //
						.output(EvseChargePointKebaUdp.ChannelId.SERIAL, "12345678") //
						.output(EvseChargePointKeba.ChannelId.FIRMWARE, "P30 v 3.10.57 (240521-093236)") //
						.output(EvseChargePointKebaUdp.ChannelId.COM_MODULE, false) //
						.output(EvseChargePointKebaUdp.ChannelId.BACKEND, false) //
						.output(EvseChargePointKebaUdp.ChannelId.DIP_SWITCH_1, "00100101") //
						.output(EvseChargePointKebaUdp.ChannelId.DIP_SWITCH_2, "00000010")) //

				.next(new TestCase() //
						.onBeforeProcessImage(() -> rh.accept(REPORT_2, logVerbosity)) //
						.output(EvseChargePointKeba.ChannelId.CHARGING_STATE, ChargingState.INTERRUPTED) //
						.output(EvseChargePointKeba.ChannelId.CABLE_STATE, CableState.PLUGGED_AND_LOCKED) //
						.output(EvseChargePointKebaUdp.ChannelId.ERROR_1, 0) //
						.output(EvseChargePointKebaUdp.ChannelId.ERROR_2, 0) //
						.output(EvseChargePointKebaUdp.ChannelId.AUTH_ON, false) //
						.output(EvseChargePointKebaUdp.ChannelId.AUTH_REQ, false) //
						.output(EvseChargePointKebaUdp.ChannelId.ENABLE_SYS, false) //
						.output(EvseChargePointKebaUdp.ChannelId.ENABLE_USER, false) //
						.output(EvseChargePointKebaUdp.ChannelId.MAX_CURR, 0) //
						.output(EvseChargePointKebaUdp.ChannelId.MAX_CURR_PERCENT, 1_000) //
						.output(EvseChargePointKebaUdp.ChannelId.CURR_HW, 32_000) //
						.output(EvseChargePointKebaUdp.ChannelId.CURR_USER, 10_000) //
						.output(EvseChargePointKebaUdp.ChannelId.CURR_FAILSAFE, 0) //
						.output(EvseChargePointKebaUdp.ChannelId.TIMEOUT_FAILSAFE, 0) //
						.output(EvseChargePointKebaUdp.ChannelId.CURR_TIMER, 0) //
						.output(EvseChargePointKebaUdp.ChannelId.TIMEOUT_CT, 0) //
						.output(EvseChargePointKebaUdp.ChannelId.SETENERGY, 0) //
						.output(EvseChargePointKebaUdp.ChannelId.OUTPUT, 0) //
						.output(EvseChargePointKebaUdp.ChannelId.INPUT, false) //
						.output(EvseChargePointKeba.ChannelId.PHASE_SWITCH_SOURCE, PhaseSwitchSource.NONE) //
						.output(EvseChargePointKeba.ChannelId.PHASE_SWITCH_STATE, PhaseSwitchState.SINGLE)) //

				.next(new TestCase() //
						.onBeforeProcessImage(() -> rh.accept(REPORT_3, logVerbosity)) //
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
						.output(EvseChargePointKeba.ChannelId.ENERGY_SESSION, 6530) //
						.output(EvseChargePointKeba.ChannelId.POWER_FACTOR, 905) //

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
