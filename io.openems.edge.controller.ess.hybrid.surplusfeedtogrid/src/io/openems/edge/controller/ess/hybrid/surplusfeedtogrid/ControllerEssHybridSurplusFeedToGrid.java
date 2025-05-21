package io.openems.edge.controller.ess.hybrid.surplusfeedtogrid;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public interface ControllerEssHybridSurplusFeedToGrid extends Controller, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		SURPLUS_FEED_TO_GRID_IS_LIMITED(Doc.of(Level.INFO) //
				.text("Surplus-Feed-To-Grid power is limited"));

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
	 * Gets the Channel for {@link ChannelId#SURPLUS_FEED_TO_GRID_IS_LIMITED}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getSurplusFeedToGridIsLimitedChannel() {
		return this.channel(ChannelId.SURPLUS_FEED_TO_GRID_IS_LIMITED);
	}

	/**
	 * Gets the Run-Failed State. See
	 * {@link ChannelId#SURPLUS_FEED_TO_GRID_IS_LIMITED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getSurplusFeedToGridIsLimited() {
		return this.getSurplusFeedToGridIsLimitedChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#SURPLUS_FEED_TO_GRID_IS_LIMITED} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setSurplusFeedToGridIsLimited(boolean value) {
		this.getSurplusFeedToGridIsLimitedChannel().setNextValue(value);
	}

}
