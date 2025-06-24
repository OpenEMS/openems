package io.openems.edge.battery.protection;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public interface BatteryVoltageProtection extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		BVP_CHARGE_BMS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)), //
		BVP_DISCHARGE_BMS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)), //
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
	 * Gets the Channel for {@link ChannelId#BVP_CHARGE_BMS}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getBvpChargeBmsChannel() {
		return this.channel(ChannelId.BVP_CHARGE_BMS);
	}

	/**
	 * Gets the {@link ChannelId#BVP_CHARGE_BMS}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBvpChargeBms() {
		return this.getBvpChargeBmsChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#BVP_CHARGE_BMS}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setBvpChargeBms(Integer value) {
		this.getBvpChargeBmsChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#BVP_DISCHARGE_BMS}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getBvpDischargeBmsChannel() {
		return this.channel(ChannelId.BVP_DISCHARGE_BMS);
	}

	/**
	 * Gets the {@link ChannelId#BVP_DISCHARGE_BMS}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBvpDischargeBms() {
		return this.getBvpDischargeBmsChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#BVP_DISCHARGE_BMS}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setBvpDischargeBms(Integer value) {
		this.getBvpDischargeBmsChannel().setNextValue(value);
	}

}
