package io.openems.edge.evse.chargepoint.keba.common;

import static io.openems.common.utils.ReflectionUtils.getValueViaReflection;
import static io.openems.edge.evse.chargepoint.keba.common.KebaUdp.preprocessDisplayTest;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.evse.chargepoint.keba.common.enums.LogVerbosity;
import io.openems.edge.evse.chargepoint.keba.udp.core.EvseChargePointKebaUdpCoreImpl;

public class KebaUdpTest {

	@Test
	public void test() {
		assertEquals("display 0 0 0 0 ", preprocessDisplayTest(""));
		assertEquals("display 0 0 0 0 123456789$123456789$123", preprocessDisplayTest("123456789 123456789 123456789"));
	}

	/**
	 * Prepares a {@link ComponentTest} with {@link KebaUdp}.
	 * 
	 * @param sut the {@link KebaUdp} implementation
	 * @return the {@link ComponentTest}
	 * @throws Exception on error
	 */
	public static ComponentTest prepareKebaUdp(KebaUdp sut) throws Exception {
		return new ComponentTest(sut) //
				.addReference("kebaUdpCore", new EvseChargePointKebaUdpCoreImpl());
	}

	/**
	 * Use the given {@link TestCase} to test all expected Channels from
	 * {@link KebaUdp}.
	 * 
	 * @param sut {@link KebaUdp}
	 * @param tc  The {@link TestCase}
	 * @throws Exception on error
	 */
	public static void testKebaUdpChannels(KebaUdp sut, TestCase tc) throws Exception {
		final AbstractUdpReadHandler<?> rh = getValueViaReflection(sut, "readHandler");
		final var logVerbosity = LogVerbosity.DEBUG_LOG;
		tc //
				.onBeforeProcessImage(() -> {
					rh.accept(REPORT_1, logVerbosity);
					rh.accept(REPORT_2, logVerbosity);
					rh.accept(REPORT_3, logVerbosity);
				}) //
				.output(KebaUdp.ChannelId.COMMUNICATION_FAILED, false) //
				.output(KebaUdp.ChannelId.CHARGINGSTATION_STATE_ERROR, false) //
				.output(KebaUdp.ChannelId.DIP_SWITCH_ERROR_1_3_NOT_SET_FOR_COMM, false) //
				.output(KebaUdp.ChannelId.DIP_SWITCH_ERROR_2_6_NOT_SET_FOR_STATIC_IP, false) //
				.output(KebaUdp.ChannelId.DIP_SWITCH_ERROR_2_6_SET_FOR_DYNAMIC_IP, false) //
				.output(KebaUdp.ChannelId.DIP_SWITCH_INFO_2_5_SET_FOR_MASTER_SLAVE_COMM, false) //
				.output(KebaUdp.ChannelId.DIP_SWITCH_INFO_2_8_SET_FOR_INSTALLATION, false) //
				.output(KebaUdp.ChannelId.DIP_SWITCH_MAX_HW, 32000) //
				.output(KebaUdp.ChannelId.PRODUCT_SERIES_IS_NOT_COMPATIBLE, false) //
				.output(KebaUdp.ChannelId.NO_ENERGY_METER_INSTALLED, false) //

				.output(KebaUdp.ChannelId.PRODUCT, "KC-P30-EC240422-E00") //
				.output(KebaUdp.ChannelId.SERIAL, "12345678") //
				.output(KebaUdp.ChannelId.FIRMWARE, "P30 v 3.10.57 (240521-093236)") //
				.output(KebaUdp.ChannelId.COM_MODULE, false) //
				.output(KebaUdp.ChannelId.BACKEND, false) //
				.output(KebaUdp.ChannelId.DIP_SWITCH_1, "00100101") //
				.output(KebaUdp.ChannelId.DIP_SWITCH_2, "00000010") //

				.output(KebaUdp.ChannelId.ERROR_1, 0) //
				.output(KebaUdp.ChannelId.ERROR_2, 0) //
				.output(KebaUdp.ChannelId.AUTH_ON, false) //
				.output(KebaUdp.ChannelId.AUTH_REQ, false) //
				.output(KebaUdp.ChannelId.ENABLE_SYS, false) //
				.output(KebaUdp.ChannelId.ENABLE_USER, false) //
				.output(KebaUdp.ChannelId.MAX_CURR, 0) //
				.output(KebaUdp.ChannelId.MAX_CURR_PERCENT, 1_000) //
				.output(KebaUdp.ChannelId.CURR_HW, 32_000) //
				.output(KebaUdp.ChannelId.CURR_USER, 10_000) //
				.output(KebaUdp.ChannelId.CURR_FAILSAFE, 0) //
				.output(KebaUdp.ChannelId.TIMEOUT_FAILSAFE, 0) //
				.output(KebaUdp.ChannelId.CURR_TIMER, 0) //
				.output(KebaUdp.ChannelId.TIMEOUT_CT, 0) //
				.output(KebaUdp.ChannelId.SETENERGY, 0) //
				.output(KebaUdp.ChannelId.OUTPUT, 0) //
				.output(KebaUdp.ChannelId.INPUT, false) //
		;
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
			"State": 3,
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
			"U1": 229,
			"U2": 230,
			"U3": 231,
			"I1": 7000,
			"I2": 8000,
			"I3": 9000,
			"P": 5678000,
			"PF": 905,
			"E pres": 65302,
			"E total": 7747835,
			"Serial": "22054282",
			"Sec": 534926
			}
			""";

}
