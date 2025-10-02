package io.openems.edge.predictor.production.linearmodel;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
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

		/**
		 * Forecasted production power from the linear prediction model, one hour ahead
		 * (calculated now for +1h).
		 */
		PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_1H_AHEAD(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Forecasted production power from the linear prediction model one hour ahead.")), //

		/**
		 * Forecasted production power from the linear prediction model, realized at the
		 * predicted time (value predicted 1h earlier).
		 */
		PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_1H_REALIZED(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Forecasted production power from the linear prediction model realized at the predicted time (predicted 1h ago).")), //

		/**
		 * Forecasted production power from the linear prediction model, six hours ahead
		 * (calculated now for +6h).
		 */
		PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_6H_AHEAD(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Forecasted production power from the linear prediction model six hours ahead.")), //

		/**
		 * Forecasted production power from the linear prediction model, realized at the
		 * predicted time (value predicted 6h earlier).
		 */
		PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_6H_REALIZED(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Forecasted production power from the linear prediction model realized at the predicted time (predicted 6h ago).")), //

		/**
		 * Forecasted production power from the linear prediction model, twelve hours
		 * ahead (calculated now for +12h).
		 */
		PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_12H_AHEAD(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Forecasted production power from the linear prediction model twelve hours ahead.")), //

		/**
		 * Forecasted production power from the linear prediction model, realized at the
		 * predicted time (value predicted 12h earlier).
		 */
		PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_12H_REALIZED(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Forecasted production power from the linear prediction model realized at the predicted time (predicted 12h ago).")), //

		/**
		 * Forecasted production power from the linear prediction model, twenty-four
		 * hours ahead (calculated now for +24h).
		 */
		PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_24H_AHEAD(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Forecasted production power from the linear prediction model twenty-four hours ahead.")), //

		/**
		 * Forecasted production power from the linear prediction model, realized at the
		 * predicted time (value predicted 24h earlier).
		 */
		PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_24H_REALIZED(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Forecasted production power from the linear prediction model realized at the predicted time (predicted 24h ago).")), //

		/**
		 * Forecasted production power from the linear prediction model, thirty-six
		 * hours ahead (calculated now for +36h).
		 */
		PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_36H_AHEAD(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Forecasted production power from the linear prediction model thirty-six hours ahead.")), //

		/**
		 * Forecasted production power from the linear prediction model, realized at the
		 * predicted time (value predicted 36h earlier).
		 */
		PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_36H_REALIZED(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Forecasted production power from the linear prediction model realized at the predicted time (predicted 36h ago).")),

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

	/**
	 * Gets the Channel for
	 * {@link ChannelId#PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_1H_AHEAD}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getPrediction1hAheadChannel() {
		return this.channel(ChannelId.PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_1H_AHEAD);
	}

	/**
	 * Gets the prediction from the linear prediction model one hour ahead. See
	 * {@link ChannelId#PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_1H_AHEAD}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Integer getPrediction1hAhead() {
		return this.getPrediction1hAheadChannel().value().get();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_1H_AHEAD}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setPrediction1hAhead(Integer value) {
		this.getPrediction1hAheadChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for
	 * {@link ChannelId#PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_1H_REALIZED}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getPrediction1hRealizedChannel() {
		return this.channel(ChannelId.PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_1H_REALIZED);
	}

	/**
	 * Gets the realized prediction from the linear prediction model (the value that
	 * was predicted one hour earlier). See
	 * {@link ChannelId#PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_1H_REALIZED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Integer getPrediction1hRealized() {
		return this.getPrediction1hRealizedChannel().value().get();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_1H_REALIZED}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setPrediction1hRealized(Integer value) {
		this.getPrediction1hRealizedChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for
	 * {@link ChannelId#PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_6H_AHEAD}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getPrediction6hAheadChannel() {
		return this.channel(ChannelId.PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_6H_AHEAD);
	}

	/**
	 * Gets the prediction from the linear prediction model six hours ahead. See
	 * {@link ChannelId#PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_6H_AHEAD}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Integer getPrediction6hAhead() {
		return this.getPrediction6hAheadChannel().value().get();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_6H_AHEAD}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setPrediction6hAhead(Integer value) {
		this.getPrediction6hAheadChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for
	 * {@link ChannelId#PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_6H_REALIZED}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getPrediction6hRealizedChannel() {
		return this.channel(ChannelId.PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_6H_REALIZED);
	}

	/**
	 * Gets the realized prediction from the linear prediction model (the value that
	 * was predicted six hours earlier). See
	 * {@link ChannelId#PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_6H_REALIZED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Integer getPrediction6hRealized() {
		return this.getPrediction6hRealizedChannel().value().get();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_6H_REALIZED}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setPrediction6hRealized(Integer value) {
		this.getPrediction6hRealizedChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for
	 * {@link ChannelId#PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_12H_AHEAD}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getPrediction12hAheadChannel() {
		return this.channel(ChannelId.PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_12H_AHEAD);
	}

	/**
	 * Gets the prediction from the linear prediction model twelve hours ahead. See
	 * {@link ChannelId#PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_12H_AHEAD}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Integer getPrediction12hAhead() {
		return this.getPrediction12hAheadChannel().value().get();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_12H_AHEAD}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setPrediction12hAhead(Integer value) {
		this.getPrediction12hAheadChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for
	 * {@link ChannelId#PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_12H_REALIZED}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getPrediction12hRealizedChannel() {
		return this.channel(ChannelId.PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_12H_REALIZED);
	}

	/**
	 * Gets the realized prediction from the linear prediction model (the value that
	 * was predicted twelve hours earlier). See
	 * {@link ChannelId#PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_12H_REALIZED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Integer getPrediction12hRealized() {
		return this.getPrediction12hRealizedChannel().value().get();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_12H_REALIZED}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setPrediction12hRealized(Integer value) {
		this.getPrediction12hRealizedChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for
	 * {@link ChannelId#PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_24H_AHEAD}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getPrediction24hAheadChannel() {
		return this.channel(ChannelId.PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_24H_AHEAD);
	}

	/**
	 * Gets the prediction from the linear prediction model twenty-four hours ahead.
	 * See {@link ChannelId#PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_24H_AHEAD}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Integer getPrediction24hAhead() {
		return this.getPrediction24hAheadChannel().value().get();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_24H_AHEAD}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setPrediction24hAhead(Integer value) {
		this.getPrediction24hAheadChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for
	 * {@link ChannelId#PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_24H_REALIZED}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getPrediction24hRealizedChannel() {
		return this.channel(ChannelId.PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_24H_REALIZED);
	}

	/**
	 * Gets the realized prediction from the linear prediction model (the value that
	 * was predicted twenty-four hours earlier). See
	 * {@link ChannelId#PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_24H_REALIZED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Integer getPrediction24hRealized() {
		return this.getPrediction24hRealizedChannel().value().get();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_24H_REALIZED}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setPrediction24hRealized(Integer value) {
		this.getPrediction24hRealizedChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for
	 * {@link ChannelId#PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_36H_AHEAD}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getPrediction36hAheadChannel() {
		return this.channel(ChannelId.PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_36H_AHEAD);
	}

	/**
	 * Gets the prediction from the linear prediction model thirty-six hours ahead.
	 * See {@link ChannelId#PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_36H_AHEAD}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Integer getPrediction36hAhead() {
		return this.getPrediction36hAheadChannel().value().get();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_36H_AHEAD}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setPrediction36hAhead(Integer value) {
		this.getPrediction36hAheadChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for
	 * {@link ChannelId#PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_36H_REALIZED}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getPrediction36hRealizedChannel() {
		return this.channel(ChannelId.PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_36H_REALIZED);
	}

	/**
	 * Gets the realized prediction from the linear prediction model (the value that
	 * was predicted thirty-six hours earlier). See
	 * {@link ChannelId#PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_36H_REALIZED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Integer getPrediction36hRealized() {
		return this.getPrediction36hRealizedChannel().value().get();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#PREDICTOR_PRODUCTION_LINEAR_MODEL_PREDICTION_36H_REALIZED}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setPrediction36hRealized(Integer value) {
		this.getPrediction36hRealizedChannel().setNextValue(value);
	}
}
