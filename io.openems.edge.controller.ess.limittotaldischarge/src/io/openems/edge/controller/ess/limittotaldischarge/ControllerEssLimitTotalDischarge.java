package io.openems.edge.controller.ess.limittotaldischarge;

import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public interface ControllerEssLimitTotalDischarge extends Controller, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		STATE_MACHINE(Doc.of(State.values()) //
				.text("Current State of State-Machine")), //
		AWAITING_HYSTERESIS(Doc.of(Level.INFO) //
				.text("Would change State, but hysteresis is active")),
		/**
		 * Holds the minimum SoC value configured.
		 */
		MIN_SOC(Doc.of(OpenemsType.INTEGER) //
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

}
