package io.openems.edge.controller.symmetric.balancingschedule;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public interface ControllerEssBalancingSchedule extends Controller, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		NO_ACTIVE_SETPOINT(Doc.of(Level.INFO) //
				.text("No active Set-Point given")), //
		SCHEDULE_PARSE_FAILED(Doc.of(Level.FAULT) //
				.text("Unable to parse Schedule")), //

		GRID_ACTIVE_POWER_SET_POINT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Target Active-Power Setpoint at the grid connection point"));

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
	 * Gets the Channel for {@link ChannelId#NO_ACTIVE_SETPOINT}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getNoActiveSetpointChannel() {
		return this.channel(ChannelId.NO_ACTIVE_SETPOINT);
	}

	/**
	 * Gets the Run-Failed State. See {@link ChannelId#NO_ACTIVE_SETPOINT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getNoActiveSetpoint() {
		return this.getNoActiveSetpointChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#NO_ACTIVE_SETPOINT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setNoActiveSetpoint(boolean value) {
		this.getNoActiveSetpointChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SCHEDULE_PARSE_FAILED}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getScheduleParseFailedChannel() {
		return this.channel(ChannelId.SCHEDULE_PARSE_FAILED);
	}

	/**
	 * Gets the Run-Failed State. See {@link ChannelId#SCHEDULE_PARSE_FAILED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getScheduleParseFailed() {
		return this.getScheduleParseFailedChannel().value();
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
	 * Gets the Channel for {@link ChannelId#GRID_ACTIVE_POWER_SET_POINT}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getGridActivePowerSetPointChannel() {
		return this.channel(ChannelId.GRID_ACTIVE_POWER_SET_POINT);
	}

	/**
	 * Gets the Active Power Limit in [W]. See
	 * {@link ChannelId#GRID_ACTIVE_POWER_SET_POINT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getGridActivePowerSetPoint() {
		return this.getGridActivePowerSetPointChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#GRID_ACTIVE_POWER_SET_POINT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setGridActivePowerSetPoint(Integer value) {
		this.getGridActivePowerSetPointChannel().setNextValue(value);
	}

	/**
	 * Sets the Active Power Limit in [W]. See
	 * {@link ChannelId#GRID_ACTIVE_POWER_SET_POINT}.
	 *
	 * @param value the active power limit
	 * @throws OpenemsNamedException on error
	 */
	public default void setGridActivePowerSetPoint(Integer value) throws OpenemsNamedException {
		this.getGridActivePowerSetPointChannel().setNextWriteValue(value);
	}
}
