package io.openems.edge.predictor.lstmmodel;

import static io.openems.common.channel.Level.INFO;
import static io.openems.common.types.OpenemsType.DOUBLE;
import static io.openems.common.types.OpenemsType.LONG;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.DoubleReadChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface LstmModel extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		LAST_TRAINED_TIME(Doc.of(LONG) //
				.text("Last trained time as Unixtimestamp [ms]")), //
		MODEL_ERROR(Doc.of(DOUBLE) //
				.text("Error in the Model")), //
		CANNOT_TRAIN_CONDITON(Doc.of(INFO) //
				.text("When the data set is empty, entirely null, or contains 50% null values."));

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
	 * Gets the Channel for {@link ChannelId#CANNOT_TRAIN_CONDITON}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getCannotTrainConditionChannel() {
		return this.channel(ChannelId.CANNOT_TRAIN_CONDITON);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#CANNOT_TRAIN_CONDITON} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setCannotTrainCondition(boolean value) {
		this.getCannotTrainConditionChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#LAST_TRAINED_TIME}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getLastTrainedTimeChannel() {
		return this.channel(ChannelId.LAST_TRAINED_TIME);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#LAST_TRAINED_TIME}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setLastTrainedTime(long value) {
		this.getLastTrainedTimeChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MODEL_ERROR}.
	 *
	 * @return the Channel
	 */
	public default DoubleReadChannel getModelErrorChannel() {
		return this.channel(ChannelId.MODEL_ERROR);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#LAST_TRAINED_TIME}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setModelError(Double value) {
		this.getModelErrorChannel().setNextValue(value);
	}

}
