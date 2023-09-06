package io.openems.edge.energy;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface Energy extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		SCHEDULE_ERROR(Doc.of(Level.WARNING) //
				.text("Error in Schedule"));

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
	 * Gets the Channel for {@link ChannelId#SCHEDULE_ERROR}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getScheduleErrorChannel() {
		return this.channel(ChannelId.SCHEDULE_ERROR);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#SCHEDULE_ERROR}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setScheduleError(Boolean value) {
		this.getScheduleErrorChannel().setNextValue(value);
	}
}
