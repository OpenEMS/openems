package io.openems.edge.chp.ecpower.ro;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.ModbusComponent;


public interface XrgiRo extends ModbusComponent, OpenemsComponent, ElectricityMeter  {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		//ACTIVE_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT)),
		ERROR(Doc.of(OpenemsType.BOOLEAN)),
		OPERATING(Doc.of(OpenemsType.BOOLEAN)),
		CHP_READY_FOR_OPERATION(Doc.of(OpenemsType.BOOLEAN)),
		CHP_NOT_READY_FOR_OPERATION(Doc.of(OpenemsType.BOOLEAN)),
		MULTIPLE_STORAGE_UNITS_PRESENT(Doc.of(OpenemsType.BOOLEAN)),
		STORAGE_SENSOR_ORDER_DETECTED(Doc.of(OpenemsType.BOOLEAN)),

		// Temperatur (°C x100)
		STORAGE_TEMPERATURE_TOP(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
		STORAGE_TEMPERATURE_BOTTOM(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
		FLOW_MASTER_FLOW_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
		FLOW_MASTER_RETURN_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
		CHP_NETWORK_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
		OUTDOOR_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

		// Leistung / Produktion
		AKTUELLE_ELEKTRO_PRODUKTION(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT)), // kW x10
		CURRENT_HEAT_OUTPUT(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)),
		TOTAL_ACTIVE_ENERGY(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)), // kWh
		TOTAL_HEAT_ENERGY(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)), // kWh
		ACTIVE_ENERGY_15MIN(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)), // kWh
		HEAT_ENERGY_15MIN(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)), // kWh

		// Gas
		TOTAL_GAS_CONSUMPTION(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)), // kWh

		// Betriebsstunden etc.
		TOTAL_OPERATING_HOURS(Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR)),
		REMAINING_HOURS_UNTIL_MAINTENANCE(Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR)),
		LAST_ERROR_CODE(Doc.of(OpenemsType.INTEGER)),
		TOTAL_GENERATOR_STARTS(Doc.of(OpenemsType.INTEGER)),

		// Temperaturen QW/FM (°C x100)
		MIXER_TMV_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
		MIXER_TMK_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
		MIXER_TLV_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
		MIXER_TLK_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
		MIXER_RETURN_TEMPERATUR(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
		OPERATIONAL_SETPOINT_TMV(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
		FLOW_MASTER_BYPASS_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
		FLOW_MASTER_SUPPLY_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
		FLOW_MASTER_SETPOINT_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
		FLOW_MASTER_OPERATIONAL_SETPOINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

		// Prozent (%)
		MIXER_ENGINE_LOOP_PUMP(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)),
		MIXER_SECONDARY_PUMP(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)),
		FLOW_MASTER_PUMP_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)),
		MIXER_VALVE_POSITION(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)),
		FLOW_MASTER_VALVE_POSITION(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)),

		// Wärmemotor
		CURRENT_ENGINE_HEAT_OUTPUT(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT)), // kW x100
		HEAT_TRANSFER_VALUE(Doc.of(OpenemsType.INTEGER)), // kW/K x10
		LAYER_SEPARATION_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)),

		// Leistung/Leistungsgrenzen
		POWER_UNIT_REQUESTED_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT)),
		POWER_UNIT_POWER_LIMIT(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT)),
		POWER_UNIT_TARGET_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT)),
		POWER_UNIT_ENGINE_VALVE_POSITION(Doc.of(OpenemsType.INTEGER)),
		POWER_UNIT_MAP_PRESSURE(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIBAR)),
		POWER_UNIT_FUEL_VALVE_POSITION(Doc.of(OpenemsType.INTEGER)),
		POWER_UNIT_IGNITION_ANGLE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)),
		POWER_UNIT_ENGINE_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
		POWER_UNIT_RPM(Doc.of(OpenemsType.INTEGER)),

		// Spannung, Frequenz
		L1_L2_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT)),
		L2_L3_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT)),
		L3_L1_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT)),
		//FREQUENCY(Doc.of(OpenemsType.INTEGER).unit(Unit.HERTZ)),
		ALERT_STATUS(Doc.of(OpenemsType.INTEGER)), 
		VPP_ENABLE(Doc.of(OpenemsType.INTEGER)),
		CHP_POWER_CONTROL(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)),
		

		
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	
}
