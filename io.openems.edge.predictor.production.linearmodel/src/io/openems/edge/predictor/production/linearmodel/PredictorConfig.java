package io.openems.edge.predictor.production.linearmodel;

import java.time.Duration;
import java.time.ZonedDateTime;

import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;
import io.openems.edge.predictor.api.mlcore.datastructures.Series;
import io.openems.edge.predictor.api.mlcore.regression.Regressor;
import io.openems.edge.predictor.production.linearmodel.prediction.PredictionContext;
import io.openems.edge.predictor.production.linearmodel.prediction.PredictionOrchestrator;

public interface PredictorConfig {

	@FunctionalInterface
	public interface PredictionOrchestratorFactory {

		/**
		 * Creates a new {@link PredictionOrchestrator} for the given context.
		 *
		 * @param context the prediction context
		 * @return a new {@link PredictionOrchestrator}
		 */
		public PredictionOrchestrator create(PredictionContext context);
	}

	@FunctionalInterface
	public interface RegressorFitter {

		/**
		 * Fits a {@link Regressor} to the given training data.
		 *
		 * @param features the feature matrix containing input variables
		 * @param target   the target series to predict
		 * @return a fitted {@link Regressor} instance
		 */
		public Regressor fit(//
				DataFrame<ZonedDateTime> features, //
				Series<ZonedDateTime> target);
	}

	/**
	 * The retraining interval in days.
	 *
	 * @return the retraining interval in days
	 */
	public int trainingIntervalInDays();

	/**
	 * The training window in quarters.
	 *
	 * @return the training window in quarters
	 */
	public int trainingWindowInQuarters();

	/**
	 * The minimum number of samples required for training.
	 *
	 * @return the minimum number of training samples
	 */
	public int minTrainingSamples();

	/**
	 * The maximum number of training samples.
	 *
	 * @return the maximum number of training samples
	 */
	public int maxTrainingSamples();

	/**
	 * The function used to fit a regressor.
	 *
	 * @return the regressor fitter
	 */
	public RegressorFitter regressorFitter();

	/**
	 * The maximum allowed model age.
	 *
	 * @return the maximum model age
	 */
	public Duration maxModelAge();

	/**
	 * The forecast horizon in quarters.
	 *
	 * @return the number of forecast quarters
	 */
	public int forecastQuarters();

	/**
	 * The factory that creates {@link PredictionOrchestrator} instances.
	 *
	 * @return the orchestrator factory
	 */
	public PredictionOrchestratorFactory predictionOrchestratorFactory();
}
