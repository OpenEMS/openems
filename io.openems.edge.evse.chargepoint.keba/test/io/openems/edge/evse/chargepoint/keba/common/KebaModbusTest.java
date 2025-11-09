package io.openems.edge.evse.chargepoint.keba.common;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class KebaModbusTest {

	/**
	 * Prepares a {@link ComponentTest} with {@link KebaModbus}.
	 * 
	 * @param kebaModbusImpl the {@link KebaModbus} implementation
	 * @return the {@link ComponentTest}
	 * @throws Exception on error
	 */
	public static ComponentTest prepareKebaModbus(KebaModbus kebaModbusImpl) throws OpenemsException, Exception {
		return new ComponentTest(kebaModbusImpl) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0") //
						.withRegisters(1000, // STATUS
								new int[] { 0x0000, 0x0003 }) //
						.withRegisters(1004, // PLUG
								new int[] { 0x0000, 0x0007 }) //
						.withRegisters(1006, // ERROR_CODE - TODO
								new int[] { 0x0000, 0x0000 }) //
						.withRegisters(1008, // CURRENT_L1: 7_000
								new int[] { 0x0000, 0x1B58 }) //
						.withRegisters(1010, // CURRENT_L2: 8_000
								new int[] { 0x0000, 0x1F40 }) //
						.withRegisters(1012, // CURRENT_L3: 9_000
								new int[] { 0x0000, 0x2328 }) //
						.withRegisters(1014, // SERIAL_NUMBER - TODO
								new int[] { 0x0000, 0x0000 }) //
						.withRegisters(1016, // Product Type and Features
								new int[] { 0x0040, 0x4657 }) //
						.withRegisters(1018, // FIRMWARE
								new int[] { 0x0000, 0x27DB }) //
						.withRegisters(1020, // ACTIVE_POWER: 5_678_000
								new int[] { 0x0056, 0xA3B0 }) //
						.withRegisters(1036, // ACTIVE_PRODUCTION_ENERGY: 7_747_835
								new int[] { 0x0076, 0x38FB }) //
						.withRegisters(1040, // VOLTAGE_L1: 229
								new int[] { 0x0000, 0x00E5 }) //
						.withRegisters(1042, // VOLTAGE_L2: 230
								new int[] { 0x0000, 0x00E6 }) //
						.withRegisters(1044, // VOLTAGE_L3: 231
								new int[] { 0x0000, 0x00E7 }) //
						.withRegisters(1046, // POWER_FACTOR: 905
								new int[] { 0x0000, 0x235A }) //
						.withRegisters(1100, // MAX_CHARGING_CURRENT
								new int[] { 0x0000, 0x19A7 }) //
						.withRegisters(1110, // MAX_SUPPORTED_CURRENT
								new int[] { 0x0000, 0x7D00 }) //
						.withRegisters(1500, // RFID - TODO
								new int[] { 0x0000, 0x0000 }) //
						.withRegisters(1502, // ENERGY_SESSION
								new int[] { 0x0000, 0xFF14 }) //
						.withRegisters(1550, // PHASE_SWITCH_SOURCE
								new int[] { 0x0000, 0x0003 }) //
						.withRegisters(1552, // PHASE_SWITCH_STATE
								new int[] { 0x0000, 0x0001 }) //
						.withRegisters(1600, // FAILSAFE_CURRENT_SETTING - TODO
								new int[] { 0x0000, 0x0000 }) //
						.withRegisters(1602, // FAILSAFE_TIMEOUT_SETTING - TODO
								new int[] { 0x0000, 0x0000 }));
	}

	/**
	 * Use the given {@link TestCase} to test all expected Channels from
	 * {@link KebaModbus}.
	 * 
	 * @param tc The {@link TestCase}
	 * @throws Exception on error
	 */
	public static void testKebaModbusChannels(TestCase tc) throws Exception {
		tc //
				.output(ModbusComponent.ChannelId.MODBUS_COMMUNICATION_FAILED, false) //

				.output(KebaModbus.ChannelId.ERROR_CODE, 0) //
				.output(KebaModbus.ChannelId.SERIAL_NUMBER, 0) //
				.output(KebaModbus.ChannelId.FAILSAFE_CURRENT_SETTING, 0) //
				.output(KebaModbus.ChannelId.FAILSAFE_TIMEOUT_SETTING, 0) //
				.output(KebaModbus.ChannelId.DEVICE_SOFTWARE_OUTDATED, false) //
				.output(KebaModbus.ChannelId.FIRMWARE, "1.2.3") //
				.output(KebaModbus.ChannelId.FIRMWARE_MAJOR, 1) //
				.output(KebaModbus.ChannelId.FIRMWARE_MINOR, 2) //
				.output(KebaModbus.ChannelId.FIRMWARE_PATCH, 3) //
				.output(KebaModbus.ChannelId.PTAF_PRODUCT_FAMILY, ProductTypeAndFeatures.ProductFamily.KC_P40) //
				.output(KebaModbus.ChannelId.PTAF_DEVICE_CURRENT, ProductTypeAndFeatures.DeviceCurrent.A32_32) //
				.output(KebaModbus.ChannelId.PTAF_CONNECTOR, ProductTypeAndFeatures.Connector.CABLE) //
				.output(KebaModbus.ChannelId.PTAF_PHASES, ProductTypeAndFeatures.Phases.THREE_PHASE) //
				.output(KebaModbus.ChannelId.PTAF_METERING, ProductTypeAndFeatures.Metering.LEGAL) //
				.output(KebaModbus.ChannelId.PTAF_RFID, ProductTypeAndFeatures.Rfid.WITH_RFID) //
				.output(KebaModbus.ChannelId.PTAF_BUTTON, ProductTypeAndFeatures.Button.WITH_BUTTON) //
		;
	}
}
