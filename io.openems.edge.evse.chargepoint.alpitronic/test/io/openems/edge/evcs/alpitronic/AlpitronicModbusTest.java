package io.openems.edge.evcs.alpitronic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import io.openems.common.channel.Level;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.evcs.api.ChargeMode;
import io.openems.edge.evcs.api.ChargeState;
import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.DeprecatedEvcs;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Phases;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.evse.chargepoint.alpitronic.common.Alpitronic;
import io.openems.edge.evse.chargepoint.alpitronic.enums.AvailableState;
import io.openems.edge.evse.chargepoint.alpitronic.enums.Connector;
import io.openems.edge.evse.chargepoint.alpitronic.enums.SelectedConnector;
import io.openems.edge.meter.api.ElectricityMeter;

class AlpitronicModbusTest {

	@Test
	void testStrictModbusChannelValuesForV25() throws Exception {
		var component = new EvcsAlpitronicImpl();
		final var tc = new TestCase("Read expected EVCS values from Modbus") //
				.activateStrictMode() //
				// OpenemsComponent + ModbusComponent
				.output(OpenemsComponent.ChannelId.STATE, Level.OK) //
				.output(ModbusComponent.ChannelId.MODBUS_COMMUNICATION_FAILED, false) //
				// EvcsAlpitronic station-level channels
				.output(EvcsAlpitronic.ChannelId.UNIX_TIME, null) //
				.output(EvcsAlpitronic.ChannelId.NUM_CONNECTORS, null) //
				.output(EvcsAlpitronic.ChannelId.STATION_STATE, null) //
				.output(EvcsAlpitronic.ChannelId.TOTAL_STATION_POWER, null) //
				.output(EvcsAlpitronic.ChannelId.SERIAL_NUMBER, null) //
				.output(EvcsAlpitronic.ChannelId.CHARGEPOINT_ID, null) //
				.output(EvcsAlpitronic.ChannelId.VID, null) //
				.output(EvcsAlpitronic.ChannelId.ID_TAG, null) //
				.output(EvcsAlpitronic.ChannelId.LOAD_MANAGEMENT_ENABLED, null) //
				.output(EvcsAlpitronic.ChannelId.SOFTWARE_VERSION_MAJOR, 2) //
				.output(EvcsAlpitronic.ChannelId.SOFTWARE_VERSION_MINOR, 5) //
				.output(EvcsAlpitronic.ChannelId.SOFTWARE_VERSION_PATCH, 3) //
				.output(EvcsAlpitronic.ChannelId.TOTAL_CHARGED_ENERGY, 98_305_828_716_544L) //
				.output(EvcsAlpitronic.ChannelId.RAW_STATUS, AvailableState.CHARGING) //
				.output(EvcsAlpitronic.ChannelId.CHARGING_VOLTAGE, 400.0) //
				.output(EvcsAlpitronic.ChannelId.CHARGING_CURRENT, 125.0) //
				.output(EvcsAlpitronic.ChannelId.MAX_CHARGING_POWER_AC, 22_000) //
				// Alpitronic channels
				.output(Alpitronic.ChannelId.RAW_CHARGE_POWER_SET, 50_000) //
				.output(Alpitronic.ChannelId.APPLY_CHARGE_POWER_LIMIT, null) //
				.output(Alpitronic.ChannelId.SETPOINT_REACTIVE_POWER, null) //
				.output(Alpitronic.ChannelId.RAW_CHARGE_POWER, 50_000) //
				.output(Alpitronic.ChannelId.CHARGED_TIME, 1_800) //
				.output(Alpitronic.ChannelId.CHARGED_ENERGY, 25.0) //
				.output(Alpitronic.ChannelId.EV_SOC, 65) //
				.output(Alpitronic.ChannelId.CONNECTOR_TYPE, SelectedConnector.CCS2) //
				.output(Alpitronic.ChannelId.EV_MAX_CHARGING_POWER, 150_000) //
				.output(Alpitronic.ChannelId.EV_MIN_CHARGING_POWER, 5_000) //
				.output(Alpitronic.ChannelId.VAR_REACTIVE_MAX, 50_000) //
				.output(Alpitronic.ChannelId.VAR_REACTIVE_MIN, -50_000) //
				// ElectricityMeter channels
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 50_000) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 16_666) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 16_666) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 16_666) //
				.output(ElectricityMeter.ChannelId.REACTIVE_POWER, null) //
				.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, null) //
				.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, null) //
				.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, null) //
				.output(ElectricityMeter.ChannelId.VOLTAGE, 400_000) //
				.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 400_000) //
				.output(ElectricityMeter.ChannelId.VOLTAGE_L2, 400_000) //
				.output(ElectricityMeter.ChannelId.VOLTAGE_L3, 400_000) //
				.output(ElectricityMeter.ChannelId.CURRENT, 125_000) //
				.output(ElectricityMeter.ChannelId.CURRENT_L1, 41_666) //
				.output(ElectricityMeter.ChannelId.CURRENT_L2, 41_666) //
				.output(ElectricityMeter.ChannelId.CURRENT_L3, 41_666) //
				.output(ElectricityMeter.ChannelId.FREQUENCY, null) //
				.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, null) //
				.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1, null) //
				.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2, null) //
				.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3, null) //
				.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, null) //
				.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1, null) //
				.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2, null) //
				.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3, null) //
				// Evcs + ManagedEvcs + DeprecatedEvcs
				.output(Evcs.ChannelId.STATUS, Status.CHARGING) //
				.output(Evcs.ChannelId.CHARGING_TYPE, ChargingType.UNDEFINED) //
				.output(Evcs.ChannelId.PHASES, Phases.THREE_PHASE) //
				.output(Evcs.ChannelId.FIXED_MINIMUM_HARDWARE_POWER, 5_000) //
				.output(Evcs.ChannelId.FIXED_MAXIMUM_HARDWARE_POWER, 150_000) //
				.output(Evcs.ChannelId.MINIMUM_HARDWARE_POWER, 5_000) //
				.output(Evcs.ChannelId.MAXIMUM_HARDWARE_POWER, 150_000) //
				.output(Evcs.ChannelId.MAXIMUM_POWER, null) //
				.output(Evcs.ChannelId.MINIMUM_POWER, null) //
				.output(Evcs.ChannelId.ENERGY_SESSION, 25_000) //
				.output(Evcs.ChannelId.CHARGINGSTATION_COMMUNICATION_FAILED, false) //
				.output(ManagedEvcs.ChannelId.POWER_PRECISION, 1.0) //
				.output(ManagedEvcs.ChannelId.SET_CHARGE_POWER_LIMIT, null) //
				.output(ManagedEvcs.ChannelId.SET_CHARGE_POWER_LIMIT_WITH_FILTER, null) //
				.output(ManagedEvcs.ChannelId.IS_CLUSTERED, null) //
				.output(ManagedEvcs.ChannelId.CHARGE_MODE, ChargeMode.FORCE_CHARGE) //
				.output(ManagedEvcs.ChannelId.SET_DISPLAY_TEXT, null) //
				.output(ManagedEvcs.ChannelId.SET_CHARGE_POWER_REQUEST, null) //
				.output(ManagedEvcs.ChannelId.SET_ENERGY_LIMIT, null) //
				.output(ManagedEvcs.ChannelId.CHARGE_STATE, ChargeState.CHARGING) //
				.output(DeprecatedEvcs.ChannelId.CHARGE_POWER, 50_000); //
		prepareAlpitronic(component) //
				.next(new TestCase()) //
				.next(new TestCase()) //
				.next(new TestCase()) //
				.next(new TestCase()) //
				.next(new TestCase()) //
				.next(tc) //
				.deactivate();
	}

	@Test
	void testStrictModbusChannelValuesForV18() throws Exception {
		var component = new EvcsAlpitronicImpl();
		prepareAlpitronicV18(component) //
				.next(new TestCase()) //
				.next(new TestCase()) //
				.next(new TestCase()) //
				.next(new TestCase()) //
				.next(new TestCase()) //
				.next(new TestCase());

		assertEquals(1, component.channel(EvcsAlpitronic.ChannelId.SOFTWARE_VERSION_MAJOR).value().get());
		assertEquals(8, component.channel(EvcsAlpitronic.ChannelId.SOFTWARE_VERSION_MINOR).value().get());
		assertEquals(0, component.channel(EvcsAlpitronic.ChannelId.SOFTWARE_VERSION_PATCH).value().get());
		assertNull(component.channel(EvcsAlpitronic.ChannelId.TOTAL_CHARGED_ENERGY).value().get());
		assertNull(component.channel(EvcsAlpitronic.ChannelId.MAX_CHARGING_POWER_AC).value().get());
		assertEquals(SelectedConnector.CCS_DC, component.channel(Alpitronic.ChannelId.CONNECTOR_TYPE).value().asEnum());
		assertEquals(50_000, component.channel(Alpitronic.ChannelId.VAR_REACTIVE_MAX).value().get());
		assertEquals(-50_000, component.channel(Alpitronic.ChannelId.VAR_REACTIVE_MIN).value().get());
	}

	@Test
	void testFirmwareVersionReadRetriesOncePerFailedPoll() throws Exception {
		var component = new EvcsAlpitronicImpl();
		var bridge = prepareAlpitronicWithoutFirmwareVersion();
		var test = new ComponentTest(component) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", bridge) //
				.activate(MyConfig.create() //
						.setModbusId("modbus0") //
						.setId("evcs0") //
						.setModbusUnitId(1) //
						.setConnector(Connector.SLOT_0) //
						.setMaxHwPower(150_000) //
						.setMinHwPower(5_000) //
						.build()); //

		test.next(new TestCase()) //
				.next(new TestCase());
		bridge.withInputRegisters(46, 0x0001, 0x0008, 0x0000);
		test.next(new TestCase()) //
				.next(new TestCase()) //
				.next(new TestCase());

		assertEquals(1, component.channel(EvcsAlpitronic.ChannelId.SOFTWARE_VERSION_MAJOR).value().get());
		assertEquals(8, component.channel(EvcsAlpitronic.ChannelId.SOFTWARE_VERSION_MINOR).value().get());
		assertEquals(0, component.channel(EvcsAlpitronic.ChannelId.SOFTWARE_VERSION_PATCH).value().get());
		assertNull(component.channel(EvcsAlpitronic.ChannelId.MAX_CHARGING_POWER_AC).value().get());
	}

	public static ComponentTest prepareAlpitronic(EvcsAlpitronicImpl alpitronicImpl) throws Exception {
		return new ComponentTest(alpitronicImpl) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0") //
						// Holding Registers (FC3)
						.withRegisters(100, new int[] { 0x0000, 0xC350 }) // RAW_CHARGE_POWER_SET: 50000 W
						// Station-Level Input Registers (0-52)
						.withInputRegisters(0, new int[] { 0x655F, 0x2400 }) // UNIX_TIME: 1700000000
						.withInputRegisters(2, new int[] { 0x0004 }) // NUM_CONNECTORS: 4
						.withInputRegisters(3, new int[] { 0x0000 }) // STATION_STATE: available
						.withInputRegisters(4, new int[] { 0x0000, 0xC350 }) // TOTAL_STATION_POWER: 50000 W
						.withInputRegisters(6,
								new int[] { 0x4859, 0x4331, 0x3233, 0x3435, 0x3637, 0x3800, 0x0000, 0x0000, 0x0000,
										0x0000, 0x0000, 0x0000 }) // SERIAL_NUMBER
						.withInputRegisters(18, new int[] { 0x0001 }) // LOAD_MANAGEMENT_ENABLED: true
						.withInputRegisters(30,
								new int[] { 0x414C, 0x5049, 0x5452, 0x4F4E, 0x4943, 0x5F48, 0x5943, 0x5F30, 0x3031,
										0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000 }) // CHARGEPOINT_ID
						.withInputRegisters(46, new int[] { 0x0002, 0x0005, 0x0003 }) // SOFTWARE_VERSION: 2.5.3
						.withInputRegisters(49, new int[] { 0x0000, 0xC350 }) // VAR_REACTIVE_MAX (station): 50000
						// VAR_REACTIVE_MIN (station): 50000 -> inverted to -50000
						.withInputRegisters(51, new int[] { 0x0000, 0xC350 })
						// Connector-Level Input Registers for SLOT_0 (100-137)
						.withInputRegisters(100, new int[] { 0x0003 }) // RAW_STATUS: CHARGING
						.withInputRegisters(101, new int[] { 0x0000, 0x9C40 }) // CHARGING_VOLTAGE: 400.0 V
						.withInputRegisters(103, new int[] { 0x30D4 }) // CHARGING_CURRENT: 125.0 A
						.withInputRegisters(104, new int[] { 0x0000, 0xC350 }) // RAW_CHARGE_POWER: 50000 W
						.withInputRegisters(106, new int[] { 0x0708 }) // CHARGED_TIME: 1800 s
						.withInputRegisters(107, new int[] { 0x09C4 }) // CHARGED_ENERGY: 25 kWh
						.withInputRegisters(108, new int[] { 0x1964 }) // EV_SOC: 65 %
						.withInputRegisters(109, new int[] { 0x0001 }) // CONNECTOR_TYPE: CCS2 on v2.5+
						.withInputRegisters(110, new int[] { 0x0002, 0x49F0 }) // EV_MAX_CHARGING_POWER: 150000 W
						.withInputRegisters(112, new int[] { 0x0000, 0x1388 }) // EV_MIN_CHARGING_POWER: 5000 W
						.withInputRegisters(118, new int[] { 0x0000, 0x0000, 0x0000, 0x0000 }) // VID
						.withInputRegisters(122,
								new int[] { 0x5246, 0x4944, 0x5F31, 0x3233, 0x3435, 0x0000, 0x0000, 0x0000, 0x0000,
										0x0000 }) // ID_TAG
						// TOTAL_CHARGED_ENERGY (64 bit)
						.withInputRegisters(132, new int[] { 0x0000, 0x5968, 0x9C00, 0x0000 })
						.withInputRegisters(136, new int[] { 0x0000, 0x55F0 }) // MAX_CHARGING_POWER_AC: 22000 W
						// VAR_REACTIVE_MAX/MIN at connector offset+49/51 for v2.5 (SLOT_0: 149/151)
						.withInputRegisters(149, new int[] { 0x0000, 0xC350 }) // VAR_REACTIVE_MAX: 50000
						// VAR_REACTIVE_MIN: 50000 -> inverted to -50000
						.withInputRegisters(151, new int[] { 0x0000, 0xC350 }))
				.activate(MyConfig.create() //
						.setModbusId("modbus0") //
						.setId("evcs0") //
						.setModbusUnitId(1) //
						.setConnector(Connector.SLOT_0) //
						.setMaxHwPower(150_000) //
						.setMinHwPower(5_000) //
						.build());
	}

	private static DummyModbusBridge prepareAlpitronicWithoutFirmwareVersion() {
		return new DummyModbusBridge("modbus0") //
				// Holding Registers (FC3)
				.withRegisters(100, 0x0000, 0xC350) // RAW_CHARGE_POWER_SET: 50000 W
				// Station-Level Input Registers (0-52)
				.withInputRegisters(0, 0x655F, 0x2400) // UNIX_TIME: 1700000000
				.withInputRegisters(2, 0x0004) // NUM_CONNECTORS: 4
				.withInputRegisters(3, 0x0000) // STATION_STATE: available
				.withInputRegisters(4, 0x0000, 0xC350) // TOTAL_STATION_POWER: 50000 W
				.withInputRegisters(6, 0x4859, 0x4331, 0x3233, 0x3435, 0x3637, 0x3800, 0x0000, 0x0000, 0x0000, 0x0000,
						0x0000, 0x0000) // SERIAL_NUMBER
				.withInputRegisters(18, 0x0001) // LOAD_MANAGEMENT_ENABLED: true
				.withInputRegisters(30, 0x414C, 0x5049, 0x5452, 0x4F4E, 0x4943, 0x5F48, 0x5943, 0x5F30, 0x3031, 0x0000,
						0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000) // CHARGEPOINT_ID
				.withInputRegisters(49, 0x0000, 0xC350) // VAR_REACTIVE_MAX (station): 50000
				.withInputRegisters(51, 0x0000, 0xC350) // VAR_REACTIVE_MIN (station): 50000 -> inverted to -50000
				// Connector-Level Input Registers for SLOT_0 (100-137)
				.withInputRegisters(100, 0x0003) // RAW_STATUS: CHARGING
				.withInputRegisters(101, 0x0000, 0x9C40) // CHARGING_VOLTAGE: 400.0 V
				.withInputRegisters(103, 0x30D4) // CHARGING_CURRENT: 125.0 A
				.withInputRegisters(104, 0x0000, 0xC350) // RAW_CHARGE_POWER: 50000 W
				.withInputRegisters(106, 0x0708) // CHARGED_TIME: 1800 s
				.withInputRegisters(107, 0x09C4) // CHARGED_ENERGY: 25 kWh
				.withInputRegisters(108, 0x1964) // EV_SOC: 65 %
				.withInputRegisters(109, 0x0001) // CONNECTOR_TYPE: CCS2 on v2.5+
				.withInputRegisters(110, 0x0002, 0x49F0) // EV_MAX_CHARGING_POWER: 150000 W
				.withInputRegisters(112, 0x0000, 0x1388) // EV_MIN_CHARGING_POWER: 5000 W
				.withInputRegisters(118, 0x0000, 0x0000, 0x0000, 0x0000) // VID
				.withInputRegisters(122, 0x5246, 0x4944, 0x5F31, 0x3233, 0x3435, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000) // ID_TAG
				.withInputRegisters(132, 0x0000, 0x5968, 0x9C00, 0x0000) // TOTAL_CHARGED_ENERGY (64 bit)
				.withInputRegisters(136, 0x0000, 0x55F0) // MAX_CHARGING_POWER_AC: 22000 W
				// VAR_REACTIVE_MAX/MIN at connector offset+49/51 for v2.5 (SLOT_0: 149/151)
				.withInputRegisters(49, 0x0000, 0xC350) // VAR_REACTIVE_MAX: 50000
				.withInputRegisters(51, 0x0000, 0xC350); // VAR_REACTIVE_MIN: 50000 -> inverted to -50000
	}

	public static ComponentTest prepareAlpitronicV18(EvcsAlpitronicImpl alpitronicImpl) throws Exception {
		return new ComponentTest(alpitronicImpl) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0") //
						.withRegisters(100, new int[] { 0x0000, 0xC350 }) // RAW_CHARGE_POWER_SET: 50000 W
						.withInputRegisters(46, new int[] { 0x0001, 0x0008, 0x0000 }) // SOFTWARE_VERSION: 1.8.0
						.withInputRegisters(100, new int[] { 0x0003 }) // RAW_STATUS: CHARGING
						.withInputRegisters(101, new int[] { 0x0000, 0x9C40 }) // CHARGING_VOLTAGE: 400.0 V
						.withInputRegisters(103, new int[] { 0x30D4 }) // CHARGING_CURRENT: 125.0 A
						.withInputRegisters(104, new int[] { 0x0000, 0xC350 }) // RAW_CHARGE_POWER: 50000 W
						.withInputRegisters(106, new int[] { 0x0708 }) // CHARGED_TIME: 1800 s
						.withInputRegisters(107, new int[] { 0x09C4 }) // CHARGED_ENERGY: 25 kWh
						.withInputRegisters(108, new int[] { 0x1964 }) // EV_SOC: 65 %
						.withInputRegisters(109, new int[] { 0x0001 }) // CONNECTOR_TYPE: CCS_DC on v1.8-v2.4
						.withInputRegisters(110, new int[] { 0x0002, 0x49F0 }) // EV_MAX_CHARGING_POWER: 150000 W
						.withInputRegisters(112, new int[] { 0x0000, 0x1388 }) // EV_MIN_CHARGING_POWER: 5000 W
						.withInputRegisters(114, new int[] { 0x0000, 0xC350 }) // VAR_REACTIVE_MAX: 50000
						// VAR_REACTIVE_MIN: 50000 -> inverted to -50000
						.withInputRegisters(116, new int[] { 0x0000, 0xC350 }))
				.activate(MyConfig.create() //
						.setModbusId("modbus0") //
						.setId("evcs0") //
						.setModbusUnitId(1) //
						.setConnector(Connector.SLOT_0) //
						.setMaxHwPower(150_000) //
						.setMinHwPower(5_000) //
						.build());
	}
}
