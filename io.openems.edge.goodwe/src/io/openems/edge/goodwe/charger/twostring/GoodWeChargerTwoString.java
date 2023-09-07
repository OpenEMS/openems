package io.openems.edge.goodwe.charger.twostring;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface GoodWeChargerTwoString extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		TOTAL_MPPT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),
		TOTAL_MPPT_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
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
	 * Gets the Channel for {@link ChannelId#TOTAL_MPPT_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getTotalMpptPowerChannel() {
		return this.channel(ChannelId.TOTAL_MPPT_POWER);
	}

	/**
	 * Gets the Channel for {@link ChannelId#TOTAL_MPPT_CURRENT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getTotalMpptCurrentChannel() {
		return this.channel(ChannelId.TOTAL_MPPT_CURRENT);
	}
}