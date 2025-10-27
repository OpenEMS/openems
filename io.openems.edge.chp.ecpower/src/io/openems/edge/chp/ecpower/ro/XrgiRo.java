package io.openems.edge.chp.ecpower.ro;

import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.ModbusComponent;

public interface XrgiRo extends ModbusComponent, OpenemsComponent, ElectricityMeter {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		// ACTIVE_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT)),
		ERROR(Doc.of(OpenemsType.BOOLEAN).persistencePriority(PersistencePriority.HIGH)),
		OPERATING(Doc.of(OpenemsType.BOOLEAN).persistencePriority(PersistencePriority.HIGH)),
		CHP_READY_FOR_OPERATION(Doc.of(OpenemsType.BOOLEAN).persistencePriority(PersistencePriority.HIGH)),
		CHP_NOT_READY_FOR_OPERATION(Doc.of(OpenemsType.BOOLEAN).persistencePriority(PersistencePriority.HIGH)),
		MULTIPLE_STORAGE_UNITS_PRESENT(Doc.of(OpenemsType.BOOLEAN).persistencePriority(PersistencePriority.HIGH)),
		STORAGE_SENSOR_ORDER_DETECTED(Doc.of(OpenemsType.BOOLEAN).persistencePriority(PersistencePriority.HIGH)),

		// Temperatur (°C x100)
		STORAGE_TEMPERATURE_TOP(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)
				.persistencePriority(PersistencePriority.HIGH)),
		STORAGE_TEMPERATURE_BOTTOM(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)
				.persistencePriority(PersistencePriority.HIGH)),
		FLOW_MASTER_FLOW_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)
				.persistencePriority(PersistencePriority.HIGH)),
		FLOW_MASTER_RETURN_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)
				.persistencePriority(PersistencePriority.HIGH)),
		CHP_NETWORK_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)
				.persistencePriority(PersistencePriority.HIGH)),
		OUTDOOR_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)
				.persistencePriority(PersistencePriority.HIGH)),

		// Leistung / Produktion
		AKTUELLE_ELEKTRO_PRODUKTION(
				Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT).persistencePriority(PersistencePriority.HIGH)),
		CURRENT_HEAT_OUTPUT(
				Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT).persistencePriority(PersistencePriority.HIGH)),
		TOTAL_ACTIVE_ENERGY(
				Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS).persistencePriority(PersistencePriority.HIGH)),
		TOTAL_HEAT_ENERGY(
				Doc.of(OpenemsType.INTEGER).unit(Unit.WATT_HOURS).persistencePriority(PersistencePriority.HIGH)),
		ACTIVE_ENERGY_15MIN(
				Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS).persistencePriority(PersistencePriority.HIGH)), // kWh
		HEAT_ENERGY_15MIN(
				Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS).persistencePriority(PersistencePriority.HIGH)), // kWh

		// Gas
		TOTAL_GAS_CONSUMPTION(
				Doc.of(OpenemsType.INTEGER).unit(Unit.WATT_HOURS).persistencePriority(PersistencePriority.HIGH)), // kWh

		// Betriebsstunden etc.
		TOTAL_OPERATING_HOURS(
				Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR).persistencePriority(PersistencePriority.HIGH)),
		REMAINING_HOURS_UNTIL_MAINTENANCE(
				Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR).persistencePriority(PersistencePriority.HIGH)),
		LAST_ERROR_CODE(Doc.of(OpenemsType.INTEGER).persistencePriority(PersistencePriority.HIGH)),
		TOTAL_GENERATOR_STARTS(Doc.of(OpenemsType.INTEGER).persistencePriority(PersistencePriority.HIGH)),

		// Temperaturen QW/FM (°C x100)
		MIXER_TMV_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)
				.persistencePriority(PersistencePriority.HIGH)),
		MIXER_TMK_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)
				.persistencePriority(PersistencePriority.HIGH)),
		MIXER_TLV_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)
				.persistencePriority(PersistencePriority.HIGH)),
		MIXER_TLK_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)
				.persistencePriority(PersistencePriority.HIGH)),
		MIXER_RETURN_TEMPERATUR(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)
				.persistencePriority(PersistencePriority.HIGH)),
		OPERATIONAL_SETPOINT_TMV(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)
				.persistencePriority(PersistencePriority.HIGH)),
		FLOW_MASTER_BYPASS_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)
				.persistencePriority(PersistencePriority.HIGH)),
		FLOW_MASTER_SUPPLY_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)
				.persistencePriority(PersistencePriority.HIGH)),
		FLOW_MASTER_SETPOINT_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)
				.persistencePriority(PersistencePriority.HIGH)),
		FLOW_MASTER_OPERATIONAL_SETPOINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)
				.persistencePriority(PersistencePriority.HIGH)),

		// Prozent (%)
		MIXER_ENGINE_LOOP_PUMP(
				Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT).persistencePriority(PersistencePriority.HIGH)),
		MIXER_SECONDARY_PUMP(
				Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT).persistencePriority(PersistencePriority.HIGH)),
		FLOW_MASTER_PUMP_POWER(
				Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT).persistencePriority(PersistencePriority.HIGH)),
		MIXER_VALVE_POSITION(
				Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT).persistencePriority(PersistencePriority.HIGH)),
		FLOW_MASTER_VALVE_POSITION(
				Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT).persistencePriority(PersistencePriority.HIGH)),

		// Wärmemotor
		CURRENT_ENGINE_HEAT_OUTPUT(
				Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT).persistencePriority(PersistencePriority.HIGH)), // kW
																												// x100
		HEAT_TRANSFER_VALUE(Doc.of(OpenemsType.INTEGER).persistencePriority(PersistencePriority.HIGH)), // kW/K x10
		LAYER_SEPARATION_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)
				.persistencePriority(PersistencePriority.HIGH)),

		// Leistung/Leistungsgrenzen
		POWER_UNIT_REQUESTED_POWER(
				Doc.of(OpenemsType.INTEGER).unit(Unit.WATT).persistencePriority(PersistencePriority.HIGH)),
		POWER_UNIT_POWER_LIMIT(
				Doc.of(OpenemsType.INTEGER).unit(Unit.WATT).persistencePriority(PersistencePriority.HIGH)),
		POWER_UNIT_TARGET_POWER(
				Doc.of(OpenemsType.INTEGER).unit(Unit.WATT).persistencePriority(PersistencePriority.HIGH)),
		POWER_UNIT_ENGINE_VALVE_POSITION(Doc.of(OpenemsType.INTEGER).persistencePriority(PersistencePriority.HIGH)),
		POWER_UNIT_MAP_PRESSURE(
				Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIBAR).persistencePriority(PersistencePriority.HIGH)),
		POWER_UNIT_FUEL_VALVE_POSITION(Doc.of(OpenemsType.INTEGER).persistencePriority(PersistencePriority.HIGH)),
		POWER_UNIT_IGNITION_ANGLE(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).persistencePriority(PersistencePriority.HIGH)),
		POWER_UNIT_ENGINE_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)
				.persistencePriority(PersistencePriority.HIGH)),
		POWER_UNIT_RPM(Doc.of(OpenemsType.INTEGER).persistencePriority(PersistencePriority.HIGH)),

		// Spannung, Frequenz
		L1_L2_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).persistencePriority(PersistencePriority.HIGH)),
		L2_L3_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).persistencePriority(PersistencePriority.HIGH)),
		L3_L1_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).persistencePriority(PersistencePriority.HIGH)),
		// FREQUENCY(Doc.of(OpenemsType.INTEGER).unit(Unit.HERTZ).persistencePriority(PersistencePriority.HIGH)),
		MESSAGE_STATUS(Doc.of(OpenemsType.INTEGER).persistencePriority(PersistencePriority.HIGH)),
		VPP_ENABLE(Doc.of(OpenemsType.INTEGER).persistencePriority(PersistencePriority.HIGH)),
		CHP_POWER_CONTROL(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT).persistencePriority(PersistencePriority.HIGH)),

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

	// Temperature on buffer tank top
	public default Value<Integer> getBufferTankTemperature() {
		return this.getBufferTankTemperatureChannel().value();
	}

	public default IntegerReadChannel getBufferTankTemperatureChannel() {
		return this.channel(ChannelId.STORAGE_TEMPERATURE_BOTTOM);

	}
	
	// CHP ready for operation? it is not if:
	// currently operating
	// CHP_NOT_READY_FOR_OPERATION is active (device is locked for...reasons)
	public default Value<Boolean> getReadyForOperation() {
		return this.getReadyForOperationChannel().value();
	}

	public default BooleanReadChannel getReadyForOperationChannel() {
		return this.channel(ChannelId.CHP_READY_FOR_OPERATION);

	}

	//
	public default Value<Boolean> getNotReadyForOperation() {
		return this.getNotReadyForOperationChannel().value();
	}

	public default BooleanReadChannel getNotReadyForOperationChannel() {
		return this.channel(ChannelId.CHP_NOT_READY_FOR_OPERATION);

	}	
	
	//
	public default Value<Boolean> getIsOperating() {
		return this.getIsOperatingChannel().value();
	}

	public default BooleanReadChannel getIsOperatingChannel() {
		return this.channel(ChannelId.OPERATING);

	}	
	
		
}
