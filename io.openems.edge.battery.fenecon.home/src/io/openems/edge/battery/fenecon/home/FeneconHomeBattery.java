package io.openems.edge.battery.fenecon.home;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.fenecon.home.statemachine.StateMachine.State;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;

public interface FeneconHomeBattery extends Battery, OpenemsComponent, StartStoppable {

	/**
	 * Gets the Channel for {@link ChannelId#BMS_CONTROL}.
	 *
	 * @return the Channel
	 */
	public default Channel<Boolean> getBmsControlChannel() {
		return this.channel(ChannelId.BMS_CONTROL);
	}

	/**
	 * Gets the BmsControl, see {@link ChannelId#BMS_CONTROL}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getBmsControl() {
		return this.getBmsControlChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#BMS_CONTROL}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setBmsControl(Boolean value) {
		this.getBmsControlChannel().setNextValue(value);
	}

	/**
	 * Gets the target Start/Stop mode from config or StartStop-Channel.
	 *
	 * @return {@link StartStop}
	 */
	public StartStop getStartStopTarget();

	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		// Downgraded Info-Channels
		RACK_PRE_ALARM_CELL_UNDER_VOLTAGE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Cell Under Voltage Alarm")), //
		RACK_PRE_ALARM_UNDER_SOC(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Under SOC Alarm")), //
		ALARM_POSITION_BCU_1(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Alarm BCU 1 Position")), //
		RACK_LEVEL_1_UNDER_SOC(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Under SOC warning")), //
		WARNING_POSITION_BCU_1(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Warning BCU 1 Position")), //
		RACK_LEVEL_1_CELL_UNDER_VOLTAGE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Cell Under Voltage warning")), //

		RACK_PRE_ALARM_CELL_OVER_VOLTAGE(Doc.of(OpenemsType.BOOLEAN) //
				.text("Rack Cell Over Voltage Alarm")), //
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
		RACK_LEVEL_1_UNDER_SOH(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Under SOH warning")), //
		RACK_LEVEL_1_OVER_CHARGING_POWER(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Over Charging warning")), //
		RACK_LEVEL_1_OVER_DISCHARGING_POWER(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Over Discharging warning")), //
		RACK_LEVEL_2_CELL_OVER_VOLTAGE(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Cell Over Voltage Fault")), //
		RACK_LEVEL_2_CELL_UNDER_VOLTAGE(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Cell Under Voltage Fault")), //
		RACK_LEVEL_2_OVER_CHARGING_CURRENT(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Over Charging Current Fault")), //
		RACK_LEVEL_2_OVER_DISCHARGING_CURRENT(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Over Discharging Current Fault")), //
		RACK_LEVEL_2_OVER_TEMPERATURE(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Over Temperature Fault")), //
		RACK_LEVEL_2_UNDER_TEMPERATURE(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Under Temperature Fault")), //
		RACK_LEVEL_2_CELL_VOLTAGE_DIFFERENCE(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Cell Voltage Difference Fault")), //
		RACK_LEVEL_2_BCU_TEMP_DIFFERENCE(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack BCU Temp Difference Fault")), //
		RACK_LEVEL_2_CELL_TEMPERATURE_DIFFERENCE(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Cell Temperature Difference Fault")), //
		RACK_LEVEL_2_INTERNAL_COMMUNICATION(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Internal Communication Fault")), //
		RACK_LEVEL_2_EXTERNAL_COMMUNICATION(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack External Communication Fault")), //
		RACK_LEVEL_2_PRE_CHARGE_FAIL(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Pre Charge Fault")), //
		RACK_LEVEL_2_PARALLEL_FAIL(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Parallel Fault")), //
		RACK_LEVEL_2_SYSTEM_FAIL(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack System Fault")), //
		RACK_LEVEL_2_HARDWARE_FAIL(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Hardware Fault")), //

		// Alarm BCU Position
		ALARM_POSITION_BCU_2(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Alarm BCU 2 Position")), //
		ALARM_POSITION_BCU_3(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Alarm BCU 3 Position")), //
		ALARM_POSITION_BCU_4(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Alarm BCU 4 Position")), //
		ALARM_POSITION_BCU_5(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Alarm BCU 5 Position")), //
		ALARM_POSITION_BCU_6(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Alarm BCU 6 Position")), //
		ALARM_POSITION_BCU_7(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Alarm BCU 7 Position")), //
		ALARM_POSITION_BCU_8(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Alarm BCU 8 Position")), //
		ALARM_POSITION_BCU_9(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Alarm BCU 9 Position")), //
		ALARM_POSITION_BCU_10(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Alarm BCU 10 Position")), //

		// Warning BCU Position
		WARNING_POSITION_BCU_2(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Warning BCU 2 Position")), //
		WARNING_POSITION_BCU_3(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Warning BCU 3 Position")), //
		WARNING_POSITION_BCU_4(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Warning BCU 4 Position")), //
		WARNING_POSITION_BCU_5(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Warning BCU 5 Position")), //
		WARNING_POSITION_BCU_6(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Warning BCU 6 Position")), //
		WARNING_POSITION_BCU_7(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Warning BCU 7 Position")), //
		WARNING_POSITION_BCU_8(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Warning BCU 8 Position")), //
		WARNING_POSITION_BCU_9(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Warning BCU 9 Position")), //
		WARNING_POSITION_BCU_10(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Warning BCU 10 Position")), //

		// Fault BCU Position
		FAULT_POSITION_BCU_1(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Fault BCU 1 Position")), //
		FAULT_POSITION_BCU_2(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Fault BCU 2 Position")), //
		FAULT_POSITION_BCU_3(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Fault BCU 3 Position")), //
		FAULT_POSITION_BCU_4(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Fault BCU 4 Position")), //
		FAULT_POSITION_BCU_5(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Fault BCU 5 Position")), //
		FAULT_POSITION_BCU_6(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Fault BCU 6 Position")), //
		FAULT_POSITION_BCU_7(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Fault BCU 7 Position")), //
		FAULT_POSITION_BCU_8(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Fault BCU 8 Position")), //
		FAULT_POSITION_BCU_9(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Fault BCU 9 Position")), //
		FAULT_POSITION_BCU_10(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Fault BCU 10 Position")), //

		ID_OF_CELL_VOLTAGE_MIN(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Id. (Min Cell Voltage)")), //
		ID_OF_CELL_VOLTAGE_MAX(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Id. (Max Cell Voltage)")), //
		ID_OF_MIN_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Id. (Min Temp)")), //
		ID_OF_MAX_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Id. (Max Temp)")), //
		MAX_DC_CHARGE_CURRENT_LIMIT_PER_BCU(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Max Charge Current Limit Per BCU")), //
		MAX_DC_DISCHARGE_CURRENT_LIMIT_PER_BCU(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Max Discharge Current Limit Per BCU")),
		RACK_NUMBER_OF_BATTERY_BCU(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Count Of The Connected BCU")),
		RACK_NUMBER_OF_CELLS_IN_SERIES_PER_MODULE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Number Of Cells in  Series Per Module")),
		RACK_MAX_CELL_VOLTAGE_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Upper Cell Voltage Border -> System will stop charging if a cell reaches this voltage value")),
		RACK_MIN_CELL_VOLTAGE_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack Lower Cell Voltage Border -> System will stop discharging if a cell reaches this voltage value")),

		// Rack HW Fault Detail
		RACK_HW_AFE_COMMUNICATION_FAULT(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack HW AFE Communication Fault")),
		RACK_HW_ACTOR_DRIVER_FAULT(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack HW Actor Driver Fault")),
		RACK_HW_EEPROM_COMMUNICATION_FAULT(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack HW EEPROM Communication Fault")),
		RACK_HW_VOLTAGE_DETECT_FAULT(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack HW Voltage Detect Voltage")),
		RACK_HW_TEMPERATURE_DETECT_FAULT(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack HW Temperature Detect Fault")),
		RACK_HW_CURRENT_DETECT_FAULT(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack HW Current Detect Fault")),
		RACK_HW_ACTOR_NOT_CLOSE(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack HW Actor Not Close")),
		RACK_HW_ACTOR_NOT_OPEN(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack HW Actor Not Open")),
		RACK_HW_FUSE_BROKEN(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack HW Fuse Broken")),

		// Rack System Fault Detail
		RACK_SYSTEM_AFE_OVER_TEMPERATURE(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack System AFE Over Temperature")),
		RACK_SYSTEM_AFE_UNDER_TEMPERATURE(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack System AFE Under Temperature")),
		RACK_SYSTEM_AFE_OVER_VOLTAGE(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack System AFE Over Voltage")),
		RACK_SYSTEM_AFE_UNDER_VOLTAGE(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack System AFE Over Temperature")),
		RACK_SYSTEM_HIGH_TEMPERATURE_PERMANENT_FAILURE(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack System High Temperature Permanent Failure")),
		RACK_SYSTEM_LOW_TEMPERATURE_PERMANENT_FAILURE(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack System  Low Temperature Permanent Failure")),
		RACK_SYSTEM_HIGH_CELL_VOLTAGE_PERMANENT_FAILURE(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack System  High Cell Voltage Permanent Failure")),
		RACK_SYSTEM_LOW_CELL_VOLTAGE_PERMANENT_FAILURE(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack System  Low Cell Voltage Permanent Failure")),
		RACK_SYSTEM_SHORT_CIRCUIT(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Rack System Low Cell Voltage Permanent Failure")),
		UPPER_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("CV Point")),

		// BCU Status Flags
		STATUS_ALARM(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Status Alarm")),
		STATUS_WARNING(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Status WARNNG")),
		STATUS_FAULT(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Status BCU Status Fault")),
		STATUS_PFET(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Status Pre-Charge FET On/Off")),
		STATUS_CFET(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Status Charge FET On/Off")),
		STATUS_DFET(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Status Discharge FET On/Off")),
		STATUS_BATTERY_IDLE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Status Battery Idle")),
		STATUS_BATTERY_CHARGING(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Status Battery Charging")),
		STATUS_BATTERY_DISCHARGING(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Status Battery Discharging")),

		// Bcu Alarm Flags
		PRE_ALARM_CELL_OVER_VOLTAGE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Alarm Cell Over Voltage")),
		PRE_ALARM_CELL_UNDER_VOLTAGE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Alarm Cell Under Voltage")),
		PRE_ALARM_OVER_CHARGING_CURRENT(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Alarm Over Charging Current")),
		PRE_ALARM_OVER_DISCHARGING_CURRENT(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Alarm Over Discharging Current")),
		PRE_ALARM_OVER_TEMPERATURE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Alarm Over Temperature")),
		PRE_ALARM_UNDER_TEMPERATURE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Alarm Under Temperature")),
		PRE_ALARM_CELL_VOLTAGE_DIFFERENCE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Alarm Cell Voltage Difference")),
		PRE_ALARM_BCU_TEMP_DIFFERENCE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Alarm BCU Temperature Difference")),
		PRE_ALARM_UNDER_SOC(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Alarm Under SOC")),
		PRE_ALARM_UNDER_SOH(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Alarm Under SOH")),
		PRE_ALARM_OVER_CHARGING_POWER(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Alarm Over Charging Power")),
		PRE_ALARM_OVER_DISCHARGING_POWER(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Alarm Over Discharging Power")),

		// Bcu Warning Flags
		LEVEL_1_CELL_OVER_VOLTAGE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Warning Cell Over Voltage")),
		LEVEL_1_CELL_UNDER_VOLTAGE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Warning Cell Under Voltage")),
		LEVEL_1_OVER_CHARGING_CURRENT(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Warning Over Charging Current")),
		LEVEL_1_OVER_DISCHARGING_CURRENT(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Warning Over Discharging Current")),
		LEVEL_1_OVER_TEMPERATURE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Warning Over Temperature")),
		LEVEL_1_UNDER_TEMPERATURE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Warning Under Temperature")),
		LEVEL_1_CELL_VOLTAGE_DIFFERENCE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Warning Cell Voltage Difference")),
		LEVEL_1_BCU_TEMP_DIFFERENCE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Warning BCU Temperature Difference")),
		LEVEL_1_UNDER_SOC(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Warning Under SOC")),
		LEVEL_1_UNDER_SOH(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Warning Under SOH")),
		LEVEL_1_OVER_CHARGING_POWER(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Warning Over Charging Power")),
		LEVEL_1_OVER_DISCHARGING_POWER(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Warning Over Discharging Power")),

		// Bcu Fault Flags
		LEVEL_2_CELL_OVER_VOLTAGE(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Fault Cell Over Voltage")),
		LEVEL_2_CELL_UNDER_VOLTAGE(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Fault Cell Under Voltage")),
		LEVEL_2_OVER_CHARGING_CURRENT(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Fault Over Charging Current")),
		LEVEL_2_OVER_DISCHARGING_CURRENT(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Fault Over Discharging Current")),
		LEVEL_2_OVER_TEMPERATURE(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Fault Over Temperature")),
		LEVEL_2_UNDER_TEMPERATURE(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Fault Under Temperature")),
		LEVEL_2_CELL_VOLTAGE_DIFFERENCE(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Fault Cell Voltage Difference")),
		LEVEL_2_BCU_TEMP_DIFFERENCE(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Fault BCU Temperature Difference")),
		LEVEL_2_TEMPERATURE_DIFFERENCE(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Fault BCU Temperature Difference")),
		LEVEL_2_INTERNAL_COMMUNICATION(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Fault Internal Communication")),
		LEVEL_2_EXTERNAL_COMMUNICATION(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Fault External Communication")),
		LEVEL_2_PRECHARGE_FAIL(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Fault Pre-Charge Fail")),
		LEVEL_2_PARALLEL_FAIL(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Fault Parallel Fail")),
		LEVEL_2_SYSTEM_FAIL(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Fault System Fault")),
		LEVEL_2_HARDWARE_FAIL(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU Fault Hardware Fault")),

		// Bcu HW Fault Detail
		HW_AFE_COMMUNICAITON_FAULT(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU HW AFE Communication Fault")),
		HW_ACTOR_DRIVER_FAULT(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU HW Actor Driver Fault")),
		HW_EEPROM_COMMUNICATION_FAULT(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU HW EEPROM Communication Fault")),
		HW_VOLTAGE_DETECT_FAULT(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU HW Voltage Detect Fault")),
		HW_TEMPERATURE_DETECT_FAULT(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU HW Temperaure Detect Fault")),
		HW_CURRENT_DETECT_FAULT(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU HW Current Detect Fault")),
		HW_ACTOR_NOT_CLOSE(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU HW Actor Not Close Fault")),
		HW_ACTOR_NOT_OPEN(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU HW Actor Not Open")),
		HW_FUSE_BROKEN(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU HW Fuse Broken Fault")),

		// Bcu System Fault Detail
		SYSTEM_AFE_OVER_TEMPERATURE(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY)//
				.text("BCU System AFE Over Temperature Fault")),
		SYSTEM_AFE_UNDER_TEMPERATURE(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU System AFE Under Temperature Fault")),
		SYSTEM_AFE_OVER_VOLTAGE(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU System AFE Over Voltage Fault")),
		SYSTEM_AFE_UNDER_VOLTAGE(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU System AFE Under Voltage Fault")),
		SYSTEM_HIGH_TEMPERATURE_PERMANENT_FAILURE(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU System High Temperature Permanent Fault")),
		SYSTEM_LOW_TEMPERATURE_PERMANENT_FAILURE(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU System Low Temperature Permanent Fault")),
		SYSTEM_HIGH_CELL_VOLTAGE_PERMANENT_FAILURE(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU System High Cell Voltage Permanent Fault")),
		SYSTEM_LOW_CELL_VOLTAGE_PERMANENT_FAILURE(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU System Low Cell Voltage Permanent Fault")),
		BCU_SYSTEM_LOW_CELL_VOLTAGE_FAILURE(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU System Low Cell Voltage Permanent Fault")),
		SYSTEM_SHORT_CIRCUIT(Doc.of(Level.WARNING) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("BCU System Short Circuit Fault")),

		NUMBER_OF_MODULES_PER_TOWER(new IntegerDoc() //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Number of modules per tower") //
				.<FeneconHomeBatteryImpl>onValueChange(FeneconHomeBatteryImpl::updateNumberOfTowersAndModules)),

		NUMBER_OF_TOWERS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Number of towers of the built system")),

		TOWER_2_BMS_SOFTWARE_VERSION(new IntegerDoc() //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Bms software version of third tower") //
				.<FeneconHomeBatteryImpl>onValueChange(FeneconHomeBatteryImpl::updateNumberOfTowersAndModules)),

		TOWER_1_BMS_SOFTWARE_VERSION(new IntegerDoc() //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Bms software version of second tower") //
				.<FeneconHomeBatteryImpl>onValueChange(FeneconHomeBatteryImpl::updateNumberOfTowersAndModules)),

		TOWER_0_BMS_SOFTWARE_VERSION(new IntegerDoc() //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Bms software version of first tower")),

		BMS_CONTROL(Doc.of(OpenemsType.BOOLEAN) //
				.text("BMS CONTROL(1: Shutdown, 0: no action)")),
		STATE_MACHINE(Doc.of(State.values()) //
				.text("Current State of State-Machine")), //
		RUN_FAILED(Doc.of(Level.FAULT) //
				.text("Running the Logic failed")), //
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
