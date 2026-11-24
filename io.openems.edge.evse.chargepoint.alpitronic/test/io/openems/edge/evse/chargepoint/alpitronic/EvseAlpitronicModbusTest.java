package io.openems.edge.evse.chargepoint.alpitronic;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import io.openems.common.channel.Level;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.chargepoint.alpitronic.common.Alpitronic;
import io.openems.edge.evse.chargepoint.alpitronic.enums.AvailableState;
import io.openems.edge.evse.chargepoint.alpitronic.enums.Connector;
import io.openems.edge.evse.chargepoint.alpitronic.enums.SelectedConnector;
import io.openems.edge.meter.api.ElectricityMeter;

class EvseAlpitronicModbusTest {

	@Test
	void testStrictModbusChannelValues() throws Exception {
		var component = new EvseAlpitronicImpl();
		final var tc = new TestCase("Read expected EVSE values from Modbus") //
				.activateStrictMode() //
				// EvseAlpitronic
				.output(EvseAlpitronic.ChannelId.AVAILABLE_STATE, AvailableState.CHARGING) //
				.output(EvseAlpitronic.ChannelId.TOTAL_CHARGED_ENERGY, null) //
				.output(EvseAlpitronic.ChannelId.MAX_CHARGING_POWER_AC, null) //
				// Alpitronic - Modbus reads
				.output(Alpitronic.ChannelId.RAW_CHARGE_POWER, 50_000) //
				.output(Alpitronic.ChannelId.CHARGED_TIME, 1_800) //
				.output(Alpitronic.ChannelId.CHARGED_ENERGY, 25.0) //
				.output(Alpitronic.ChannelId.EV_SOC, 65) //
				.output(Alpitronic.ChannelId.EV_MAX_CHARGING_POWER, 150_000) //
				.output(Alpitronic.ChannelId.EV_MIN_CHARGING_POWER, 5_000) //
				.output(Alpitronic.ChannelId.CONNECTOR_TYPE, SelectedConnector.CCS_DC) //
				.output(Alpitronic.ChannelId.VAR_REACTIVE_MAX, 50_000) //
				.output(Alpitronic.ChannelId.VAR_REACTIVE_MIN, -50_000) //
				.output(Alpitronic.ChannelId.RAW_CHARGE_POWER_SET, 50_000) //
				// Alpitronic - write channels (no pending write)
				.output(Alpitronic.ChannelId.APPLY_CHARGE_POWER_LIMIT, null) //
				.output(Alpitronic.ChannelId.SETPOINT_REACTIVE_POWER, null) //
				// ElectricityMeter - Modbus reads
				.output(ElectricityMeter.ChannelId.VOLTAGE, 400) //
				.output(ElectricityMeter.ChannelId.CURRENT, 125) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 50_000) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 16_666) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 16_666) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 16_666) //
				.output(ElectricityMeter.ChannelId.REACTIVE_POWER, null) //
				.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, null) //
				.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, null) //
				.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, null) //
				.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 400) //
				.output(ElectricityMeter.ChannelId.VOLTAGE_L2, 400) //
				.output(ElectricityMeter.ChannelId.VOLTAGE_L3, 400) //
				.output(ElectricityMeter.ChannelId.CURRENT_L1, 41) //
				.output(ElectricityMeter.ChannelId.CURRENT_L2, 41) //
				.output(ElectricityMeter.ChannelId.CURRENT_L3, 41) //
				.output(ElectricityMeter.ChannelId.FREQUENCY, null) //
				.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, null) //
				.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1, null) //
				.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2, null) //
				.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3, null) //
				.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, null) //
				.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1, null) //
				.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2, null) //
				.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3, null) //
				// EvseChargePoint
				.output(EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, true) //
				// ModbusComponent
				.output(ModbusComponent.ChannelId.MODBUS_COMMUNICATION_FAILED, false) //
				// OpenemsComponent
				.output(OpenemsComponent.ChannelId.STATE, Level.OK); //
		prepareAlpitronic(component) //
				.next(new TestCase()) //
				.next(new TestCase()) //
				.next(new TestCase()) //
				.next(new TestCase()) //
				.next(new TestCase()) //
				.next(new TestCase()) //
				.next(new TestCase()) //
				.next(tc) //
				.deactivate();

		assertFalse(component.isReadOnly());
	}

	private static ComponentTest prepareAlpitronic(EvseAlpitronicImpl alpitronicImpl) throws Exception {
		return new ComponentTest(alpitronicImpl) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0") //
						// Input Registers (FC4) for station-level software version
						.withInputRegisters(46, new int[] { 0x0001, 0x0008, 0x0000 }) // SOFTWARE_VERSION: 1.8.0
						// Holding Registers (FC3)
						.withRegisters(100, new int[] { 0x0000, 0xC350 }) // RAW_CHARGE_POWER_SET: 50000 W
						// Input Registers (FC4) for SLOT_0 (100-116)
						.withInputRegisters(100, new int[] { 0x0003 }) // AVAILABLE_STATE: CHARGING
						.withInputRegisters(101, new int[] { 0x0000, 0x9C40 }) // VOLTAGE: 400.0 V
						.withInputRegisters(103, new int[] { 0x30D4 }) // CURRENT: 125.0 A
						.withInputRegisters(104, new int[] { 0x0000, 0xC350 }) // RAW_CHARGE_POWER: 50000 W
						.withInputRegisters(106, new int[] { 0x0708 }) // CHARGED_TIME: 1800 s
						.withInputRegisters(107, new int[] { 0x09C4 }) // CHARGED_ENERGY: 25 kWh
						.withInputRegisters(108, new int[] { 0x1964 }) // EV_SOC: 65 %
						.withInputRegisters(109, new int[] { 0x0001 }) // CONNECTOR_TYPE: CCS_DC on v1.8-v2.4
						.withInputRegisters(110, new int[] { 0x0002, 0x49F0 }) // EV_MAX_CHARGING_POWER: 150000 W
						.withInputRegisters(112, new int[] { 0x0000, 0x1388 }) // EV_MIN_CHARGING_POWER: 5000 W
						.withInputRegisters(114, new int[] { 0x0000, 0xC350 }) // VAR_REACTIVE_MAX: 50000 VAr
						// VAR_REACTIVE_MIN: 50000 -> INVERT -> -50000 VAr
						.withInputRegisters(116, new int[] { 0x0000, 0xC350 }))
				.activate(MyConfig.create() //
						.setModbusId("modbus0") //
						.setId("evse0") //
						.setModbusUnitId(1) //
						.setReadOnly(false) //
						.setConnector(Connector.SLOT_0) //
						.setMaxHwPower(150_000) //
						.setMinHwPower(5_000) //
						.build());
	}
}
