package io.openems.edge.timedata.rrd4j;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.timedata.api.Timedata;

public interface TimedataRrd4j extends Timedata, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		QUEUE_IS_FULL(Doc.of(Level.WARNING)), //
		UNABLE_TO_INSERT_SAMPLE(Doc.of(Level.WARNING));

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
	 * Gets the Channel for {@link ChannelId#QUEUE_IS_FULL}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getQueueIsFullChannel() {
		return this.channel(ChannelId.QUEUE_IS_FULL);
	}

	/**
	 * Gets the {@link StateChannel} for {@link ChannelId#QUEUE_IS_FULL}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getQueueIsFull() {
		return this.getQueueIsFullChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#QUEUE_IS_FULL}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setQueueIsFull(Boolean value) {
		this.getQueueIsFullChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#UNABLE_TO_INSERT_SAMPLE}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getUnableToInsertSampleChannel() {
		return this.channel(ChannelId.UNABLE_TO_INSERT_SAMPLE);
	}

	/**
	 * Gets the {@link StateChannel} for {@link ChannelId#UNABLE_TO_INSERT_SAMPLE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getUnableToInsertSample() {
		return this.getUnableToInsertSampleChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#UNABLE_TO_INSERT_SAMPLE} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setUnableToInsertSample(Boolean value) {
		this.getUnableToInsertSampleChannel().setNextValue(value);
	}
}
