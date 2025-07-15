package io.openems.edge.predictor.production.linearmodel;

import io.openems.common.channel.PersistencePriority;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.predictor.api.prediction.Predictor;

public interface PredictorProductionLinearModel extends Predictor, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/**
		 * Current state of the predictor linear model training.
		 */
		PREDICTOR_LINEAR_MODEL_TRAINING_STATE(Doc.of(TrainingState.values())//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Current state of the predictor linear model training.")), //

		/**
		 * Current state of the predictor linear model prediction.
		 */
		PREDICTOR_LINEAR_MODEL_PREDICTION_STATE(Doc.of(PredictionState.values())//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Current state of the predictor linear model prediction.")), //
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
	 * Gets the Channel for {@link ChannelId#PREDICTOR_LINEAR_MODEL_TRAINING_STATE}.
	 *
	 * @return the Channel
	 */
	public default Channel<TrainingState> getTrainingStateChannel() {
		return this.channel(ChannelId.PREDICTOR_LINEAR_MODEL_TRAINING_STATE);
	}

	/**
	 * Gets the training Status of the predictor linear model. See
	 * {@link ChannelId#PREDICTOR_LINEAR_MODEL_TRAINING_STATE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default TrainingState getTrainingState() {
		return this.getTrainingStateChannel().value().asEnum();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#PREDICTOR_LINEAR_MODEL_TRAINING_STATE} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setTrainingState(TrainingState value) {
		this.getTrainingStateChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for
	 * {@link ChannelId#PREDICTOR_LINEAR_MODEL_PREDICTION_STATE}.
	 *
	 * @return the Channel
	 */
	public default Channel<PredictionState> getPredictionStateChannel() {
		return this.channel(ChannelId.PREDICTOR_LINEAR_MODEL_PREDICTION_STATE);
	}

	/**
	 * Gets the prediction Status of the predictor linear model. See
	 * {@link ChannelId#PREDICTOR_LINEAR_MODEL_PREDICTION_STATE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default PredictionState getPredictionState() {
		return this.getPredictionStateChannel().value().asEnum();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#PREDICTOR_LINEAR_MODEL_PREDICTION_STATE} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setPredictionState(PredictionState value) {
		this.getPredictionStateChannel().setNextValue(value);
	}
}
