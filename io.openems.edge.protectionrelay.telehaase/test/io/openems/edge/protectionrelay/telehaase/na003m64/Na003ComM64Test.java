package io.openems.edge.protectionrelay.telehaase.na003m64;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.common.channel.Level;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.protectionrelay.telehaase.na003m64.enums.ModbusProperties;

public class Na003ComM64Test {

	/**
	 * Creates a DummyModbusBridge with a process image matching the read task on
	 * registers 10000-10020.
	 *
	 * @return the DummyModbusBridge
	 */
	private static DummyModbusBridge createModbusBridge() {
		return new DummyModbusBridge("modbus0") //
				.withRegisters(10000, //
						5, // 10000: LED_REL_1 + LED_REL_3
						30, // 10001: DIGITAL_INPUT_2..5
						0, 0, 0, 0, 0, // 10002-10006: dummy
						32769, // 10007: ERROR_LOGIC_OVERVOLTAGE_LL + ERROR_LOGIC_MASTER_ERROR
						42, // 10008: GOOD_COUNTDOWN
						0, 0, // 10009-10010: dummy
						100, 101, 102, // 10011-10013: U_DELTA_1_2/2_3/3_1
						230, 231, 232, // 10014-10016: U_STAR_1/2/3
						240, 241, 242, // 10017-10019: U_AVG_1/2/3
						50000); // 10020: FREQUENCY_MILLI_HERTZ (50000 mHz = 50 Hz)
	}

	@Test
	public void test() throws Exception {
		new ComponentTest(new Na003ComM64Impl()) //
				.addReference("setModbus", createModbusBridge()) //
				.activate(MyConfig.create() //
						.setId("relay0") //
						.setModbusId("modbus0") //
						.build()) //
				.next(new TestCase()) //
				.next(new TestCase() //
						.activateStrictMode() //
						.output(OpenemsComponent.ChannelId.STATE, Level.INFO) // DummyModbusBridge is disabled in tests
						.output(ModbusComponent.ChannelId.MODBUS_COMMUNICATION_FAILED, false) //
						.output(Na003ComM64.ChannelId.DIGITAL_INPUT_1, false) //
						.output(Na003ComM64.ChannelId.DIGITAL_INPUT_2, true) //
						.output(Na003ComM64.ChannelId.DIGITAL_INPUT_3, true) //
						.output(Na003ComM64.ChannelId.DIGITAL_INPUT_4, true) //
						.output(Na003ComM64.ChannelId.DIGITAL_INPUT_5, true) //
						.output(Na003ComM64.ChannelId.ERROR_LOGIC_CONTACT_REPORTS_CLOSED, false) //
						.output(Na003ComM64.ChannelId.ERROR_LOGIC_CONTACT_REPORTS_OPEN, false) //
						.output(Na003ComM64.ChannelId.ERROR_LOGIC_ERROR_DELAY_RUNNING, false) //
						.output(Na003ComM64.ChannelId.ERROR_LOGIC_ERROR_SYSTEM, false) //
						.output(Na003ComM64.ChannelId.ERROR_LOGIC_FREQUENCY_CHANGE_RATE, false) //
						.output(Na003ComM64.ChannelId.ERROR_LOGIC_GOOD_DELAY_RUNNING, false) //
						.output(Na003ComM64.ChannelId.ERROR_LOGIC_MASTER_ERROR, true) //
						.output(Na003ComM64.ChannelId.ERROR_LOGIC_OVERFREQUENCY, false) //
						.output(Na003ComM64.ChannelId.ERROR_LOGIC_OVERVOLTAGE_LL, true) //
						.output(Na003ComM64.ChannelId.ERROR_LOGIC_OVERVOLTAGE_LN, false) //
						.output(Na003ComM64.ChannelId.ERROR_LOGIC_PHASE_SHIFT, false) //
						.output(Na003ComM64.ChannelId.ERROR_LOGIC_REMOTE_SHUTDOWN_SELF_TEST, false) //
						.output(Na003ComM64.ChannelId.ERROR_LOGIC_TEN_MIN_OVERVOLTAGE, false) //
						.output(Na003ComM64.ChannelId.ERROR_LOGIC_UNDERVOLTAGE_LL, false) //
						.output(Na003ComM64.ChannelId.ERROR_LOGIC_UNDERVOLTAGE_LN, false) //
						.output(Na003ComM64.ChannelId.ERROR_LOGIC_UNDERFREQUENCY, false) //
						.output(Na003ComM64.ChannelId.FREQUENCY, 50000) //
						.output(Na003ComM64.ChannelId.GOOD_COUNTDOWN, 42) //
						.output(Na003ComM64.ChannelId.LED_REL_1, true) //
						.output(Na003ComM64.ChannelId.LED_REL_2, false) //
						.output(Na003ComM64.ChannelId.LED_REL_3, true) //
						.output(Na003ComM64.ChannelId.MODBUS_ADDRESS, null) // write-only via FC6
						.output(Na003ComM64.ChannelId.MODBUS_BAUDRATE, null) // write-only via FC6
						.output(Na003ComM64.ChannelId.MODBUS_PROPERTIES, ModbusProperties.UNDEFINED) // FC6 write-only
						.output(Na003ComM64.ChannelId.U_AVG_1, 240) //
						.output(Na003ComM64.ChannelId.U_AVG_2, 241) //
						.output(Na003ComM64.ChannelId.U_AVG_3, 242) //
						.output(Na003ComM64.ChannelId.U_DELTA_1_2, 100) //
						.output(Na003ComM64.ChannelId.U_DELTA_2_3, 101) //
						.output(Na003ComM64.ChannelId.U_DELTA_3_1, 102) //
						.output(Na003ComM64.ChannelId.U_STAR_1, 230) //
						.output(Na003ComM64.ChannelId.U_STAR_2, 231) //
						.output(Na003ComM64.ChannelId.U_STAR_3, 232) //
				) //
				.deactivate();
	}

	@Test
	public void testDebugLog() throws Exception {
		var sut = new Na003ComM64Impl();
		new ComponentTest(sut) //
				.addReference("setModbus", createModbusBridge()) //
				.activate(MyConfig.create() //
						.setId("relay0") //
						.setModbusId("modbus0") //
						.build()) //
				.next(new TestCase());
		assertEquals("Frequency:50000 mHz|Input:-xxxx", sut.debugLog());
	}

}
