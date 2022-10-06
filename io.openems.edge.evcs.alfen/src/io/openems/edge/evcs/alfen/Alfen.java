package io.openems.edge.evcs.alfen;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;


public interface Alfen extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		METER_STATE(Doc.of(OpenemsType.INTEGER).persistencePriority(PersistencePriority.LOW).text("Meter state")),
		METER_LAST_VALUE_TIMESTAMP(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLISECONDS).persistencePriority(PersistencePriority.MEDIUM).text("Milliseconds since last received measurement")),
		METER_TYPE(Doc.of(OpenemsType.INTEGER).persistencePriority(PersistencePriority.LOW).text("Meter type")),
		VOLTAGE_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT).persistencePriority(PersistencePriority.MEDIUM).text("Voltage Phase L1")),
		VOLTAGE_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT).persistencePriority(PersistencePriority.MEDIUM).text("Voltage Phase L2")),
		VOLTAGE_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT).persistencePriority(PersistencePriority.MEDIUM).text("Voltage Phase L3")),
		VOLTAGE_L1_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT).persistencePriority(PersistencePriority.MEDIUM).text("Voltage Phase L1-L2")),
		VOLTAGE_L2_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT).persistencePriority(PersistencePriority.MEDIUM).text("Voltage Phase L2-L3")),
		VOLTAGE_L3_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT).persistencePriority(PersistencePriority.MEDIUM).text("Voltage Phase L3-L1")),
		CURRENT_N(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE).persistencePriority(PersistencePriority.LOW).text("Current N")),
		CURRENT_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE).persistencePriority(PersistencePriority.LOW).text("Current L1")),
		CURRENT_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE).persistencePriority(PersistencePriority.LOW).text("Current L2")),
		CURRENT_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE).persistencePriority(PersistencePriority.LOW).text("Current L3")),
		CURRENT(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE).persistencePriority(PersistencePriority.LOW).text("Current")),
		POWER_FACTOR_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE).persistencePriority(PersistencePriority.LOW).text("Power factor L1")),
		POWER_FACTOR_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE).persistencePriority(PersistencePriority.LOW).text("Power factor L2")),
		POWER_FACTOR_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE).persistencePriority(PersistencePriority.LOW).text("Power factor L3")),
		POWER_FACTOR_SUM(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE).persistencePriority(PersistencePriority.LOW).text("Power factor sum")),
		FREQUENCY(Doc.of(OpenemsType.FLOAT).unit(Unit.HERTZ).persistencePriority(PersistencePriority.LOW).text("Frequency")),
		CHARGE_POWER_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT).persistencePriority(PersistencePriority.HIGH).text("Charge Power L1")),
		CHARGE_POWER_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT).persistencePriority(PersistencePriority.HIGH).text("Charge Power L2")),
		CHARGE_POWER_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT).persistencePriority(PersistencePriority.HIGH).text("Charge Power L3")),
		APPARENT_POWER_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT_AMPERE).persistencePriority(PersistencePriority.LOW).text("Apparent Power L1")),
		APPARENT_POWER_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT_AMPERE).persistencePriority(PersistencePriority.LOW).text("Apparent Power L2")),
		APPARENT_POWER_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT_AMPERE).persistencePriority(PersistencePriority.LOW).text("Apparent Power L3")),
		APPARENT_POWER_SUM(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT_AMPERE).persistencePriority(PersistencePriority.LOW).text("Apparent Power sum")),
		REACTIVE_POWER_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT_AMPERE_REACTIVE).persistencePriority(PersistencePriority.LOW).text("Reactive Power L1")),
		REACTIVE_POWER_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT_AMPERE_REACTIVE).persistencePriority(PersistencePriority.LOW).text("Reactive Power L2")),
		REACTIVE_POWER_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT_AMPERE_REACTIVE).persistencePriority(PersistencePriority.LOW).text("Reactive Power L3")),
		REACTIVE_POWER_SUM(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT_AMPERE_REACTIVE).persistencePriority(PersistencePriority.LOW).text("Reactive Power sum")),
		ENERGY_DELIVERED_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT_HOURS).persistencePriority(PersistencePriority.LOW).text("Real energy delivered L1")),
		ENERGY_DELIVERED_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT_HOURS).persistencePriority(PersistencePriority.LOW).text("Real energy delivered L2")),
		ENERGY_DELIVERED_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT_HOURS).persistencePriority(PersistencePriority.LOW).text("Real energy delivered L3")),
		ENERGY_DELIVERED_SUM(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT_HOURS).persistencePriority(PersistencePriority.LOW).text("Real energy delivered sum")),
		ENERGY_CONSUMED_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT_HOURS).persistencePriority(PersistencePriority.LOW).text("Real energy consumed L1")),
		ENERGY_CONSUMED_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT_HOURS).persistencePriority(PersistencePriority.LOW).text("Real energy consumed L2")),
		ENERGY_CONSUMED_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT_HOURS).persistencePriority(PersistencePriority.LOW).text("Real energy consumed L3")),
		ENERGY_CONSUMED_SUM(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT_HOURS).persistencePriority(PersistencePriority.LOW).text("Real energy consumed sum")),
		APPARENT_ENERGY_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT_AMPERE_HOURS).persistencePriority(PersistencePriority.LOW).text("Apparent Power L1")),
		APPARENT_ENERGY_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT_AMPERE_HOURS).persistencePriority(PersistencePriority.LOW).text("Apparent Power L2")),
		APPARENT_ENERGY_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT_AMPERE_HOURS).persistencePriority(PersistencePriority.LOW).text("Apparent Power L3")),
		APPARENT_ENERGY_SUM(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT_AMPERE_HOURS).persistencePriority(PersistencePriority.LOW).text("Apparent Power sum")),
		REACTIVE_ENERGY_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT_AMPERE_REACTIVE_HOURS).persistencePriority(PersistencePriority.LOW).text("Reactive Power L1")),
		REACTIVE_ENERGY_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT_AMPERE_REACTIVE_HOURS).persistencePriority(PersistencePriority.LOW).text("Reactive Power L2")),
		REACTIVE_ENERGY_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT_AMPERE_REACTIVE_HOURS).persistencePriority(PersistencePriority.LOW).text("Reactive Power L3")),
		REACTIVE_ENERGY_SUM(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT_AMPERE_REACTIVE_HOURS).persistencePriority(PersistencePriority.LOW).text("Reactive Power sum")),
		AVAILABILITY(Doc.of(OpenemsType.BOOLEAN).persistencePriority(PersistencePriority.MEDIUM).text("Availability")),
		MODE_3_STATE(Doc.of(OpenemsType.STRING).persistencePriority(PersistencePriority.LOW).text("Mode 3 state")),
		ACTUAL_APPLIED_MAX_CURRENT(Doc.of(OpenemsType.FLOAT).persistencePriority(PersistencePriority.LOW).text("Actual applied max current")),
		MODBUS_SLAVE_MAX_CURRENT_VALID_TIME(Doc.of(OpenemsType.INTEGER).unit(Unit.SECONDS).persistencePriority(PersistencePriority.LOW).text("Remaining time before fallback to safe current")),
		MODBUS_SLAVE_MAX_CURRENT(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_WRITE).unit(Unit.AMPERE).persistencePriority(PersistencePriority.LOW).text("Modbus slave max current")),
		ACTIVE_LOAD_BALANCING_SAFE_CURRENT(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE).persistencePriority(PersistencePriority.LOW).text("Active load balamcing safe current")),
		MODBUS_SLAVE_RECEIVED_SETPOINT_ACCOUNTED_FOR(Doc.of(OpenemsType.BOOLEAN).persistencePriority(PersistencePriority.LOW).text("Modbus slave received setpoint accounted for")),
		CHARGE_USING_1_OR_3_PHASES(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE).unit(Unit.NONE).persistencePriority(PersistencePriority.MEDIUM).text("Charge using 1 or 3 phases")),
		
		ERROR(Doc.of(Level.FAULT).persistencePriority(PersistencePriority.LOW).text("Error in the charging station.")),
		
		SET_CHARGING_CURRENT_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE).accessMode(AccessMode.READ_WRITE).persistencePriority(PersistencePriority.HIGH).text("set current")),
		SET_CHARGING_CURRENT_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE).accessMode(AccessMode.READ_WRITE).persistencePriority(PersistencePriority.HIGH).text("set current")),
		SET_CHARGING_CURRENT_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE).accessMode(AccessMode.READ_WRITE).persistencePriority(PersistencePriority.HIGH).text("set current"))

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
	
	public default WriteChannel<Float> getSetChargingCurrentL1Channel() {
		return this.channel(ChannelId.SET_CHARGING_CURRENT_L1);
	}
	
	public default void setChargingCurrentL1(Float value) throws OpenemsNamedException {
		this.getSetChargingCurrentL1Channel().setNextValue(value);
	}
	

	public default void setChargingCurrentL1WriteValue(Float value) throws OpenemsNamedException {
		this.getSetChargingCurrentL1Channel().setNextWriteValue(value);
	}
	
	public default WriteChannel<Float> getSetChargingCurrentL2Channel() {
		return this.channel(ChannelId.SET_CHARGING_CURRENT_L2);
	}
	
	public default void setChargingCurrentL2(Float value) throws OpenemsNamedException {
		this.getSetChargingCurrentL2Channel().setNextValue(value);
	}
	

	public default void setChargingCurrentL2WriteValue(Float value) throws OpenemsNamedException {
		this.getSetChargingCurrentL2Channel().setNextWriteValue(value);
	}

	
	public default WriteChannel<Float> getSetChargingCurrentL3Channel() {
		return this.channel(ChannelId.SET_CHARGING_CURRENT_L3);
	}
	
	public default void setChargingCurrentL3(Float value) throws OpenemsNamedException {
		this.getSetChargingCurrentL3Channel().setNextValue(value);
	}
	

	public default void setChargingCurrentL3WriteValue(Float value) throws OpenemsNamedException {
		this.getSetChargingCurrentL3Channel().setNextWriteValue(value);
	}
	
	public default WriteChannel<Float> getModbusSlaveMaxCurrentChannel() {
		return this.channel(ChannelId.MODBUS_SLAVE_MAX_CURRENT);
	}
	
	public default void setModbusSlaveMaxCurrent(Float value) {
		this.getModbusSlaveMaxCurrentChannel().setNextValue(value);
	}
	
	public default void setModbusSlaveMaxCurrentWriteValue(Float value) throws OpenemsNamedException {
		this.getModbusSlaveMaxCurrentChannel().setNextWriteValue(value);
	}
	
	public default WriteChannel<Integer> getUsedPhasesChannel() {
		return this.channel(ChannelId.CHARGE_USING_1_OR_3_PHASES);
	}
	
	public default void setUsedPhases(Integer value) {
		this.getUsedPhasesChannel().setNextValue(value);
	}
	
	public default void setUsedPhasesWriteValue(Integer value) throws OpenemsNamedException {
		this.getUsedPhasesChannel().setNextWriteValue(value);
	}


}
