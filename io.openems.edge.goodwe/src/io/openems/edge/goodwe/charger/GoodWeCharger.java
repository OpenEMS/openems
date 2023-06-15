package io.openems.edge.goodwe.charger;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public interface GoodWeCharger extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		HAS_NO_DC_PV(Doc.of(Level.INFO) //
				.text("This GoodWe has no DC-PV. Chargers can be deleted."));

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
	 * Gets the Channel for {@link ChannelId#HAS_NO_DC_PV}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getHasNoDcPvChannel() {
		return this.channel(ChannelId.HAS_NO_DC_PV);
	}

	/**
	 * Gets the Has-No-DC-PV State. See {@link ChannelId#HAS_NO_DC_PV}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getHasNoDcPv() {
		return this.getHasNoDcPvChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#HAS_NO_DC_PV}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setHasNoDcPv(Boolean value) {
		this.getHasNoDcPvChannel().setNextValue(value);
	}
}