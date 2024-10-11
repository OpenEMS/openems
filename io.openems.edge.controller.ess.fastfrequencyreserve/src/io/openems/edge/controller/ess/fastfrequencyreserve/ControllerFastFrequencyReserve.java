package io.openems.edge.controller.ess.fastfrequencyreserve;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.ess.fastfrequencyreserve.enums.ActivationTime;
import io.openems.edge.controller.ess.fastfrequencyreserve.enums.ControlMode;
import io.openems.edge.controller.ess.fastfrequencyreserve.enums.SupportDuration;
import io.openems.edge.controller.ess.fastfrequencyreserve.statemachine.StateMachine.State;

public interface ControllerFastFrequencyReserve extends Controller, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		CONTROL_MODE(Doc.of(ControlMode.values()) //
				.initialValue(ControlMode.MANUAL_OFF) //
				.text("Configured Control Mode")), //
		STATE_MACHINE(Doc.of(State.values()) //
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Current State of State-Machine")), //
		SCHEDULE_PARSE_FAILED(Doc.of(Level.FAULT) //
				.text("Unable to parse Schedule")), //
		NO_ACTIVE_SETPOINT(Doc.of(OpenemsType.BOOLEAN) //
				.text("No active Set-Point given")), //
		DISCHARGE_POWER_SET_POINT(Doc.of(OpenemsType.INTEGER) //
				.persistencePriority(PersistencePriority.HIGH)//
				.accessMode(AccessMode.READ_WRITE)),
		NO_FREQUENCY_LIMIT(Doc.of(OpenemsType.BOOLEAN) //
				.text("No Frequency limit is given")), //
		FREQUENCY_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.persistencePriority(PersistencePriority.HIGH)//
				.accessMode(AccessMode.READ_WRITE)), //
		NO_START_TIMESTAMP(Doc.of(OpenemsType.BOOLEAN) //
				.text("No start timestamp")), //
		START_TIMESTAMP(Doc.of(OpenemsType.LONG) //
				.persistencePriority(PersistencePriority.HIGH)//
				.accessMode(AccessMode.READ_WRITE)), //
		NO_DURATION(Doc.of(OpenemsType.BOOLEAN) //
				.text("No duration")), //
		DURATION(Doc.of(OpenemsType.INTEGER) //
				.persistencePriority(PersistencePriority.HIGH)//
				.accessMode(AccessMode.READ_WRITE)), //
		ACTIVATION_TIME(Doc.of(ActivationTime.values())//
				.accessMode(AccessMode.READ_WRITE)), //
		SUPPORT_DURATIN(Doc.of(SupportDuration.values())//
				.accessMode(AccessMode.READ_WRITE)),
		LAST_TRIGGERED_TIME(Doc.of(OpenemsType.STRING) //
				.persistencePriority(PersistencePriority.HIGH)//
				.accessMode(AccessMode.READ_WRITE) //
				.text("Last Triggered time in Human readable form")//

		);

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
	 * Gets the Channel for {@link ChannelId#SUPPORT_DURATIN}.
	 *
	 * @return the Channel
	 */
	public default Channel<SupportDuration> getSupportDurationChannel() {
		return this.channel(ChannelId.SUPPORT_DURATIN);
	}

	/**
	 * Gets the SupportDuration, see {@link ChannelId#SUPPORT_DURATIN}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default SupportDuration getSupportDuration() {
		return this.getSupportDurationChannel().value().asEnum();
	}

	/**
	 * Gets the Channel for {@link ChannelId#ACTIVATION_TIME}.
	 *
	 * @return the Channel
	 */
	public default Channel<ActivationTime> getActivationTimeChannel() {
		return this.channel(ChannelId.ACTIVATION_TIME);
	}

	/**
	 * Gets the ActivationTime, see {@link ChannelId#ACTIVATION_TIME}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default ActivationTime getActivationTime() {
		return this.getActivationTimeChannel().value().asEnum();
	}

	/**
	 * Gets the WriteChannel {@link ChannelId#LAST_TRIGGERED_TIME}.
	 *
	 * @return the WriteChannel
	 */
	public default WriteChannel<String> getLastTriggeredTimeChannel() {
		return this.channel(ChannelId.LAST_TRIGGERED_TIME);
	}

	/**
	 * Gets the getLastTriggeredTime, see {@link ChannelId#LAST_TRIGGERED_TIME}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<String> getLastTriggeredTime() {
		return this.getLastTriggeredTimeChannel().value();
	}

	/**
	 * Sets the LastTriggeredTimseStamp, see {@link ChannelId#LAST_TRIGGERED_TIME}.
	 * 
	 * @param value the value to be set
	 */
	public default void setLastTriggeredTime(String value) {
		this.getLastTriggeredTimeChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel {@link ChannelId#SCHEDULE_PARSE_FAILED}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getScheduleParseFailedChannel() {
		return this.channel(ChannelId.SCHEDULE_PARSE_FAILED);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#SCHEDULE_PARSE_FAILED} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setScheduleParseFailed(boolean value) {
		this.getScheduleParseFailedChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel {@link ChannelId#DISCHARGE_POWER_SET_POINT}.
	 *
	 * @return the Channel
	 */
	public default WriteChannel<Integer> getDischargePowerSetPointChannel() {
		return this.channel(ChannelId.DISCHARGE_POWER_SET_POINT);
	}

	/**
	 * Gets the getDischargeActivePowerSetPoint, see
	 * {@link ChannelId#DISCHARGE_POWER_SET_POINT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDischargePowerSetPoint() {
		return this.getDischargePowerSetPointChannel().value();
	}

	/**
	 * Gets the WriteChannel {@link ChannelId#FREQUENCY_LIMIT}.
	 *
	 * @return the WriteChannel
	 */
	public default WriteChannel<Integer> getFrequencyLimitChannel() {
		return this.channel(ChannelId.FREQUENCY_LIMIT);
	}

	/**
	 * Gets the getFrequencyLimit, see {@link ChannelId#FREQUENCY_LIMIT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getFrequencyLimit() {
		return this.getFrequencyLimitChannel().value();
	}

	/**
	 * Gets the WriteChannel {@link ChannelId#DURATION}.
	 *
	 * @return the WriteChannel
	 */
	public default WriteChannel<Integer> getDurationChannel() {
		return this.channel(ChannelId.DURATION);
	}

	/*
	 * Gets the getDuration, see {@link ChannelId#DURATION}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDuration() {
		return this.getDurationChannel().value();
	}

	/**
	 * Gets the WriteChannel {@link ChannelId#START_TIMESTAMP}.
	 *
	 * @return the WriteChannel
	 */
	public default WriteChannel<Long> getStartTimestampChannel() {
		return this.channel(ChannelId.START_TIMESTAMP);
	}

	/**
	 * Gets the getStartTimestamp, see {@link ChannelId#START_TIMESTAMP}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Long> getStartTimestamp() {
		return this.getStartTimestampChannel().value();
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
	 * Gets the Channel for {@link ChannelId#CONTROL_MODE}.
	 *
	 * @return the Channel
	 */
	public default Channel<ControlMode> getControlModeChannel() {
		return this.channel(ChannelId.CONTROL_MODE);
	}

	/**
	 * Gets the {@link StateChannel} for {@link ChannelId#CONTROL_MODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<ControlMode> getControlMode() {
		return this.getControlModeChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#CONTROL_MODE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setControlMode(ControlMode value) {
		this.getControlModeChannel().setNextValue(value);
	}
}
