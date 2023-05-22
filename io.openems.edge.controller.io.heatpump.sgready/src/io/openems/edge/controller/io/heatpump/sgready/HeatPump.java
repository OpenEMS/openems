package io.openems.edge.controller.io.heatpump.sgready;

import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public interface HeatPump extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		STATUS(Doc.of(Status.values()). //
				text("Current State")), //
		AWAITING_HYSTERESIS(Doc.of(OpenemsType.BOOLEAN)), //
		REGULAR_STATE_TIME(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_SECONDS) //
				.persistencePriority(PersistencePriority.HIGH)), //
		RECOMMENDATION_STATE_TIME(Doc.of(OpenemsType.LONG)//
				.unit(Unit.CUMULATED_SECONDS) //
				.persistencePriority(PersistencePriority.HIGH)), //
		FORCE_ON_STATE_TIME(Doc.of(OpenemsType.LONG)//
				.unit(Unit.CUMULATED_SECONDS) //
				.persistencePriority(PersistencePriority.HIGH)), //
		LOCK_STATE_TIME(Doc.of(OpenemsType.LONG)//
				.unit(Unit.CUMULATED_SECONDS) //
				.persistencePriority(PersistencePriority.HIGH)), //
		GRID_ACTIVE_POWER_NOT_PRESENT(Doc.of(Level.WARNING) //
				.text("There is no grid active power present.")),
		STATE_OF_CHARGE_NOT_PRESENT(Doc.of(Level.WARNING) //
				.text("There is no state of charge present.")), //
		ESS_DISCHARGE_POWER_NOT_PRESENT(Doc.of(Level.WARNING) //
				.text("There is no ess discharge power present.")); //

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
	 * Gets the Channel for {@link ChannelId#STATUS}.
	 *
	 * @return the Channel
	 */
	public default Channel<Status> getStatusChannel() {
		return this.channel(ChannelId.STATUS);
	}

	/**
	 * Gets the Status of the heat pump. See {@link ChannelId#STATUS}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Status getStatus() {
		return this.getStatusChannel().value().asEnum();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#STATUS} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setStatus(Status value) {
		this.getStatusChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#AWAITING_HYSTERESIS}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getAwaitingHysteresisChannel() {
		return this.channel(ChannelId.AWAITING_HYSTERESIS);
	}

	/**
	 * Gets the value for the {@link ChannelId#AWAITING_HYSTERESIS}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getAwaitingHysteresis() {
		return this.getAwaitingHysteresisChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#AWAITING_HYSTERESIS} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setAwaitingHysteresis(boolean value) {
		this.getAwaitingHysteresisChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#GRID_ACTIVE_POWER_NOT_PRESENT}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getGridActivePowerNotPresentChannel() {
		return this.channel(ChannelId.GRID_ACTIVE_POWER_NOT_PRESENT);
	}

	/**
	 * Gets the value for the {@link ChannelId#GRID_ACTIVE_POWER_NOT_PRESENT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getGridActivePowerNotPresent() {
		return this.getGridActivePowerNotPresentChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#GRID_ACTIVE_POWER_NOT_PRESENT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setGridActivePowerNotPresent(boolean value) {
		this.getGridActivePowerNotPresentChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#STATE_OF_CHARGE_NOT_PRESENT}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getStateOfChargeNotPresentChannel() {
		return this.channel(ChannelId.STATE_OF_CHARGE_NOT_PRESENT);
	}

	/**
	 * Gets the value for the {@link ChannelId#STATE_OF_CHARGE_NOT_PRESENT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getStateOfChargeNotPresent() {
		return this.getStateOfChargeNotPresentChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#STATE_OF_CHARGE_NOT_PRESENT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setStateOfChargeNotPresent(boolean value) {
		this.getStateOfChargeNotPresentChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#ESS_DISCHARGE_POWER_NOT_PRESENT}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getEssDischargePowerNotPresentChannel() {
		return this.channel(ChannelId.ESS_DISCHARGE_POWER_NOT_PRESENT);
	}

	/**
	 * Gets the value for the {@link ChannelId#ESS_DISCHARGE_POWER_NOT_PRESENT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getEssDischargePowerNotPresent() {
		return this.getEssDischargePowerNotPresentChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ESS_DISCHARGE_POWER_NOT_PRESENT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setEssDischargePowerNotPresent(boolean value) {
		this.getEssDischargePowerNotPresentChannel().setNextValue(value);
	}
}