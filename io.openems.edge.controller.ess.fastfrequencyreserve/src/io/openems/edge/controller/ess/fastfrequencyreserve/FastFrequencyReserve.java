package io.openems.edge.controller.ess.fastfrequencyreserve;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.LongWriteChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.ess.fastfrequencyreserve.enums.Mode;
import io.openems.edge.controller.ess.fastfrequencyreserve.statemachine.StateMachine;
import io.openems.edge.controller.ess.fastfrequencyreserve.statemachine.StateMachine.State;

//CHECKSTYLE:OFF
public interface FastFrequencyReserve extends Controller, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		MODE(Doc.of(Mode.values()) //
				.initialValue(Mode.MANUAL_OFF) //
				.text("Configured Mode")), //
	
		/**
		 * Current state of the StateMachine.
		 */
		STATE_MACHINE(Doc.of(State.values()) //
				.text("Current State of State-Machine")), //

		SCHEDULE_PARSE_FAILED(Doc.of(Level.FAULT) //
				.text("Unable to parse Schedule")), //

		NO_ACTIVE_SETPOINT(Doc.of(OpenemsType.BOOLEAN) //
				.text("No active Set-Point given")), //
		DISCHARGE_POWER_SET_POINT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Discharge Active-Power Setpoint")),

		NO_FREQUENCY_LIMIT(Doc.of(OpenemsType.BOOLEAN) //
				.text("No Frequency limit is given")), //
		FREQUENCY_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Frequency Limit")), //

		NO_START_TIMESTAMP(Doc.of(OpenemsType.BOOLEAN) //
				.text("No Frequency limit is given")), //
		START_TIMESTAMP(Doc.of(OpenemsType.LONG) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Frequency Limit")), //

		NO_DURATION(Doc.of(OpenemsType.BOOLEAN) //
				.text("No Frequency limit is given")), //
		DURATION(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Frequency Limit") //

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
	 * Gets the Channel for {@link ChannelId#STATE_MACHINE}.
	 *
	 * @return the Channel
	 */
	public default Channel<State> getStateMachineChannel() {
		return this.channel(ChannelId.STATE_MACHINE);
	}

	/**
	 * Gets current state of the {@link StateMachine}. See
	 * {@link ChannelId#STATE_MACHINE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default State getStateMachine() {
		return this.getStateMachineChannel().value().asEnum();
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

	// ========================================================================================
	public default StateChannel getScheduleParseFailedChannel() {
		return this.channel(ChannelId.SCHEDULE_PARSE_FAILED);
	}

	public default Value<Boolean> getScheduleParseFailed() {
		return this.getScheduleParseFailedChannel().value();
	}

	public default void _setScheduleParseFailed(boolean value) {
		this.getScheduleParseFailedChannel().setNextValue(value);
	}

	// ========================================================================================
	public default IntegerWriteChannel getDischargePowerSetPointChannel() {
		return this.channel(ChannelId.DISCHARGE_POWER_SET_POINT);
	}

	public default Value<Integer> getDischargeActivePowerSetPoint() {
		return this.getDischargePowerSetPointChannel().getNextValue();
	}

	public default void _setDischargeActivePowerSetPoint(Integer value) {
		this.getDischargePowerSetPointChannel().setNextValue(value);
	}

	public default void setDischargeActivePowerSetPoint(Integer value) throws OpenemsNamedException {
		this.getDischargePowerSetPointChannel().setNextWriteValue(value);
	}

	// ========================================================================================
	public default StateChannel getNoActiveSetpointChannel() {
		return this.channel(ChannelId.NO_ACTIVE_SETPOINT);
	}

	public default Value<Boolean> getNoActiveSetpoint() {
		return this.getNoActiveSetpointChannel().value();
	}

	public default void _setNoActiveSetpoint(boolean value) {
		this.getNoActiveSetpointChannel().setNextValue(value);
	}

	// ========================================================================================
	public default StateChannel getNoFrequencyLimitChannel() {
		return this.channel(ChannelId.NO_FREQUENCY_LIMIT);
	}

	public default Value<Boolean> getNofrequencyLimitSetpoint() {
		return this.getNoFrequencyLimitChannel().value();
	}

	public default void _setNofrequencyLimitSetpoint(boolean value) {
		this.getNoFrequencyLimitChannel().setNextValue(value);
	}

	// ========================================================================================
	public default IntegerWriteChannel getFrequencyLimitChannel() {
		return this.channel(ChannelId.FREQUENCY_LIMIT);
	}

	public default Value<Integer> getFrequencyLimitSetPoint() {
		return this.getFrequencyLimitChannel().getNextValue();
	}

	public default void _setFrequencyLimitSetPoint(Integer value) {
		this.getFrequencyLimitChannel().setNextValue(value);
	}

	public default void setFrequencyLimitSetPoint(Integer value) throws OpenemsNamedException {
		this.getFrequencyLimitChannel().setNextWriteValue(value);
	}
	// ========================================================================================

	public default IntegerWriteChannel getDurationChannel() {
		return this.channel(ChannelId.DURATION);
	}

	public default Value<Integer> getDuration() {
		return this.getDurationChannel().getNextValue();
	}

	public default void _setDuration(Integer value) {
		this.getDurationChannel().setNextValue(value);
	}

	public default void setDuration(Integer value) throws OpenemsNamedException {
		this.getDurationChannel().setNextWriteValue(value);
	}
	// ========================================================================================

	public default StateChannel getNoDurationChannel() {
		return this.channel(ChannelId.NO_FREQUENCY_LIMIT);
	}

	public default Value<Boolean> getNoDuration() {
		return this.getNoDurationChannel().value();
	}

	public default void _setNoDuration(boolean value) {
		this.getNoDurationChannel().setNextValue(value);
	}
	// ========================================================================================

	public default LongWriteChannel getStartTimeStampChannel() {
		return this.channel(ChannelId.START_TIMESTAMP);
	}

	public default Value<Long> getStartTimeStamp() {
		return this.getStartTimeStampChannel().getNextValue();
	}

	public default void _setStartTimeStamp(Long value) {
		this.getStartTimeStampChannel().setNextValue(value);
	}

	public default void setStartTimeStamp(Long value) throws OpenemsNamedException {
		this.getStartTimeStampChannel().setNextWriteValue(value);
	}
	// ========================================================================================

	public default StateChannel getNoStartTimeStampChannel() {
		return this.channel(ChannelId.NO_START_TIMESTAMP);
	}

	public default Value<Boolean> getNoStartTimeStamp() {
		return this.getNoStartTimeStampChannel().value();
	}

	public default void _setNoStartTimeStamp(boolean value) {
		this.getNoStartTimeStampChannel().setNextValue(value);
	}

	// ========================================================================================

//	public default StateChannel getScheduleParseFailedChannel() {
//		return this.channel(ChannelId.SCHEDULE_PARSE_FAILED);
//	}
//
//	/**
//	 * Gets the Run-Failed State. See {@link ChannelId#SCHEDULE_PARSE_FAILED}.
//	 *
//	 * @return the Channel {@link Value}
//	 */
//	public default Value<Boolean> getScheduleParseFailed() {
//		return this.getScheduleParseFailedChannel().value();
//	}
//
//	/**
//	 * Internal method to set the 'nextValue' on
//	 * {@link ChannelId#SCHEDULE_PARSE_FAILED} Channel.
//	 *
//	 * @param value the next value
//	 */
//	public default void _setScheduleParseFailed(boolean value) {
//		this.getScheduleParseFailedChannel().setNextValue(value);
//	}
//
//	/**
//	 * Gets the Channel for {@link ChannelId#DISCHARGE_POWER_SET_POINT}.
//	 *
//	 * @return the Channel
//	 */
//	public default IntegerWriteChannel getDischargePowerSetPointChannel() {
//		return this.channel(ChannelId.DISCHARGE_POWER_SET_POINT);
//	}
//
//	/**
//	 * Gets the Active Power Limit in [W]. See
//	 * {@link ChannelId#DISCHARGE_POWER_SET_POINT}.
//	 *
//	 * @return the Channel {@link Value}
//	 */
//	public default Value<Integer> getDischargeActivePowerSetPoint() {
//		return this.getDischargePowerSetPointChannel().value();
//	}
//
//	/**
//	 * Internal method to set the 'nextValue' on
//	 * {@link ChannelId#DISCHARGE_POWER_SET_POINT} Channel.
//	 *
//	 * @param value the next value
//	 */
//	public default void _setDischargeActivePowerSetPoint(Integer value) {
//		this.getDischargePowerSetPointChannel().setNextValue(value);
//	}
//
//	/**
//	 * Sets the Active Power Limit in [W]. See
//	 * {@link ChannelId#DISCHARGE_POWER_SET_POINT}.
//	 *
//	 * @param value the active power limit
//	 * @throws OpenemsNamedException on error
//	 */
//	public default void setDischargeActivePowerSetPoint(Integer value) throws OpenemsNamedException {
//		this.getDischargePowerSetPointChannel().setNextWriteValue(value);
//	}
//	
//	/**
//	 * Gets the Channel for {@link ChannelId#NO_ACTIVE_SETPOINT}.
//	 *
//	 * @return the Channel
//	 */
//	public default StateChannel getNoActiveSetpointChannel() {
//		return this.channel(ChannelId.NO_ACTIVE_SETPOINT);
//	}
//
//	/**
//	 * Gets the Run-Failed State. See {@link ChannelId#NO_ACTIVE_SETPOINT}.
//	 *
//	 * @return the Channel {@link Value}
//	 */
//	public default Value<Boolean> getNoActiveSetpoint() {
//		return this.getNoActiveSetpointChannel().value();
//	}
//
//	/**
//	 * Internal method to set the 'nextValue' on
//	 * {@link ChannelId#NO_ACTIVE_SETPOINT} Channel.
//	 *
//	 * @param value the next value
//	 */
//	public default void _setNoActiveSetpoint(boolean value) {
//		this.getNoActiveSetpointChannel().setNextValue(value);
//	}

}
//CHECKSTYLE:ON