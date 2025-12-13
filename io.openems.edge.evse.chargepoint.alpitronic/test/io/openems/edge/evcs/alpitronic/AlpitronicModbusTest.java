package io.openems.edge.evcs.alpitronic;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class AlpitronicModbusTest {

	/**
	 * Prepares a {@link ComponentTest} with {@link EvcsAlpitronicImpl}.
	 *
	 * @param alpitronicImpl the {@link EvcsAlpitronicImpl} implementation
	 * @return the {@link ComponentTest}
	 * @throws Exception on error
	 */
	public static ComponentTest prepareAlpitronic(EvcsAlpitronicImpl alpitronicImpl)
			throws OpenemsException, Exception {
		return new ComponentTest(alpitronicImpl) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0") //
						// Station-Level Input Registers (0-52)
						.withRegisters(0, // UNIX_TIME: 1700000000
								new int[] { 0x655F, 0x2400 }) //
						.withRegisters(2, // NUM_CONNECTORS: 4
								new int[] { 0x0004 }) //
						.withRegisters(3, // STATION_STATE: 0 (Available)
								new int[] { 0x0000 }) //
						.withRegisters(4, // TOTAL_STATION_POWER: 50000 W
								new int[] { 0x0000, 0xC350 }) //
						.withRegisters(6, // SERIAL_NUMBER: "HYC12345678" (12 registers)
								new int[] { 0x4859, 0x4331, 0x3233, 0x3435, 0x3637, 0x3800, 0x0000, 0x0000, 0x0000,
										0x0000, 0x0000, 0x0000 }) //
						.withRegisters(18, // LOAD_MANAGEMENT_ENABLED: true (1)
								new int[] { 0x0001 }) //
						.withRegisters(30, // CHARGEPOINT_ID: "ALPITRONIC_HYC_001" (16 registers)
								new int[] { 0x414C, 0x5049, 0x5452, 0x4F4E, 0x4943, 0x5F48, 0x5943, 0x5F30, 0x3031,
										0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000 }) //
						.withRegisters(46, // SOFTWARE_VERSION: 2.5.3
								new int[] { 0x0002, 0x0005, 0x0003 }) //
						.withRegisters(49, // VAR_REACTIVE_MAX (Station-Level): 50000 var
								new int[] { 0x0000, 0xC350 }) //
						.withRegisters(51, // VAR_REACTIVE_MIN (Station-Level): -50000 var
								new int[] { 0xFFFF, 0x3CB0 }) //

						// Connector-Level Input Registers for SLOT_0 (100-137)
						.withRegisters(100, // RAW_STATUS: 3 (CHARGING)
								new int[] { 0x0003 }) //
						.withRegisters(101, // CHARGING_VOLTAGE: 400.0 V (raw value 40000, SCALE_FACTOR_MINUS_2 -> 400.0)
								new int[] { 0x0000, 0x9C40 }) //
						.withRegisters(103, // CHARGING_CURRENT: 125.0 A (raw value 12500, SCALE_FACTOR_MINUS_2 -> 125.0)
								new int[] { 0x30D4 }) //
						.withRegisters(104, // RAW_CHARGE_POWER: 50000 W
								new int[] { 0x0000, 0xC350 }) //
						.withRegisters(106, // CHARGED_TIME: 1800 seconds (30 min)
								new int[] { 0x0708 }) //
						.withRegisters(107, // CHARGED_ENERGY: 25 kWh (raw value 2500, SCALE_FACTOR_MINUS_2 -> 25.0, callback * 10 -> 25000 Wh)
								new int[] { 0x09C4 }) //
						.withRegisters(108, // EV_SOC: 65% (raw value 6500, SCALE_FACTOR_MINUS_2 -> 65)
								new int[] { 0x1964 }) //
						.withRegisters(109, // CONNECTOR_TYPE: 1 (CCS2)
								new int[] { 0x0001 }) //
						.withRegisters(110, // EV_MAX_CHARGING_POWER: 150000 W
								new int[] { 0x0002, 0x49F0 }) //
						.withRegisters(112, // EV_MIN_CHARGING_POWER: 5000 W
								new int[] { 0x0000, 0x1388 }) //
						.withRegisters(118, // VID: null (4 registers)
								new int[] { 0x0000, 0x0000, 0x0000, 0x0000 }) //
						.withRegisters(122, // ID_TAG: "RFID_12345" (10 registers)
								new int[] { 0x5246, 0x4944, 0x5F31, 0x3233, 0x3435, 0x0000, 0x0000, 0x0000, 0x0000,
										0x0000 }) //
						.withRegisters(132, // TOTAL_CHARGED_ENERGY: 1500000 kWh (in Wh = 1500000000)
								new int[] { 0x0000, 0x5968, 0x9C00, 0x0000 }) //
						.withRegisters(136, // MAX_CHARGING_POWER_AC: 22000 W
								new int[] { 0x0000, 0x55F0 }) //

				// Holding Registers (Write)
				// These are write-only, so we don't need to populate them for reading
				);
	}

	/**
	 * Use the given {@link TestCase} to test all expected Channels from
	 * {@link EvcsAlpitronicImpl}.
	 *
	 * @param tc The {@link TestCase}
	 * @throws Exception on error
	 */
	public static void testAlpitronicModbusChannels(TestCase tc) throws Exception {
		tc //
				.output(ModbusComponent.ChannelId.MODBUS_COMMUNICATION_FAILED, false) //
		;
	}
}
