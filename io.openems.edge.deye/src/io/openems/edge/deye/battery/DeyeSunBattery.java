package io.openems.edge.deye.battery;



import static io.openems.common.channel.PersistencePriority.HIGH;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
//import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.deye.enums.BatteryChargeMode;
import io.openems.edge.deye.enums.BatteryOperateMode;
import io.openems.edge.deye.enums.BatteryRunState;
import io.openems.edge.deye.enums.WorkState;
import io.openems.edge.timedata.api.TimedataProvider;

public interface DeyeSunBattery
		extends Battery, OpenemsComponent, EventHandler, ModbusSlave, TimedataProvider {

	/**
	 * Gets the Modbus Unit-ID.
	 *
	 * @return the Unit-ID
	 */
	public Integer getUnitId();

	/**
	 * Gets the Modbus-Bridge Component-ID, i.e. "modbus0".
	 *
	 * @return the Component-ID
	 */
	public String getModbusBridgeId();

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		// OpenEMS state machine
		RUN_STATE(Doc.of(BatteryRunState.values()) //
				.text("Current State of State-Machine").persistencePriority(HIGH)), //	
		
		//START_STOP(Doc.of(StartStop.values())),

	    // BMS Status Registers (read-only)
	    BMS_CHARGING_VOLTAGE(Doc.of(OpenemsType.INTEGER)
	            .unit(Unit.MILLIVOLT)
	            .accessMode(AccessMode.READ_WRITE)),
	    BMS_DISCHARGING_VOLTAGE(Doc.of(OpenemsType.INTEGER)
	            .unit(Unit.MILLIVOLT)
	            .accessMode(AccessMode.READ_WRITE)),	    
	    CONFIGURABLE_CHARGE_CURRENT_LIMIT(Doc.of(OpenemsType.INTEGER)
	            .unit(Unit.AMPERE)
	            .accessMode(AccessMode.READ_WRITE)),
	    CONIGURABLE_DISCHARGE_CURRENT_LIMIT(Doc.of(OpenemsType.INTEGER)
	            .unit(Unit.AMPERE)
	            .accessMode(AccessMode.READ_WRITE)),
	    BMS_BATTERY_SOC(Doc.of(OpenemsType.INTEGER)
	            .unit(Unit.PERCENT)
	            .accessMode(AccessMode.READ_ONLY)),
	    BMS_BATTERY_VOLTAGE(Doc.of(OpenemsType.INTEGER)
	            .unit(Unit.MILLIVOLT)
	            .accessMode(AccessMode.READ_WRITE)),
	    BMS_BATTERY_CURRENT(Doc.of(OpenemsType.INTEGER)
	            .unit(Unit.MILLIAMPERE)
	            .accessMode(AccessMode.READ_WRITE)),
	    OFF_GRID_BATTERY_CHARGE_CURRENT_LIMIT(Doc.of(OpenemsType.INTEGER)
	            .unit(Unit.AMPERE)
	            .accessMode(AccessMode.READ_WRITE)),
	    OFF_GRID_BATTERY_DISCHARGE_CURRENT_LIMIT(Doc.of(OpenemsType.INTEGER)
	            .unit(Unit.AMPERE)
	            .accessMode(AccessMode.READ_WRITE)),
	    BMS_BATTERY_ALARM(Doc.of(OpenemsType.INTEGER)
	            .accessMode(AccessMode.READ_WRITE)),
	    BMS_BATTERY_FAULT_LOCATION(Doc.of(OpenemsType.INTEGER)
	            .accessMode(AccessMode.READ_WRITE)),
	    BMS_BATTERY_SYMBOL_2(Doc.of(OpenemsType.INTEGER)
	            .accessMode(AccessMode.READ_WRITE)),
	    BMS_BATTERY_LITHIUM_TYPE(Doc.of(OpenemsType.INTEGER)
	            .accessMode(AccessMode.READ_WRITE)),
	    BMS_BATTERY_SOH(Doc.of(OpenemsType.INTEGER)
	            .accessMode(AccessMode.READ_WRITE)),		
		
		SET_BATTERY_CHARGE_MODE(Doc.of(BatteryChargeMode.values()) // lead or lithium optimized charge curve
				.accessMode(AccessMode.WRITE_ONLY)), //		
		BATTERY_CHARGE_MODE(Doc.of(BatteryChargeMode.values()) // 
				.accessMode(AccessMode.READ_ONLY)), //			
		
		SET_BATTERY_EQUALIZATION_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.WRITE_ONLY)), //	
		BATTERY_EQUALIZATION_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //				
				.accessMode(AccessMode.READ_ONLY)), //			

		SET_BATTERY_ABSORPTION_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.WRITE_ONLY)), //	
		BATTERY_ABSORPTION_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //			
		

		SET_BATTERY_FLOAT_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.WRITE_ONLY)), //	
		BATTERY_FLOAT_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_ONLY)), //			
		
		SET_BATTERY_CAPACITY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE_HOURS) //
				.accessMode(AccessMode.WRITE_ONLY)), //	
		BATTERY_CAPACITY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE_HOURS) //				
				.accessMode(AccessMode.READ_ONLY)), //			
		
		SET_BATTERY_EMPTY_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.WRITE_ONLY)), //	
		BATTERY_EMPTY_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_ONLY)), //
	
		
		SET_BATTERY_ZERO_EXPORT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		BATTERY_ZERO_EXPORT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY)), //		
		
		// Days between battery balancing cycles
		// Values between 0-90 days are vaild
		SET_BATTERY_EQUALIZATION_DAY_CYCLE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.WRITE_ONLY)), //	
		BATTERY_EQUALIZATION_DAY_CYCLE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_ONLY)), //			
		
		// time in balancing mode. Resolution 0.5h
		// Values 0-20 are valid (-> max. 10h) 
		SET_BATTERY_EQUALIZATION_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.WRITE_ONLY)), //		
		BATTERY_EQUALIZATION_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_ONLY)), //			
		
		SET_BATTERY_MAX_CHARGE_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.accessMode(AccessMode.WRITE_ONLY)), //		
		BATTERY_MAX_CHARGE_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.accessMode(AccessMode.READ_WRITE)), //			
		
		SET_BATTERY_MAX_DISCHARGE_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.accessMode(AccessMode.WRITE_ONLY)), //		
		BATTERY_MAX_DISCHARGE_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.accessMode(AccessMode.READ_WRITE)), //		
		
		// Battery voltages need to be adjusted based on temperature 
		// (for example, charging voltage must be higher in cold conditions). 
		// The TEMPCO register defines by how many millivolts per degree Celsius the voltage setpoints shift per cell.
		SET_BATTERY_TEMPERATURE_COMPENSATION_VALUE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.WRITE_ONLY)), //		
		BATTERY_TEMPERATURE_COMPENSATION_VALUE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_ONLY)), //			

	    // Battery operating parameters (read-only)
	    BATTERY_OPERATE_MODE(Doc.of(BatteryOperateMode.values())
	        .accessMode(AccessMode.READ_ONLY)), // register 111
	    
	    // Battery operating parameters (read-only)
	    SET_BATTERY_OPERATE_MODE(Doc.of(BatteryOperateMode.values())
	        .accessMode(AccessMode.WRITE_ONLY)), // register 111	    

	    LITHIUM_WAKE_UP_SIGN(Doc.of(OpenemsType.INTEGER)
	        .accessMode(AccessMode.READ_ONLY)), // register 112

	    BATTERY_INTERNAL_RESISTANCE(Doc.of(OpenemsType.INTEGER)
	        .unit(Unit.MILLIOHM)
	        .accessMode(AccessMode.READ_ONLY)), // register 113

	    BATTERY_CHARGING_EFFICIENCY(Doc.of(OpenemsType.INTEGER)
	        .unit(Unit.PERCENT)
	        .accessMode(AccessMode.READ_ONLY)), // register 114

	    BATTERY_CAPACITY_SHUTDOWN(Doc.of(OpenemsType.INTEGER)
	        .unit(Unit.PERCENT)
	        .accessMode(AccessMode.READ_ONLY)), // register 115

	    BATTERY_CAPACITY_RESTART(Doc.of(OpenemsType.INTEGER)
	        .unit(Unit.PERCENT)
	        .accessMode(AccessMode.READ_ONLY)), // register 116

	    BATTERY_LOW_BATT_CAPACITY(Doc.of(OpenemsType.INTEGER)
	        .unit(Unit.PERCENT)
	        .accessMode(AccessMode.READ_ONLY)), // register 117

	    BATTERY_VOLTAGE_SHUTDOWN(Doc.of(OpenemsType.INTEGER)
	        .unit(Unit.MILLIVOLT)
	        .accessMode(AccessMode.READ_ONLY)), // register 118

	    BATTERY_VOLTAGE_RESTART(Doc.of(OpenemsType.INTEGER)
	        .unit(Unit.MILLIVOLT)
	        .accessMode(AccessMode.READ_ONLY)), // register 119

	    BATTERY_VOLTAGE_LOW_BATT(Doc.of(OpenemsType.INTEGER)
	        .unit(Unit.MILLIVOLT)
	        .accessMode(AccessMode.READ_ONLY)), // register 120
	    
	    // BMS Metrics
	    BATTERY_TEMPERATURE(Doc.of(OpenemsType.INTEGER) // register 586
	            .unit(Unit.DEGREE_CELSIUS)
	            .accessMode(AccessMode.READ_ONLY)),
	    BATTERY_VOLTAGE(Doc.of(OpenemsType.INTEGER) // register 587
	            .unit(Unit.MILLIVOLT)
	            .accessMode(AccessMode.READ_ONLY)),
	    BATTERY_SOC(Doc.of(OpenemsType.INTEGER) // register 588
	            .unit(Unit.PERCENT)
	            .accessMode(AccessMode.READ_ONLY)),
	    BATTERY_OUTPUT_POWER(Doc.of(OpenemsType.INTEGER) // register 590
	            .unit(Unit.WATT)
	            .accessMode(AccessMode.READ_ONLY)),
	    BATTERY_OUTPUT_CURRENT(Doc.of(OpenemsType.INTEGER) // register 591
	            .unit(Unit.MILLIAMPERE)
	            .accessMode(AccessMode.READ_ONLY)),
	    BATTERY_CORRECTED_AH(Doc.of(OpenemsType.INTEGER) // register 592
	            .unit(Unit.AMPERE_HOURS)
	            .accessMode(AccessMode.READ_ONLY)),	   	    

	

	    // Battery Energy
	    TODAY_BATTERY_CHARGE(Doc.of(OpenemsType.INTEGER)
	            .unit(Unit.WATT_HOURS)
	            .accessMode(AccessMode.READ_ONLY)),
	    TODAY_BATTERY_DISCHARGE(Doc.of(OpenemsType.INTEGER)
	            .unit(Unit.WATT_HOURS)
	            .accessMode(AccessMode.READ_ONLY)),
	    
	    TOTAL_BATTERY_CHARGE(Doc.of(OpenemsType.LONG)
	            .unit(Unit.WATT_HOURS)
	            .accessMode(AccessMode.READ_ONLY)),
	    TOTAL_BATTERY_DISCHARGE(Doc.of(OpenemsType.LONG)
	            .unit(Unit.WATT_HOURS)
	            .accessMode(AccessMode.READ_ONLY)),
	    
	    // OpenEMS calculated channels
	    // Ideally they have the same values as total
	    // counters from Deye
	    DC_CHARGE_ENERGY(Doc.of(OpenemsType.LONG)
	            .unit(Unit.WATT_HOURS)
	            .accessMode(AccessMode.READ_ONLY)),
	    DC_DISCHARGE_ENERGY(Doc.of(OpenemsType.LONG)
	            .unit(Unit.WATT_HOURS)
	            .accessMode(AccessMode.READ_ONLY)),


	    // Temperatures
	    DC_TRANSFORMER_TEMP(Doc.of(OpenemsType.FLOAT)
	            .unit(Unit.DEGREE_CELSIUS)
	            .accessMode(AccessMode.READ_ONLY)),
	    HEATSINK_TEMP(Doc.of(OpenemsType.FLOAT)
	            .unit(Unit.DEGREE_CELSIUS)
	            .accessMode(AccessMode.READ_ONLY)),
	
		// EnumWriteChannels
		SET_WORK_STATE(Doc.of(WorkState.values()) //
				.accessMode(AccessMode.WRITE_ONLY)), //

		// IntegerWriteChannel
		SET_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		SET_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		
		SET_APPARENT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE) //
				.accessMode(AccessMode.WRITE_ONLY)), //		

		SET_GEN_PEAK_SHAVING_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		SET_GRID_PEAK_SHAVING_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		CT_RATIO(Doc.of(OpenemsType.INTEGER)), //
		
		//BATTERY_RUN_STATE(Doc.of(BatteryRunState.values()).accessMode(AccessMode.READ_WRITE)),	
		


		// LongReadChannel
		ORIGINAL_ACTIVE_CHARGE_ENERGY(Doc.of(OpenemsType.LONG)), //
		ORIGINAL_ACTIVE_DISCHARGE_ENERGY(Doc.of(OpenemsType.LONG)), //

		
/*		
		// IntegerReadChannels
		ORIGINAL_ALLOWED_CHARGE_POWER(new IntegerDoc() //
				.onChannelUpdate((self, newValue) -> {
					// on each Update to the channel -> set the ALLOWED_CHARGE_POWER value with a
					// delta of max 500
					IntegerReadChannel currentValueChannel = self
							.channel(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER);
					var originalValue = newValue.asOptional();
					var currentValue = currentValueChannel.value().asOptional();
					final int value;
					if (!originalValue.isPresent() && !currentValue.isPresent()) {
						value = 0;
					} else if (originalValue.isPresent() && !currentValue.isPresent()) {
						value = originalValue.get();
					} else if (!originalValue.isPresent() && currentValue.isPresent()) {
						value = currentValue.get();
					} else {
						value = Math.max(originalValue.get(), currentValue.get() - 500);
					}
					currentValueChannel.setNextValue(value);
				})), //

		ORIGINAL_ALLOWED_DISCHARGE_POWER(new IntegerDoc() //
				.onChannelUpdate((self, newValue) -> {
					// on each Update to the channel -> set the ALLOWED_DISCHARGE_POWER value with a
					// delta of max 500
					IntegerReadChannel currentValueChannel = self
							.channel(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER);
					var originalValue = newValue.asOptional();
					var currentValue = currentValueChannel.value().asOptional();
					final int value;
					if (!originalValue.isPresent() && !currentValue.isPresent()) {
						value = 0;
					} else if (originalValue.isPresent() && !currentValue.isPresent()) {
						value = originalValue.get();
					} else if (!originalValue.isPresent() && currentValue.isPresent()) {
						value = currentValue.get();
					} else {
						value = Math.min(originalValue.get(), currentValue.get() + 500);
					}
					currentValueChannel.setNextValue(value);
				})), //
*/
	
		APPARENT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE)), //


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

	/**
	 * Gets the Channel for {@link ChannelId#BMS_BATTERY_ALARM}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getBatteryAlarmChannel() {
		return this.channel(ChannelId.BMS_BATTERY_ALARM);
	}

	/**
	 * Gets the DC Discharge Power in [W]. Negative values for Charge; positive for
	 * Discharge. See {@link ChannelId#BMS_BATTERY_ALARM}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBatteryAlarm() {
		return this.getBatteryAlarmChannel().value();
	}	
	
	
	/**
	 * Gets the Channel for {@link ChannelId#GRID_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcPowerChannel() {
		return this.channel(ChannelId.BATTERY_OUTPUT_POWER);
	}

	/**
	 * Gets the DC Discharge Power in [W]. Negative values for Charge; positive for
	 * Discharge. See {@link ChannelId#GRID_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDcPower() {
		return this.getDcPowerChannel().value();
	}	
	
	
	/**
	 * Gets the Channel for {@link ChannelId#BATTERY_OUTPUT_CURRENT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getBatteryCurrentChannel() {
		return this.channel(ChannelId.BATTERY_OUTPUT_CURRENT);
	}

	/**
	 * Gets the DC Discharge Power in [W]. Negative values for Charge; positive for
	 * Discharge. See {@link ChannelId#GRID_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBatteryCurrent() {
		return this.getBatteryCurrentChannel().value();
	}		
	
	/**
	 * Gets the Channel for {@link ChannelId#CONIGURABLE_DISCHARGE_CURRENT_LIMIT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getConfigurableDischargeCurrentLimitChannel() {
		return this.channel(ChannelId.CONIGURABLE_DISCHARGE_CURRENT_LIMIT);
	}

	/**
	 * Gets the DC Discharge Power in [W]. Negative values for Charge; positive for
	 * Discharge. See {@link ChannelId#CONIGURABLE_DISCHARGE_CURRENT_LIMIT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getConfigurableDischargeCurrentLimit() {
		return this.getConfigurableDischargeCurrentLimitChannel().value();
	}	
	
	/**
	 * Gets the Channel for {@link ChannelId#CONFIGURABLE_CHARGE_CURRENT_LIMIT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getConfigurableChargeCurrentLimitChannel() {
		return this.channel(ChannelId.CONFIGURABLE_CHARGE_CURRENT_LIMIT);
	}

	/**
	 * Gets the DC Discharge Power in [W]. Negative values for Charge; positive for
	 * Discharge. See {@link ChannelId#CONFIGURABLE_CHARGE_CURRENT_LIMIT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getConfigurableChargeCurrentLimit() {
		return this.getConfigurableChargeCurrentLimitChannel().value();
	}	
		
	/**
	 * Gets the Channel for {@link ChannelId#BMS_BATTERY_VOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getBmsBatteryVoltageChannel() {
		return this.channel(ChannelId.BMS_BATTERY_VOLTAGE);
	}

	/**
	 * Gets the DC Discharge Power in [W]. Negative values for Charge; positive for
	 * Discharge. See {@link ChannelId#BMS_BATTERY_VOLTAGE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBmsBatteryVoltage() {
		return this.getBmsBatteryVoltageChannel().value();
	}	
	

	//
	public default EnumReadChannel getBatteryOperateModeChannel() {
		return this.channel(ChannelId.BATTERY_OPERATE_MODE);
	}

	public default EnumWriteChannel getSetBatteryOperateModeChannel() {
		return this.channel(ChannelId.SET_BATTERY_OPERATE_MODE);
	}	


	public default Value<Integer> getBatteryOperateMode() {
		return this.getBatteryOperateModeChannel().value();
	}


	public default void setBatteryOperateMode(BatteryOperateMode value) throws OpenemsNamedException {
		this.getSetBatteryOperateModeChannel().setNextWriteValue(value);
	}

	public static ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
		// TODO Auto-generated method stub
		return null;
	}		

	

	// ----------------------------------------
	// StateMachine Channel
	// ----------------------------------------

	/**
	 * Gets the Channel for {@link ChannelId#RUN_STATE}.
	 *
	 * @return the Channel
	 */
	public default Channel<BatteryRunState> getRunStateChannel() {
		return this.channel(ChannelId.RUN_STATE);
	}

	/**
	 * Gets current state of the {@link StateMachine}. See
	 * {@link ChannelId#RUN_STATE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default BatteryRunState getRunState() {
		return this.getRunStateChannel().value().asEnum();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#RUN_STATE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setRunState(BatteryRunState value) {
		this.getRunStateChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#BATTERY_VOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getBatteryVoltageChannel() {
		return this.channel(ChannelId.BATTERY_VOLTAGE);
	}

	/**
	 * Gets the Battery Voltage.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBatteryVoltage() {
		return this.getBatteryVoltageChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#BATTERY_EMPTY_VOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getBatteryEmptyVoltageChannel() {
		return this.channel(ChannelId.BATTERY_EMPTY_VOLTAGE);
	}

	/**
	 * Gets the Battery Empty Voltage.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBatteryEmptyVoltage() {
		return this.getBatteryEmptyVoltageChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#BATTERY_VOLTAGE_LOW_BATT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getBatteryVoltageLowChannel() {
		return this.channel(ChannelId.BATTERY_VOLTAGE_LOW_BATT);
	}

	/**
	 * Gets the Battery Low Voltage.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBatteryVoltageLow() {
		return this.getBatteryVoltageLowChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#BATTERY_CAPACITY_SHUTDOWN}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getBatteryCapacityShutdownChannel() {
		return this.channel(ChannelId.BATTERY_CAPACITY_SHUTDOWN);
	}

	/**
	 * Gets the Battery Capacity Shutdown.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBatteryCapacityShutdown() {
		return this.getBatteryCapacityShutdownChannel().value();
	}
	
	// Capacity
	public default IntegerReadChannel getBatteryCapacityChannel() {
	    return this.channel(ChannelId.BATTERY_CAPACITY);
	}

	public default Value<Integer> getBatteryCapacity() {
	    return this.getBatteryCapacityChannel().value();
	}	

	public boolean hasError();

	public boolean hasWarning();

	// 108 / 109
	// Getter inverter configured values for Charge/Discharge max. Current
	//
	public default IntegerReadChannel getBmsMaxDischargeCurrentChannel() {
	    return this.channel(ChannelId.CONIGURABLE_DISCHARGE_CURRENT_LIMIT);
	}

	public default Value<Integer> getBmsMaxDischargeCurrent() {
	    return this.getBmsMaxDischargeCurrentChannel().value();
	}

	public default IntegerReadChannel getBmsMaxChargeCurrentChannel() {
	    return this.channel(ChannelId.CONFIGURABLE_CHARGE_CURRENT_LIMIT);
	}

	public default Value<Integer> getBmsMaxChargeCurrent() {
	    return this.getBmsMaxChargeCurrentChannel().value();
	}	
	
	// 212 / 213
	// Getter / Setter Battery max. Charge Current
	//
	public default IntegerReadChannel getBatteryMaxChargeCurrentChannel() {
	    return this.channel(ChannelId.BATTERY_MAX_CHARGE_CURRENT);
	}

	public default Value<Integer> getBatteryMaxChargeCurrent() {
	    return this.getBatteryMaxChargeCurrentChannel().value();
	}

	public default IntegerReadChannel getBatteryMaxDischargeCurrentChannel() {
	    return this.channel(ChannelId.BATTERY_MAX_DISCHARGE_CURRENT);
	}

	public default Value<Integer> getBatteryMaxDischargeCurrent() {
	    return this.getBatteryMaxDischargeCurrentChannel().value();
	}
	
	// 218 / 219
	// Getter Charge/Discharge Current
	//
	// 
	public default IntegerReadChannel getOffgridMaxChargeCurrentChannel() {
	    return this.channel(ChannelId.OFF_GRID_BATTERY_CHARGE_CURRENT_LIMIT);
	}

	public default Value<Integer> getOffgridMaxChargeCurrent() {
	    return this.getBmsMaxChargeCurrentChannel().value();
	}
	
	public default IntegerReadChannel getOffgridMaxDischargeCurrentChannel() {
	    return this.channel(ChannelId.OFF_GRID_BATTERY_DISCHARGE_CURRENT_LIMIT);
	}

	public default Value<Integer> getOffgridMaxDischargeCurrent() {
	    return this.getBmsMaxDischargeCurrentChannel().value();
	}
	


	
	
	public int getConfiguredMaxChargeCurrent();
	
	public int getConfiguredMaxDischargeCurrent();

	public void setOfflineByExternal(String string);

	public void clearExternalOffline();
		
}
