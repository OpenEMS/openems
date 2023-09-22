package io.openems.edge.battery.fenecon.f2b;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.fenecon.f2b.common.enums.F2bCanCommunication;
import io.openems.edge.battery.fenecon.f2b.common.enums.F2bState;
import io.openems.edge.battery.fenecon.f2b.common.enums.F2bTerminal15Sw;
import io.openems.edge.battery.fenecon.f2b.common.enums.F2bWatchdogState;
import io.openems.edge.common.channel.AbstractChannelListenerManager;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.value.Value;

public interface BatteryFeneconF2b extends Battery {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		// Write Channels
		F2B_CAN_COMMUNICATION(Doc.of(F2bCanCommunication.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		F2B_ERROR_RESET_REQUEST(Doc.of(OpenemsType.BOOLEAN)//
				.accessMode(AccessMode.READ_WRITE)), //

		F2B_TERMINAL_15_HW(Doc.of(OpenemsType.BOOLEAN)//
				.accessMode(AccessMode.READ_WRITE)//
				.text("Switch on and off terminal 15 HW ignition of the battery")), //

		F2B_TERMINAL_30C(Doc.of(OpenemsType.BOOLEAN)//
				.accessMode(AccessMode.READ_WRITE)//
				.text("Switch on and off terminal 30C (crash / emergency stop) of the battery")), //

		// ST_KL, The value for this signal should match the terminal 15 (S_KL15_WUP)
		F2B_TERMINAL_15_SW(Doc.of(F2bTerminal15Sw.values())//
				.accessMode(AccessMode.READ_WRITE)), //

		// F2B_T15_TOGGLE_REQUEST
		F2B_TERMINAL_15_TOGGLE_REQUEST(Doc.of(OpenemsType.BOOLEAN)//
				.accessMode(AccessMode.READ_WRITE)), //

		F2B_RESET(Doc.of(OpenemsType.BOOLEAN)//
				.accessMode(AccessMode.READ_WRITE)), //

		// Actual state of the F2B internal state machine
		F2B_STATE(Doc.of(F2bState.values())//
				.accessMode(AccessMode.READ_ONLY)), //

		// If its in running state, cat failure occurs
		F2B_T30C_NO_INPUT_VOLTAGE(Doc.of(Level.INFO)//
				.text("F2B Terminal 30 C no input voltage") //
				.accessMode(AccessMode.READ_ONLY)), //

		F2B_T30C_OUTPUT_ERROR(Doc.of(Level.FAULT)//
				.text("F2B Terminal 30 C output error") //
				.accessMode(AccessMode.READ_ONLY)), //

		// F2B_T15_HARDWARE_ERROR, Hardware error of T15 output (to battery)
		F2B_TERMINAL_15_HW_ERROR(Doc.of(Level.FAULT)//
				.text("F2B Terminal 15 HW error") //
				.accessMode(AccessMode.READ_ONLY)), //

		// Error of HV Power Supply → T15, CAN, … not working
		F2B_POWER_SUPPLY_ERROR_HV_SIDE(Doc.of(Level.FAULT)//
				.text("F2B power supply error HV side") //
				.accessMode(AccessMode.READ_ONLY)), //

		// F2B_T30F_INPUT_VOLTAGE, Voltage measurement of T30F input
		F2B_TERMINAL_30F_INPUT_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIVOLT)//
				.accessMode(AccessMode.READ_ONLY)), // [0 ...30 V]

		// F2B_T30C_INPUT_VOLTAGE, Voltage measurement of T30C input
		F2B_TERMINAL_30C_INPUT_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIVOLT)//
				.accessMode(AccessMode.READ_ONLY)), //

		// Time in seconds since 1.1.2016 [0:00:00]
		F2B_TIMESTAMP(Doc.of(OpenemsType.LONG)//
				.unit(Unit.CUMULATED_SECONDS)//
				.accessMode(AccessMode.READ_ONLY)), //

		// F2B_FEMS_WATCHDOG_STATE
		F2B_WATCHDOG_STATE(Doc.of(F2bWatchdogState.values())//
				.accessMode(AccessMode.READ_ONLY)), //

		// T_SEC_COU_REL
		F2B_WATCHDOG_TIMER_VALUE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.SECONDS)//
				.accessMode(AccessMode.READ_ONLY)), //
		COOLING_VALVE_STATE(Doc.of(OpenemsType.BOOLEAN)//
				.accessMode(AccessMode.READ_ONLY)), //
		AVG_CELL_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //
		EMERGENCY_ACKNOWLEDGE(Doc.of(Level.WARNING)//
				.text("Waiting for acknowledge to exit the emergency state!")), //

		// Internal Voltage of the battery.This value is more accurate than
		// LINK_VOLTAGE in case the contactors are closed
		INTERNAL_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT)//
				.accessMode(AccessMode.READ_ONLY)), //
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
	 * Gets the Channel for {@link ChannelId#F2B_STATE}.
	 *
	 * @return the Channel
	 */
	public default Channel<F2bState> getF2bStateChannel() {
		return this.channel(ChannelId.F2B_STATE);
	}

	/**
	 * Gets the F2bState, see {@link ChannelId#F2B_STATE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default F2bState getF2bState() {
		return this.getF2bStateChannel().value().asEnum();
	}

	/**
	 * Gets the Channel for {@link ChannelId#F2B_TERMINAL_15_HW}.
	 *
	 * @return the Channel
	 */
	public default WriteChannel<Boolean> getF2bTerminal15HwChannel() {
		return this.channel(ChannelId.F2B_TERMINAL_15_HW);
	}

	/**
	 * Gets the F2bTerminal15Hw, see {@link ChannelId#F2B_TERMINAL_15_HW}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getF2bTerminal15Hw() {
		return this.getF2bTerminal15HwChannel().value();
	}

	/**
	 * Writes the value to the {@link ChannelId#F2B_TERMINAL_15_HW} Register.
	 *
	 * @param value the next value
	 * @throws OpenemsNamedException on error
	 */
	public default void setF2bTerminal15Hw(boolean value) throws OpenemsNamedException {
		this.getF2bTerminal15HwChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#F2B_CAN_COMMUNICATION}.
	 *
	 * @return the Channel
	 */
	public default WriteChannel<F2bCanCommunication> getF2bCanCommunicationChannel() {
		return this.channel(ChannelId.F2B_CAN_COMMUNICATION);
	}

	/**
	 * Gets the F2bCanCommunication, see {@link ChannelId#F2B_CAN_COMMUNICATION}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<F2bCanCommunication> getF2bCanCommunication() {
		return this.getF2bCanCommunicationChannel().value();
	}

	/**
	 * Writes the value to the {@link ChannelId#F2B_CAN_COMMUNICATION} Register.
	 *
	 * @param value the next value
	 * @throws OpenemsNamedException on error
	 */
	public default void setF2bCanCommunication(F2bCanCommunication value) throws OpenemsNamedException {
		this.getF2bCanCommunicationChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#F2B_TERMINAL_30C}.
	 *
	 * @return the Channel
	 */
	public default WriteChannel<Boolean> getF2bTerminal30cChannel() {
		return this.channel(ChannelId.F2B_TERMINAL_30C);
	}

	/**
	 * Gets the F2bTerminal30c, see {@link ChannelId#F2B_TERMINAL_30C}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getF2bTerminal30c() {
		return this.getF2bTerminal30cChannel().value();
	}

	/**
	 * Writes the value to the {@link ChannelId#F2B_TERMINAL_30C} Register.
	 *
	 * @param value the next value
	 * @throws OpenemsNamedException on error
	 */
	public default void setF2bTerminal30c(boolean value) throws OpenemsNamedException {
		this.getF2bTerminal30cChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#F2B_TERMINAL_15_TOGGLE_REQUEST}.
	 *
	 * @return the Channel
	 */
	public default WriteChannel<Boolean> getF2bTerminal15ToggleRequestChannel() {
		return this.channel(ChannelId.F2B_TERMINAL_15_TOGGLE_REQUEST);
	}

	/**
	 * Writes the value to the {@link ChannelId#F2B_TERMINAL_15_TOGGLE_REQUEST}
	 * Register.
	 *
	 * @param value the next value
	 * @throws OpenemsNamedException on error
	 */
	public default void setF2bTerminal15ToggleRequest(Boolean value) throws OpenemsNamedException {
		this.getF2bTerminal15ToggleRequestChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#F2B_RESET}.
	 *
	 * @return the Channel
	 */
	public default WriteChannel<Boolean> getF2bResetChannel() {
		return this.channel(ChannelId.F2B_RESET);
	}

	/**
	 * Writes the value to the {@link ChannelId#F2B_RESET} Register.
	 *
	 * @param value the next value
	 * @throws OpenemsNamedException on error
	 */
	public default void setF2bReset(Boolean value) throws OpenemsNamedException {
		this.getF2bResetChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#F2B_TERMINAL_15_SW}.
	 *
	 * @return the Channel {@link Channel}
	 */
	public default WriteChannel<F2bTerminal15Sw> getF2bTerminal15SwChannel() {
		return this.channel(ChannelId.F2B_TERMINAL_15_SW);
	}

	/**
	 * Writes the value to the {@link ChannelId#F2B_TERMINAL_15_SW} Register.
	 *
	 * @param value the next value
	 * @throws OpenemsNamedException on error
	 */
	public default void setF2bTerminal15Sw(F2bTerminal15Sw value) throws OpenemsNamedException {
		this.getF2bTerminal15SwChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#AVG_CELL_TEMPERATURE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getAvgCellTemperatureChannel() {
		return this.channel(ChannelId.AVG_CELL_TEMPERATURE);
	}

	/**
	 * Gets the Average Cell Temperature. See
	 * {@link ChannelId#AVG_CELL_TEMPERATURE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getAvgCellTemperature() {
		return this.getAvgCellTemperatureChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#COOLING_VALVE_STATE}.
	 *
	 * @return the Channel {@link Channel}
	 */
	public default Channel<Boolean> getCoolingValveStateChannel() {
		return this.channel(ChannelId.COOLING_VALVE_STATE);
	}

	/**
	 * Gets the {@link ChannelId#COOLING_VALVE_STATE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getCoolingValveState() {
		return this.getCoolingValveStateChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#F2B_T30C_NO_INPUT_VOLTAGE}.
	 *
	 * @return the Channel {@link Channel}
	 */
	public default Channel<Boolean> getF2bT30cNoInputVoltageChannel() {
		return this.channel(ChannelId.F2B_T30C_NO_INPUT_VOLTAGE);
	}

	/**
	 * Gets the {@link ChannelId#F2B_T30C_NO_INPUT_VOLTAGE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getF2bT30cNoInputVoltage() {
		return this.getF2bT30cNoInputVoltageChannel().value();
	}

	/*
	 * Gets the Channel for {@link ChannelId#EMERGENCY_ACKNOWLEDGE}.
	 * 
	 * @return the Channel {@link Channel}
	 */
	public default Channel<Boolean> getEmergencyAcknowledgeChannel() {
		return this.channel(ChannelId.EMERGENCY_ACKNOWLEDGE);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#EMERGENCY_ACKNOWLEDGE} Channel.
	 *
	 * @param value the next {@link Boolean} value
	 */
	public default void _setEmergencyAcknowledge(boolean value) {
		this.getEmergencyAcknowledgeChannel().setNextValue(value);
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
	 * Gets the {@link StateChannel} for {@link ChannelId#SET_INTERNAL_VOLTAGE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getInternalVoltage() {
		return this.getInternalVoltageChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#SET_INTERNAL_VOLTAGE} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setInternalVoltage(Integer value) {
		this.getInternalVoltageChannel().setNextValue(value);
	}

	/**
	 * Sets the High Voltage Contactor to an unlocked state. This method is used to
	 * indicate whether the HV Contactor switch can be unlocked or not.
	 * 
	 * <p>
	 * This method allows unlocking the HV contactor, which is an electrical switch
	 * used to control the flow of battery current.
	 * </p>
	 * 
	 * <p>
	 * When the HV contactor is unlocked, it lets the battery operate on
	 * {@link Battery.ChannelId#HV_CONTACTOR}. And when the HV contactor turned on,
	 * it enables the flow of current through the circuit.
	 * </p>
	 * 
	 * @param value {@code true} if the HV contactor switch can be unlocked,
	 *              {@code false} otherwise.
	 */
	public void setHvContactorUnlocked(boolean value);

	/**
	 * Gets the device specific implemented applications as callback functions with
	 * their {@link ChannelIds}. Callback functions will be triggered once the given
	 * Channel values changed.
	 * 
	 * <p>
	 * The used callback function method here is
	 * {@link AbstractChannelListenerManager#addOnChangeListener}
	 * </p>
	 * 
	 * @return DeviceSpecificOnChangeHandler which gets a type of any class which
	 *         extends {@link BatteryFeneconF2b}
	 */
	public DeviceSpecificOnChangeHandler<? extends BatteryFeneconF2b> getDeviceSpecificOnChangeHandler();

}