package io.openems.edge.controller.ess.chargedischargelimiter;

import static io.openems.common.channel.PersistencePriority.HIGH;
import static io.openems.common.channel.PersistencePriority.LOW;

import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public interface ControllerChargeDischargeLimiter extends Controller, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		STATE_MACHINE(Doc.of(State.values()) //
				.text("Current State of State-Machine")), //
		AWAITING_HYSTERESIS(Doc.of(Level.INFO) //
				.text("Would change State, but hysteresis is active")),
		/**
		 * Holds the minimum SoC value configured.
		 */
		FORCE_CHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).persistencePriority(HIGH)),   // Priority high for testing
		BALANCING_SOC(Doc.of(OpenemsType.INTEGER) //

				.unit(Unit.PERCENT).persistencePriority(HIGH)), //
		ACTIVE_CHARGE_ENERGY(Doc.of(OpenemsType.INTEGER) //

				.unit(Unit.KILOWATT_HOURS).persistencePriority(HIGH)), //		
		MIN_SOC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT)), //
		MAX_SOC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT)); //

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
	 * Gets the Channel for {@link ChannelId#AWAITING_HYSTERESIS}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getAwaitingHysteresisChannel() {
		return this.channel(ChannelId.AWAITING_HYSTERESIS);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#AWAITING_HYSTERESIS} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setAwaitingHysteresisValue(boolean value) {
		this.getAwaitingHysteresisChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MIN_SOC}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMinSocChannel() {
		return this.channel(ChannelId.MIN_SOC);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAX_SOC}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMaxSocChannel() {
		return this.channel(ChannelId.MAX_SOC);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#MIN_SOC} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMinSoc(Integer value) {
		this.getMinSocChannel().setNextValue(value);
	}

	/**
	 * Gets the minimum SoC value configured. See {@link ChannelId#MIN_SOC}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getMinSoc() {
		return this.getMinSocChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#MAX_SOC} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMaxSoc(Integer value) {
		this.getMaxSocChannel().setNextValue(value);
	}

	/**
	 * Gets the maximum SoC value configured. See {@link ChannelId#MAX_SOC}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getMaxSoc() {
		return this.getMaxSocChannel().value();
	}
}
