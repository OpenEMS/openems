package io.openems.edge.ess.generic.symmetric;

import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public interface EssProtection extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		EP_CHARGE_MAX_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE)), //
		EP_DISCHARGE_MAX_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE)), //
		EP_DEEP_DISCHARGE_PROTECTION(Doc.of(Level.FAULT)//
				.text("Deep discharge protection triggered!")), //
		EP_OVER_CHARGE_PROTECTION(Doc.of(Level.FAULT)//
				.text("Over charge protection triggered!")),//
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
	 * Gets the Channel for {@link ChannelId#EP_CHARGE_MAX_CURRENT}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getEpChargeMaxCurrentChannel() {
		return this.channel(ChannelId.EP_CHARGE_MAX_CURRENT);
	}

	/**
	 * Gets the {@link ChannelId#EP_CHARGE_MAX_CURRENT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getEpChargeMaxCurrent() {
		return this.getEpChargeMaxCurrentChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#EP_CHARGE_MAX_CURRENT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setEpChargeMaxCurrent(Integer value) {
		this.getEpChargeMaxCurrentChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#EP_DISCHARGE_MAX_CURRENT}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getEpDischargeMaxCurrentChannel() {
		return this.channel(ChannelId.EP_DISCHARGE_MAX_CURRENT);
	}

	/**
	 * Gets the {@link ChannelId#EP_DISCHARGE_MAX_CURRENT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getEpDischargeMaxCurrent() {
		return this.getEpDischargeMaxCurrentChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#EP_DISCHARGE_MAX_CURRENT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setEpDischargeMaxCurrent(Integer value) {
		this.getEpDischargeMaxCurrentChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#EP_DEEP_DISCHARGE_PROTECTION}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getEpDeepDischargeProtectionChannel() {
		return this.channel(ChannelId.EP_DEEP_DISCHARGE_PROTECTION);
	}

	/**
	 * Gets the {@link ChannelId#EP_DEEP_DISCHARGE_PROTECTION}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getEpDeepDischargeProtection() {
		return this.getEpDeepDischargeProtectionChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#EP_DEEP_DISCHARGE_PROTECTION} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setEpDeepDischargeProtection(boolean value) {
		this.getEpDeepDischargeProtectionChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#EP_OVER_CHARGE_PROTECTION}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getEpOverChargeProtectionChannel() {
		return this.channel(ChannelId.EP_OVER_CHARGE_PROTECTION);
	}

	/**
	 * Gets the {@link ChannelId#EP_OVER_CHARGE_PROTECTION}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getEpOverChargeProtection() {
		return this.getEpOverChargeProtectionChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#EP_OVER_CHARGE_PROTECTION} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setEpOverChargeProtection(boolean value) {
		this.getEpOverChargeProtectionChannel().setNextValue(value);
	}
}
