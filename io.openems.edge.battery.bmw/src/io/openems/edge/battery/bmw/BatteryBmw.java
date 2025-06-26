package io.openems.edge.battery.bmw;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.bmw.enums.BatteryState;
import io.openems.edge.battery.bmw.enums.BatteryStateCommand;
import io.openems.edge.battery.bmw.statemachine.GoRunningSubState;
import io.openems.edge.battery.bmw.statemachine.StateMachine.State;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;

public interface BatteryBmw extends Battery, ModbusComponent, OpenemsComponent, StartStoppable {

	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		STATE_MACHINE(Doc.of(State.values()) //
				.text("Current state of State-Machine")), //
		RUN_FAILED(Doc.of(Level.FAULT) //
				.text("Running the Logic failed")), //
		ERROR_BATTERY_TYPE(Doc.of(Level.FAULT) //
				.text("Configuring the Battery Type not successful!")), //
		UNEXPECTED_STOPPED_STATE(Doc.of(Level.FAULT) //
				.text("Unexpected battery state in \"Stopped\"!")),
		UNEXPECTED_RUNNING_STATE(Doc.of(Level.FAULT) //
				.text("Unexpected battery state in \"Running\"!")),
		SOC_RAW_VALUE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.THOUSANDTH)//
				.accessMode(AccessMode.READ_ONLY)), //
		TIMEOUT_START_BATTERY(Doc.of(Level.FAULT) //
				.text("The maximum start time is passed")), //
		TIMEOUT_STOP_BATTERY(Doc.of(Level.FAULT) //
				.text("The maximum stop time is passed")), //
		GO_RUNNING_STATE_MACHINE(Doc.of(GoRunningSubState.values()) //
				.text("Current State of GoRunning State-Machine")), //
		/*
		 * ErrBits1
		 */

		UNSPECIFIED_ERROR(Doc.of(Level.FAULT) //
				.text("Unspecified Error - Cell-config-Error, Slave-count-Error")), //

		LOW_VOLTAGE_ERROR(Doc.of(Level.FAULT) //
				.text("Low Voltage Error - Cell voltage minimal")), //

		HIGH_VOLTAGE_ERROR(Doc.of(Level.FAULT) //
				.text("High Voltage Error - Cell voltage maximal")), //

		CHARGE_CURRENT_ERROR(Doc.of(Level.FAULT) //
				.text("Charge Current Error - Imax-HW, Imax-SW, I-High (e.g. current dependend on temperature")), //

		DISCHARGE_CURRENT_ERROR(Doc.of(Level.FAULT) //
				.text("Discharge Current Error - Imax-HW, Imax-SW, I-High (e.g. current dependend on temperature")), //

		CHARGE_POWER_ERROR(Doc.of(Level.FAULT) //
				.text("Charge Power Error")), //

		DISCHARGE_POWER_ERROR(Doc.of(Level.FAULT) //
				.text("Discharge Power Error")), //

		LOW_SOC_ERROR(Doc.of(Level.FAULT) //
				.text("Low SOC Error")), //

		HIGH_SOC_ERROR(Doc.of(Level.FAULT) //
				.text("High SOC Error")), //

		LOW_TEMPERATURE_ERROR(Doc.of(Level.FAULT) //
				.text("Low Temperature Error - Cell temperature minimal")), //

		HIGH_TEMPERATURE_ERROR(Doc.of(Level.FAULT) //
				.text("High Temperature Error - Cell temperature maximal")), //

		INSULATION_ERROR(Doc.of(Level.FAULT) //
				.text("Insulation Error - I-Diff error (self test error, I-Diff > |300 mA|)")), //

		CONTACTOR_ERROR(Doc.of(Level.FAULT) //
				.text("Contactor Error (contactor feedback signals")), //

		SENSOR_ERROR(Doc.of(Level.FAULT) //
				.text("Sensor Error - Current sensor error")), //

		IMBALANCE_ERROR(Doc.of(Level.FAULT) //
				.text("Imbalance Error - Static and dynamic cell imbalance (voltage)")), //

		COMMUNICATION_ERROR(Doc.of(Level.FAULT) //
				.text("Communication Error - Batcom Error (Timeout), Master-Slave Can Error (Timeout)")), //

		/*
		 * ErrBits2
		 */

		CONTAINER_ERROR(Doc.of(Level.FAULT) //
				.text("Container/(Room) Error")), //

		SOH_ERROR(Doc.of(Level.FAULT) //
				.text("SOH Error")), //

		RACK_STING_ERROR(Doc.of(Level.FAULT) //
				.text("Rack/String Error")), //

		/*
		 * WarnBits1
		 */

		UNSPECIFIED_WARNING(Doc.of(Level.WARNING) //
				.text("Unspecified Warning - Cell-config-Error, Slave-count-Error")), //

		LOW_VOLTAGE_WARNING(Doc.of(Level.WARNING) //
				.text("Low Voltage Error - Cell voltage high")), //

		HIGH_VOLTAGE_WARNING(Doc.of(Level.WARNING) //
				.text("High Voltage Warning - Cell voltage high")), //

		CHARGE_CURRENT_WARNING(Doc.of(Level.WARNING) //
				.text("Charge Current Warning - Imax-HW, Imax-SW, I-High (e.g. current dependend on temperature")), //

		DISCHARGE_CURRENT_WARNING(Doc.of(Level.WARNING) //
				.text("Discharge Current Warning - Imax-HW, Imax-SW, I-High (e.g. current dependend on temperature")), //

		CHARGE_POWER_WARNING(Doc.of(Level.WARNING) //
				.text("Charge Power Warning")), //

		DISCHARGE_POWER_WARNING(Doc.of(Level.WARNING) //
				.text("Discharge Power Warning")), //

		LOW_SOC_WARNING(Doc.of(Level.WARNING) //
				.text("Low SOC Warning")), //

		HIGH_SOC_WARNING(Doc.of(Level.WARNING) //
				.text("High SOC Warning")), //

		LOW_TEMPERATURE_WARNING(Doc.of(Level.WARNING) //
				.text("Low Temperature Warning - Cell temperature high")), //

		HIGH_TEMPERATURE_WARNING(Doc.of(Level.WARNING) //
				.text("High Temperature Warning - Cell temperature high")), //

		INSULATION_WARNING(Doc.of(Level.WARNING) //
				.text("Insulation Warning - I-Diff error (self test error, I-Diff > |300 mA|)")), //

		CONTACTOR_WARNING(Doc.of(Level.WARNING) //
				.text("Contactor Warning (contactor feedback signals")), //

		SENSOR_WARNING(Doc.of(Level.WARNING) //
				.text("Sensor Warning - Current sensor error")), //

		IMBALANCE_WARNING(Doc.of(Level.WARNING) //
				.text("Imbalance Warning - Static and dynamic cell imbalance (voltage)")), //

		COMMUNICATION_WARNING(Doc.of(Level.WARNING) //
				.text("Communication Warning - Batcom Error (Timeout), Master-Slave Can Error (Timeout)")), //

		// WarnBits2
		CONTAINER_WARNING(Doc.of(Level.WARNING) //
				.text("Container/(Room) Warning")), //

		SOH_WARNING(Doc.of(Level.WARNING) //
				.text("SOH Warning")), //

		RACK_STING_WARNING(Doc.of(Level.WARNING) //
				.text("Rack/String Warning - min. 1 string is in error condition (disconnected)")), //

		// Read / write channels
		BATTERY_STATE_COMMAND(Doc.of(BatteryStateCommand.values()) //
				.accessMode(AccessMode.READ_WRITE)), //

		BATTERY_STATE(Doc.of(BatteryState.values()) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH)), //

		MAX_OPERATING_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.AMPERE) //
				.text("Absolute maximum operating (max. discharge current) current of battery")), //

		MIN_OPERATING_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.AMPERE) //
				.text("Absolute minimum operating current (max. charge current) of battery")), //

		MAX_DYNAMIC_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.DEZIVOLT)), //

		MIN_DYNAMIC_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.DEZIVOLT)), //

		CONNECTED_STRING_NUMBER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //

		INSTALLED_STRING_NUMBER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //

		BATTERY_TOTAL_SOC(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.TENTHOUSANDTH) //
				.text("Battery state of charge calculated for all strings, which are available (connected and not connected)")), //

		BATTERY_SOC(Doc.of(OpenemsType.DOUBLE) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.TENTHOUSANDTH) //
				.onChannelChange(BatteryBmwImpl::updateSoc)), //

		REMAINING_CHARGE_CAPACITY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.AMPERE_HOURS)), //

		REMAINING_DISCHARGE_CAPACITY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.AMPERE_HOURS) //
				.text("Remaining discharge capacity - Ah possible to charge from now on")), //

		REMANING_CHARGE_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.KILOWATT_HOURS) //
				.text("Remaining energy to charge")), //

		REMANING_DISCHARGE_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.KILOWATT_HOURS) //
				.text("Remaining energy to discharge")), //

		NOMINAL_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.KILOWATT_HOURS) //
				.text("Battery Nominal Energy (connected Racks)")), //

		NOMINAL_ENERGY_TOTAL(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.KILOWATT_HOURS) //
				.text("Battery Nominal Energy (all Racks)")), //

		NOMINAL_CAPACITY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.AMPERE_HOURS) //
				.text("Battery Nominal Capacity (connected Racks)")), //

		// External voltage (at DC connector) of the battery
		LINK_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.DEZIVOLT) //
				.onChannelChange(BatteryBmwImpl::updateVoltage)), //

		INTERNAL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.DEZIVOLT) //
				.onChannelChange(BatteryBmwImpl::updateVoltage)), //

		AVG_BATTERY_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //

		MIN_BATTERY_TEMPERATURE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEZIDEGREE_CELSIUS)), //

		MAX_BATTERY_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //

		INSULATION_RESISTANCE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.KILOOHM) //
				.text("Insulation Resistanc")), //

		DISCHARGE_MAX_CURRENT_HIGH_RESOLUTION(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.DEZIAMPERE) //
				.text("Battery maximum limit dynamic current (max. discharge current)")), //

		CHARGE_MAX_CURRENT_HIGH_RESOLUTION(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.DEZIAMPERE) //
				.text("Battery minimum limit dynamic current (max. charge current)")), //

		FULL_CYCLE_COUNT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Battery Full Cycles Count - count complete energy throughputs")), //

		// actual not implemented @ BCS side
		HEART_BEAT(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_ONLY) //
				.text("Life sign signal")), //

		AVG_CELL_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //
		; //

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
	 * Gets the Channel for {@link ChannelId#STATE_MACHINE}.
	 *
	 * @return the Channel
	 */
	public default Channel<State> getStateMachineChannel() {
		return this.channel(ChannelId.STATE_MACHINE);
	}

	/**
	 * Gets the {@link StateChannel} for {@link ChannelId#STATE_MACHINE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<State> getStateMachine() {
		return this.getStateMachineChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#STATE_MACHINE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setStateMachine(State value) {
		this.getStateMachineChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#RUN_FAILED}.
	 *
	 * @return the Channel
	 */
	public default Channel<Boolean> getRunFailedChannel() {
		return this.channel(ChannelId.RUN_FAILED);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#RUN_FAILED}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setRunFailed(boolean value) {
		this.getRunFailedChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#UNEXPECTED_STOPPED_STATE}.
	 * 
	 * @return the Channel
	 */
	public default StateChannel getUnexpectedStoppedStateChannel() {
		return this.channel(ChannelId.UNEXPECTED_STOPPED_STATE);
	}

	/**
	 * Gets the {@link StateChannel} for {@link ChannelId#UNEXPECTED_STOPPED_STATE}.
	 * 
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getUnexpectedStoppedState() {
		return this.getUnexpectedStoppedStateChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#UNEXPECTED_STOPPED_STATE} Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setUnexpectedStoppedState(boolean value) {
		this.getUnexpectedStoppedStateChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#UNEXPECTED_RUNNING_STATE}.
	 * 
	 * @return the Channel
	 */
	public default StateChannel getUnexpectedRunningStateChannel() {
		return this.channel(ChannelId.UNEXPECTED_RUNNING_STATE);
	}

	/**
	 * Gets the {@link StateChannel} for {@link ChannelId#UNEXPECTED_RUNNING_STATE}.
	 * 
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getUnexpectedRunningState() {
		return this.getUnexpectedRunningStateChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#UNEXPECTED_RUNNING_STATE} Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setUnexpectedRunningState(boolean value) {
		this.getUnexpectedRunningStateChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#TIMEOUT_START_BATTERY}.
	 * 
	 * @return the Channel
	 */
	public default StateChannel getTimeoutStartBatteryChannel() {
		return this.channel(ChannelId.TIMEOUT_START_BATTERY);
	}

	/**
	 * Gets the {@link StateChannel} for {@link ChannelId#TIMEOUT_START_BATTERY}.
	 * 
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getTimeoutStartBattery() {
		return this.getTimeoutStartBatteryChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#TIMEOUT_START_BATTERY} Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setTimeoutStartBattery(Boolean value) {
		this.getTimeoutStartBatteryChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#TIMEOUT_STOP_BATTERY}.
	 * 
	 * @return the Channel
	 */
	public default StateChannel getTimeoutStopBatteryChannel() {
		return this.channel(ChannelId.TIMEOUT_STOP_BATTERY);
	}

	/**
	 * Gets the {@link StateChannel} for {@link ChannelId#TIMEOUT_STOP_BATTERY}.
	 * 
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getTimeoutStopBattery() {
		return this.getTimeoutStopBatteryChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#TIMEOUT_STOP_BATTERY} Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setTimeoutStopBattery(Boolean value) {
		this.getTimeoutStopBatteryChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#BATTERY_STATE_COMMAND}.
	 *
	 * @return the Channel {@link Channel}
	 */
	public default WriteChannel<BatteryStateCommand> getBatteryStateCommandChannel() {
		return this.channel(ChannelId.BATTERY_STATE_COMMAND);
	}

	/**
	 * See {@link ChannelId#BATTERY_STATE_COMMAND}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<BatteryStateCommand> getBatteryStateCommand() {
		return this.getBatteryStateCommandChannel().value();
	}

	/**
	 * See {@link ChannelId#BATTERY_STATE_COMMAND}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setBatteryStateCommand(BatteryStateCommand value) throws OpenemsNamedException {
		this.getBatteryStateCommandChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#BATTERY_STATE}.
	 *
	 * @return the Channel {@link Channel}
	 */
	public default Channel<BatteryState> getBatteryStateChannel() {
		return this.channel(ChannelId.BATTERY_STATE);
	}

	/**
	 * See {@link ChannelId#BATTERY_STATE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<BatteryState> getBatteryState() {
		return this.getBatteryStateChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#LINK_VOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getLinkVoltageChannel() {
		return this.channel(ChannelId.LINK_VOLTAGE);
	}

	/**
	 * Gets the LinkVoltage, see {@link ChannelId#LINK_VOLTAGE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getLinkVoltage() {
		return this.getLinkVoltageChannel().value();
	}

	/*
	 * Gets the Channel for {@link ChannelId#INTERNAL_VOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getInternalVoltageChannel() {
		return this.channel(ChannelId.INTERNAL_VOLTAGE);
	}

	/**
	 * Gets the {@link StateChannel} for {@link ChannelId#INTERNAL_VOLTAGE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getInternalVoltage() {
		return this.getInternalVoltageChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#GO_RUNNING_STATE_MACHINE}.
	 * 
	 * @return the Channel
	 */
	public default Channel<GoRunningSubState> getGoRunningStateMachineChannel() {
		return this.channel(ChannelId.GO_RUNNING_STATE_MACHINE);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#GO_RUNNING_STATE_MACHINE} Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setGoRunningStateMachine(GoRunningSubState value) {
		this.getGoRunningStateMachineChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SOC_RAW_VALUE}.
	 * 
	 * @return the Channel
	 */
	public default Channel<Integer> getSocRawValueChannel() {
		return this.channel(ChannelId.SOC_RAW_VALUE);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#SOC_RAW_VALUE}
	 * Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setSocRawValue(int value) {
		this.getSocRawValueChannel().setNextValue(value);
	}

	/**
	 * Gets the target Start/Stop mode from config or StartStop-Channel.
	 * 
	 * @return {@link StartStop}
	 */
	public StartStop getStartStopTarget();
}