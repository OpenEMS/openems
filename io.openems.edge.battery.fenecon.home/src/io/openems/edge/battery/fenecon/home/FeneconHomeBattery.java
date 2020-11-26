package io.openems.edge.battery.fenecon.home;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.fenecon.home.enums.BMSControl;
import io.openems.edge.battery.fenecon.home.enums.State;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;

public interface FeneconHomeBattery extends Battery, OpenemsComponent, StartStoppable {

	/**
	 * Gets the Channel for {@link ChannelId#PRE_CHARGE_CONTROL}.
	 * 
	 * @return the Channel
	 */
	public default WriteChannel<BMSControl> getBMSControlChannel() {
		return this.channel(ChannelId.BMS_CONTROL);
	}

	/**
	 * Gets the PreChargeControl, see {@link ChannelId#PRE_CHARGE_CONTROL}.
	 * 
	 * @return the Channel {@link Value}
	 */
	public default BMSControl getBMSControl() {
		return this.getBMSControlChannel().value().asEnum();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#PRE_CHARGE_CONTROL} Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setBMSControl(BMSControl value) {
		this.getBMSControlChannel().setNextValue(value);
	}

	/**
	 * Writes the value to the {@link ChannelId#PRE_CHARGE_CONTROL} Register.
	 * 
	 * @param value the next value
	 * @throws OpenemsNamedException on error
	 */
	public default void setBMSControl(BMSControl value) throws OpenemsNamedException {
		this.getBMSControlChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAX_START_ATTEMPTS}.
	 * 
	 * @return the Channel
	 */
	public default StateChannel getMaxStartAttemptsChannel() {
		return this.channel(ChannelId.MAX_START_ATTEMPTS);
	}

	/**
	 * Gets the {@link StateChannel} for {@link ChannelId#MAX_START_ATTEMPTS}.
	 * 
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getMaxStartAttempts() {
		return this.getMaxStartAttemptsChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#MAX_START_ATTEMPTS} Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setMaxStartAttempts(Boolean value) {
		this.getMaxStartAttemptsChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAX_STOP_ATTEMPTS}.
	 * 
	 * @return the Channel
	 */
	public default StateChannel getMaxStopAttemptsChannel() {
		return this.channel(ChannelId.MAX_STOP_ATTEMPTS);
	}

	/**
	 * Gets the {@link StateChannel} for {@link ChannelId#MAX_STOP_ATTEMPTS}.
	 * 
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getMaxStopAttempts() {
		return this.getMaxStopAttemptsChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#MAX_STOP_ATTEMPTS}
	 * Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setMaxStopAttempts(Boolean value) {
		this.getMaxStopAttemptsChannel().setNextValue(value);
	}

	/**
	 * Gets the target Start/Stop mode from config or StartStop-Channel.
	 * 
	 * @return {@link StartStop}
	 */
	public StartStop getStartStopTarget();

	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		// EnumWriteChannels
		RACK_PRE_ALARM_CELL_OVER_VOLTAGE(Doc.of(OpenemsType.BOOLEAN) //
				.text("Rack Cell Over Voltage Alarm")), //
		RACK_PRE_ALARM_CELL_UNDER_VOLTAGE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Cell Under Voltage Alarm")), //
		RACK_PRE_ALARM_OVER_CHARGING_CURRENT(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Over Charging Current Alarm")), //
		RACK_PRE_ALARM_OVER_DISCHARGING_CURRENT(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Over Discharging Current Alarm")), //
		RACK_PRE_ALARM_OVER_TEMPERATURE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Over Temperature Alarm")), //
		RACK_PRE_ALARM_UNDER_TEMPERATURE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Under Temperature Alarm")), //
		RACK_PRE_ALARM_CELL_VOLTAGE_DIFFERENCE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Cell VOltage Difference Alarm")), //
		RACK_PRE_ALARM_BCU_TEMP_DIFFERENCE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack BCU Temp Difference Alarm")), //
		RACK_PRE_ALARM_UNDER_SOC(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Under SOC Alarm")), //
		RACK_PRE_ALARM_UNDER_SOH(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Under SOH Alarm")), //
		RACK_PRE_ALARM_OVER_CHARGING_POWER(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Over Charging Alarm")), //
		RACK_PRE_ALARM_OVER_DISCHARGING_POWER(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Over Discharging Alarm")), //
		RACK_LEVEL_1_CELL_OVER_VOLTAGE(Doc.of(OpenemsType.BOOLEAN) //
				.text("Rack Cell Over Voltage warning")), //
		RACK_LEVEL_1_CELL_UNDER_VOLTAGE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Cell Under Voltage warning")), //
		RACK_LEVEL_1_OVER_CHARGING_CURRENT(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Over Charging Current warning")), //
		RACK_LEVEL_1_OVER_DISCHARGING_CURRENT(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Over Discharging Current warning")), //
		RACK_LEVEL_1_OVER_TEMPERATURE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Over Temperature warning")), //
		RACK_LEVEL_1_UNDER_TEMPERATURE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Under Temperature warning")), //
		RACK_LEVEL_1_CELL_VOLTAGE_DIFFERENCE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Cell VOltage Difference warning")), //
		RACK_LEVEL_1_BCU_TEMP_DIFFERENCE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack BCU Temp Difference warning")), //
		RACK_LEVEL_1_UNDER_SOC(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Under SOC warning")), //
		RACK_LEVEL_1_UNDER_SOH(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Under SOH warning")), //
		RACK_LEVEL_1_OVER_CHARGING_POWER(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Over Charging warning")), //
		RACK_LEVEL_1_OVER_DISCHARGING_POWER(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Over Discharging warning")), //
		RACK_LEVEL_2_CELL_OVER_VOLTAGE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Cell Over Voltage Fault")), //
		RACK_LEVEL_2_CELL_UNDER_VOLTAGE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Cell Under Voltage Fault")), //
		RACK_LEVEL_2_OVER_CHARGING_CURRENT(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Over Charging Current Fault")), //
		RACK_LEVEL_2_OVER_DISCHARGING_CURRENT(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Over Discharging Current Fault")), //
		RACK_LEVEL_2_OVER_TEMPERATURE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Over Temperature Fault")), //
		RACK_LEVEL_2_UNDER_TEMPERATURE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Under Temperature Fault")), //
		RACK_LEVEL_2_CELL_VOLTAGE_DIFFERENCE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Cell Voltage Difference Fault")), //
		RACK_LEVEL_2_BCU_TEMP_DIFFERENCE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack BCU Temp Difference Fault")), //
		RACK_LEVEL_2_CELL_TEMPERATURE_DIFFERENCE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Cell Temperature Difference Fault")), //
		RACK_LEVEL_2_INTERNAL_COMMUNICATION(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Internal Communication Fault")), //
		RACK_LEVEL_2_EXTERNAL_COMMUNICATION(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack External Communication Fault")), //
		RACK_LEVEL_2_PRE_CHARGE_FAIL(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Pre Charge Fault")), //
		RACK_LEVEL_2_PARALLEL_FAIL(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Parallel Fault")), //
		RACK_LEVEL_2_SYSTEM_FAIL(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack System Fault")), //
		RACK_LEVEL_2_HARDWARE_FAIL(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Hardware Fault")), //
		ALARM_POSITION_BCU_1(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Alarm BCU 1 Position ")), //
		ALARM_POSITION_BCU_2(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Alarm BCU 2 Position ")), //
		ALARM_POSITION_BCU_3(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Alarm BCU 3 Position ")), //
		ALARM_POSITION_BCU_4(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Alarm BCU 4 Position ")), //
		ALARM_POSITION_BCU_5(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Alarm BCU 5 Position ")), //
		ALARM_POSITION_BCU_6(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Alarm BCU 6 Position ")), //
		ALARM_POSITION_BCU_7(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Alarm BCU 7 Position ")), //
		ALARM_POSITION_BCU_8(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Alarm BCU 8 Position ")), //
		ALARM_POSITION_BCU_9(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Alarm BCU 9 Position ")), //
		ALARM_POSITION_BCU_10(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Alarm BCU 10 Position ")), //
		WARNING_POSITION_BCU_1(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Warning BCU 1 Position ")), //
		WARNING_POSITION_BCU_2(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Warning BCU 2 Position ")), //
		WARNING_POSITION_BCU_3(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Warning BCU 3 Position ")), //
		WARNING_POSITION_BCU_4(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Warning BCU 4 Position ")), //
		WARNING_POSITION_BCU_5(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Warning BCU 5 Position ")), //
		WARNING_POSITION_BCU_6(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Warning BCU 6 Position ")), //
		WARNING_POSITION_BCU_7(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Warning BCU 7 Position ")), //
		WARNING_POSITION_BCU_8(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Warning BCU 8 Position ")), //
		WARNING_POSITION_BCU_9(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Warning BCU 9 Position ")), //
		WARNING_POSITION_BCU_10(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Warning BCU 10 Position ")), //
		FAULT_POSITION_BCU_1(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Fault BCU 1 Position ")), //
		FAULT_POSITION_BCU_2(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Fault BCU 2 Position ")), //
		FAULT_POSITION_BCU_3(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Fault BCU 3 Position ")), //
		FAULT_POSITION_BCU_4(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Fault BCU 4 Position ")), //
		FAULT_POSITION_BCU_5(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Fault BCU 5 Position ")), //
		FAULT_POSITION_BCU_6(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Fault BCU 6 Position ")), //
		FAULT_POSITION_BCU_7(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Fault BCU 7 Position ")), //
		FAULT_POSITION_BCU_8(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Fault BCU 8 Position ")), //
		FAULT_POSITION_BCU_9(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Fault BCU 9 Position ")), //
		FAULT_POSITION_BCU_10(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Fault BCU 10 Position ")), //
		BATTERY_RACK_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Battery Rack Voltage")), //
		BATTERY_RACK_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Battery Rack Current")), //
		BATTERY_RACK_SOC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Battery Rack State Of Charge")), //
		BATTERY_RACK_SOH(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PERCENT) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Battery Rack State Of Health")), //
		CELL_VOLTAGE_MIN(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Min Cell Voltage of All Module")), //
		ID_OF_CELL_VOLTAGE_MIN(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Id. (Min Cell Voltage)")), //
		CELL_VOLTAGE_MAX(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Cell Voltage MAX")), //
		ID_OF_CELL_VOLTAGE_MAX(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Id. (Max Cell Voltage)")), //
		MIN_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Min Temperature of Battery Rack")), //
		ID_OF_MIN_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Id. (Min Temp)")), //
		MAX_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Max Temperature of Battery Rack")), //
		ID_OF_MAX_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Id. (Max Temp)")), //
		MAX_CHARGE_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Battery Rack DC Charge Current Limit")), //
		MAX_DISCHARGE_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Battery Rack DC Discharge Current Limit")),
		MAX_DC_CHARGE_CURRENT_LIMIT_PER_BCU(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Max Charge Current Limit Per BCU")), //
		MAX_DC_DISCHARGE_CURRENT_LIMIT_PER_BCU(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Max Discharge Current Limit Per BCU")),
		RACK_NUMBER_OF_BATTERY_BCU(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Count Of The Connected BCU")),
		RACK_NUMBER_OF_CELLS_IN_SERIES_PER_MODULE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Rack Number Of Cells in  Series Per Module")),
		RACK_MAX_CELL_VOLTAGE_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Rack Upper Cell Voltage Border -> System will stop charging if a cell reaches this voltage value")),
		RACK_MIN_CELL_VOLTAGE_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Rack Lower Cell Voltage Border -> System will stop discharging if a cell reaches this voltage value")),
		RACK_HW_AFE_COMMUNICATION_FAULT(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Rack HW AFE Communication Fault")),
		RACK_HW_ACTOR_DRIVER_FAULT(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Rack HW Actor Driver Fault")),
		RACK_HW_EEPROM_COMMUNICATION_FAULT(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Rack HW EEPROM Communication Fault")),
		RACK_HW_VOLTAGE_DETECT_FAULT(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Rack HW Voltage Detect Voltage")),
		RACK_HW_TEMPERATURE_DETECT_FAULT(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Rack HW Temperature Detect Fault")),
		RACK_HW_CURRENT_DETECT_FAULT(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Rack HW Current Detect Fault")),
		RACK_HW_ACTOR_NOT_CLOSE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Rack HW Actor Not Close")),
		RACK_HW_ACTOR_NOT_OPEN(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Rack HW Actor Not Open")),
		RACK_HW_FUSE_BROKEN(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Rack HW Fuse Broken")),
		RACK_SYSTEM_AFE_OVER_TEMPERATURE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Rack System AFE Over Temperature")),
		RACK_SYSTEM_AFE_UNDER_TEMPERATURE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Rack System AFE Under Temperature")),
		RACK_SYSTEM_AFE_OVER_VOLTAGE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Rack System AFE Over Voltage")),
		RACK_SYSTEM_AFE_UNDER_VOLTAGE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Rack System AFE Over Temperature")),
		RACK_SYSTEM_HIGH_TEMPERATURE_PERMANENT_FAILURE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Rack System High Temperature Permanent Failure")),
		RACK_SYSTEM_LOW_TEMPERATURE_PERMANENT_FAILURE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Rack System  Low Temperature Permanent Failure")),
		RACK_SYSTEM_HIGH_CELL_VOLTAGE_PERMANENT_FAILURE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Rack System  High Cell Voltage Permanent Failure")),
		RACK_SYSTEM_LOW_CELL_VOLTAGE_PERMANENT_FAILURE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Rack System  Low Cell Voltage Permanent Failure")),
		RACK_SYSTEM_SHORT_CIRCUIT(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Rack System Low Cell Voltage Permanent Failure")),
		UPPER_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("CV Point")),
		BCU_BMS_SOFTWARE_VERSION(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Bcu Bms Software Version")),
		BCU_BMS_HARDWARE_VERSION(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Bcu Bms Hardware Version")),
		BCU_STATUS_ALARM(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Status Alarm")),
		BCU_STATUS_WARNING(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Status WARNNG")),
		BCU_STATUS_FAULT(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Status BCU Status Fault")),
		BCU_STATUS_PFET(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Status Pre-Charge FET On/Off")),
		BCU_STATUS_CFET(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Status Charge FET On/Off")),
		BCU_STATUS_DFET(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Status Discharge FET On/Off")),
		BCU_STATUS_BATTERY_IDLE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Status Battery Idle")),
		BCU_STATUS_BATTERY_CHARGING(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Status Battery Charging")),
		BCU_STATUS_BATTERY_DISCHARGING(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Status Battery Discharging")),
		BCU_PRE_ALARM_CELL_OVER_VOLTAGE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Alarm Cell Over Voltage")),
		BCU_PRE_ALARM_CELL_UNDER_VOLTAGE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Alarm Cell Under Voltage")),
		BCU_PRE_ALARM_OVER_CHARGING_CURRENT(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Alarm Over Charging Current")),
		BCU_PRE_ALARM_OVER_DISCHARGING_CURRENT(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Alarm Over Discharging Current")),
		BCU_PRE_ALARM_OVER_TEMPERATURE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Alarm Over Temperature")),
		BCU_PRE_ALARM_UNDER_TEMPERATURE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Alarm Under Temperature")),
		BCU_PRE_ALARM_CELL_VOLTAGE_DIFFERENCE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Alarm Cell Voltage Difference")),
		BCU_PRE_ALARM_BCU_TEMP_DIFFERENCE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Alarm BCU Temperature Difference")),
		BCU_PRE_ALARM_UNDER_SOC(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Alarm Under SOC")),
		BCU_PRE_ALARM_UNDER_SOH(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Alarm Under SOH")),
		BCU_PRE_ALARM_OVER_CHARGING_POWER(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Alarm Over Charging Power")),
		BCU_PRE_ALARM_OVER_DISCHARGING_POWER(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Alarm Over Discharging Power")),
		BCU_LEVEL_1_CELL_OVER_VOLTAGE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Warning Cell Over Voltage")),
		BCU_LEVEL_1_CELL_UNDER_VOLTAGE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Warning Cell Under Voltage")),
		BCU_LEVEL_1_OVER_CHARGING_CURRENT(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Warning Over Charging Current")),
		BCU_LEVEL_1_OVER_DISCHARGING_CURRENT(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Warning Over Discharging Current")),
		BCU_LEVEL_1_OVER_TEMPERATURE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Warning Over Temperature")),
		BCU_LEVEL_1_UNDER_TEMPERATURE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Warning Under Temperature")),
		BCU_LEVEL_1_CELL_VOLTAGE_DIFFERENCE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Warning Cell Voltage Difference")),
		BCU_LEVEL_1_BCU_TEMP_DIFFERENCE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Warning BCU Temperature Difference")),
		BCU_LEVEL_1_UNDER_SOC(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Warning Under SOC")),
		BCU_LEVEL_1_UNDER_SOH(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Warning Under SOH")),
		BCU_LEVEL_1_OVER_CHARGING_POWER(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Warning Over Charging Power")),
		BCU_LEVEL_1_OVER_DISCHARGING_POWER(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Warning Over Discharging Power")),
		BCU_LEVEL_2_CELL_OVER_VOLTAGE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Fault Cell Over Voltage")),
		BCU_LEVEL_2_CELL_UNDER_VOLTAGE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Fault Cell Under Voltage")),
		BCU_LEVEL_2_OVER_CHARGING_CURRENT(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Fault Over Charging Current")),
		BCU_LEVEL_2_OVER_DISCHARGING_CURRENT(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Fault Over Discharging Current")),
		BCU_LEVEL_2_OVER_TEMPERATURE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Fault Over Temperature")),
		BCU_LEVEL_2_UNDER_TEMPERATURE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Fault Under Temperature")),
		BCU_LEVEL_2_CELL_VOLTAGE_DIFFERENCE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Fault Cell Voltage Difference")),
		BCU_LEVEL_2_BCU_TEMP_DIFFERENCE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Fault BCU Temperature Difference")),
		BCU_LEVEL_2_TEMPERATURE_DIFFERENCE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Fault BCU Temperature Difference")),
		BCU_LEVEL_2_INTERNAL_COMMUNICATION(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Fault Internal Communication")),
		BCU_LEVEL_2_EXTERNAL_COMMUNICATION(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Fault External Communication")),
		BCU_LEVEL_2_PRECHARGE_FAIL(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Fault Pre-Charge Fail")),
		BCU_LEVEL_2_PARALLEL_FAIL(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Fault Parallel Fail")),
		BCU_LEVEL_2_SYSTEM_FAIL(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Fault System Fault")),
		BCU_LEVEL_2_HARDWARE_FAIL(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Fault Hardware Fault")),
		BCU_HW_AFE_COMMUNICAITON_FAULT(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU HW AFE Communication Fault")),
		BCU_HW_ACTOR_DRIVER_FAULT(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU HW Actor Driver Fault")),
		BCU_HW_EEPROM_COMMUNICATION_FAULT(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU HW EEPROM Communication Fault")),
		BCU_HW_VOLTAGE_DETECT_FAULT(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU HW Voltage Detect Fault")),
		BCU_HW_TEMPERATURE_DETECT_FAULT(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU HW Temperaure Detect Fault")),
		BCU_HW_CURRENT_DETECT_FAULT(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU HW Current Detect Fault")),
		BCU_HW_ACTOR_NOT_CLOSE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU HW Actor Not Close Fault")),
		BCU_HW_ACTOR_NOT_OPEN(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU HW Actor Not Open")),
		BCU_HW_FUSE_BROKEN(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU HW Fuse Broken Fault ")),
		BCU_SYSTEM_AFE_OVER_TEMPERATURE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)//
				.text("BCU System AFE Over Temperature Fault")),
		BCU_SYSTEM_AFE_UNDER_TEMPERATURE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU System AFE Under Temperature Fault")),
		BCU_SYSTEM_AFE_OVER_VOLTAGE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU System AFE Over Voltage Fault")),
		BCU_SYSTEM_AFE_UNDER_VOLTAGE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU System AFE Under Voltage Fault")),
		BCU_SYSTEM_HIGH_TEMPERATURE_PERMANENT_FAILURE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU System High Temperature Permanent Fault")),
		BCU_SYSTEM_LOW_TEMPERATURE_PERMANENT_FAILURE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU System Low Temperature Permanent Fault")),
		BCU_SYSTEM_HIGH_CELL_VOLTAGE_PERMANENT_FAILURE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU System High Cell Voltage Permanent Fault")),
		BCU_SYSTEM_LOW_CELL_VOLTAGE_PERMANENT_FAILURE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU System Low Cell Voltage Permanent Fault")),
		BCU_SYSTEM_LOW_CELL_VOLTAGE_FAILURE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU System Low Cell Voltage Permanent Fault")),
		BCU_SYSTEM_SHORT_CIRCUIT(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU System Short Circuit Fault")),
		BCU_SOC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU SOC")),
		BCU_SOH(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU SOH")),
		BCU_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Battery BCU Voltage")),
		BCU_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Battery BCU Current")),
		BCU_MIN_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Min Cell Voltage")),
		BCU_MAX_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Maxc Cell Voltage")),
		BCU_AVARAGE_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Avarage Of All Cell Voltages")),
		BCU_MAX_CHARGE_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU DC Charge Current Limit")),
		BCU_MIN_CHARGE_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU DC Discharge Current Limit")),
		BMS_SERIAL_NUMBER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMS Serial Number")),
		NO_OF_CYCLES(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Number Of Full charged/discharged cycles")),
		DESIGN_CAPACITY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE_HOURS) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Design Capacity Of the Module")),
		USEABLE_CAPACITY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE_HOURS) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Useable Cpacity Of The Module")),
		REMAINING_CAPACITY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE_HOURS) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Remaning Cpacity Of The Module")),
		BCU_MAX_CELL_VOLTAGE_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Max Cell Voltage Limit")),
		BCU_MIN_CELL_VOLTAGE_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BCU Min Cell Voltage Limit")),
		BMU_NUMBER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Bmu Number")),
		BMU_SOFTWARE_VERSION(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMU Software Version")),
		BMU_HARDWARE_VERSION(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMU Hardware Version")),
		CELL_1_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMU Cell Voltage of Block 1")),
		CELL_2_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMU Cell Voltage of Block 2")),
		CELL_3_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMU Cell Voltage of Block 3")),
		CELL_4_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMU Cell Voltage of Block 4")),
		CELL_5_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMU Cell Voltage of Block 5")),
		CELL_6_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMU Cell Voltage of Block 6")),
		CELL_7_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMU Cell Voltage of Block 7")),
		CELL_8_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMU Cell Voltage of Block 8")),
		CELL_9_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMU Cell Voltage of Block 9")),
		CELL_10_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMU Cell Voltage of Block 10")),
		CELL_11_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMU Cell Voltage of Block 11")),
		CELL_12_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMU Cell Voltage of Block 12")),
		CELL_13_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMU Cell Voltage of Block 13")),
		CELL_14_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMU Cell Voltage of Block 14")),
		CELL_15_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMU Cell Voltage of Block 15")),
		CELL_16_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMU Cell Voltage of Block 16")),
		BMU_TEMPERATURE_1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)//
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMU Temperature 1")),
		BMU_TEMPERATURE_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)//
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMU Temperature 2")),
		BMU_TEMPERATURE_3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)//
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMU Temperature 3")),
		BMU_TEMPERATURE_4(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)//
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMU Temperature 4")),
		BMU_TEMPERATURE_5(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)//
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMU Temperature 5")),
		BMU_TEMPERATURE_6(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)//
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMU Temperature 6")),
		BMU_TEMPERATURE_7(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)//
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMU Temperature 7")),
		BMU_TEMPERATURE_8(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)//
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMU Temperature 8")),
		BMU_CELL_1_BALANCING_STATUS(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMU Cell 1 Balancing Status ")),
		BMU_CELL_2_BALANCING_STATUS(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMU Cell 2 Balancing Status ")),
		BMU_CELL_3_BALANCING_STATUS(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMU Cell 3 Balancing Status ")),
		BMU_CELL_4_BALANCING_STATUS(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMU Cell 4 Balancing Status ")),
		BMU_CELL_5_BALANCING_STATUS(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMU Cell 5 Balancing Status ")),
		BMU_CELL_6_BALANCING_STATUS(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMU Cell 6 Balancing Status ")),
		BMU_CELL_7_BALANCING_STATUS(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMU Cell 7 Balancing Status ")),
		BMU_CELL_8_BALANCING_STATUS(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMU Cell 8 Balancing Status ")),
		BMU_CELL_9_BALANCING_STATUS(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMU Cell 9 Balancing Status ")),
		BMU_CELL_10_BALANCING_STATUS(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMU Cell 10 Balancing Status ")),
		BMU_CELL_11_BALANCING_STATUS(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMU Cell 11 Balancing Status ")),
		BMU_CELL_12_BALANCING_STATUS(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMU Cell 12 Balancing Status ")),
		BMU_CELL_13_BALANCING_STATUS(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMU Cell 13 Balancing Status ")),
		BMU_CELL_14_BALANCING_STATUS(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMU Cell 14 Balancing Status ")),
		BMU_CELL_15_BALANCING_STATUS(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMU Cell 15 Balancing Status ")),
		BMU_CELL_16_BALANCING_STATUS(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMU Cell 16 Balancing Status ")),
		BMU_MAX_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)//
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMU Max Cell Voltage")),
		BMU_MIN_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)//
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMU Min Cell Voltage")),
		BMU_MAX_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)//
				.accessMode(AccessMode.READ_WRITE) //
				.text("Max BMU Temperature")),
		BMU_MIN_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)//
				.accessMode(AccessMode.READ_WRITE) //
				.text("Min BMU Temperature")),
		SUM_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)//
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMU Sum Voltage")),
		BMS_CONTROL(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("BMS CONTROL(1: Shutdown 0:no action)")),
		KEEP_FET_OPEN(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Keep FET Open (Disconnect the relay; 1:Keep open , 0: normal operation)")),
		STATE_MACHINE(Doc.of(State.values()) //
				.text("Current State of State-Machine")), //
		RUN_FAILED(Doc.of(Level.FAULT) //
				.text("Running the Logic failed")), //
		MAX_START_ATTEMPTS(Doc.of(Level.FAULT) //
				.text("The maximum number of start attempts failed")), //
		MAX_STOP_ATTEMPTS(Doc.of(Level.FAULT) //
				.text("The maximum number of stop attempts failed")), //
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
