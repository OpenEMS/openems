package io.openems.edge.predictor.production.linearmodel.prediction;

import java.time.ZonedDateTime;

import com.google.common.annotations.VisibleForTesting;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.predictor.api.common.PredictionException;
import io.openems.edge.predictor.api.mlcore.datastructures.Series;
import io.openems.edge.predictor.api.mlcore.regression.Regressor;
import io.openems.edge.predictor.production.linearmodel.services.FeatureEngineeringService;
import io.openems.edge.predictor.production.linearmodel.services.PredictionDataService;

public class PredictionOrchestrator {

	private final PredictionContext predictionContext;

	private final PredictionDataService predictionDataService;
	private final FeatureEngineeringService featureEngineeringService;

	public PredictionOrchestrator(PredictionContext predictionContext) {
		this.predictionContext = predictionContext;
		this.predictionDataService = new PredictionDataService(//
				this.predictionContext.weather(), //
				this.predictionContext.clock());
		this.featureEngineeringService = new FeatureEngineeringService();
	}

	@VisibleForTesting
	PredictionOrchestrator(//
			PredictionContext predictionContext, //
			PredictionDataService predictionDataService, //
			FeatureEngineeringService featureEngineeringService) {
		this.predictionContext = predictionContext;
		this.predictionDataService = predictionDataService;
		this.featureEngineeringService = featureEngineeringService;
	}

	/**
	 * Runs a prediction using the trained {@link Regressor}.
	 *
	 * @return a {@link Series} with timestamps and predicted values
	 * @throws OpenemsException if prediction fails
	 */
	public Series<ZonedDateTime> runPrediction() throws PredictionException {
		var rawFeatureMatrix = this.predictionDataService.prepareFeatureMatrix(//
				this.predictionContext.forecastQuarters());

		var transformedFeatureMatrix = this.featureEngineeringService.transformForPrediction(rawFeatureMatrix);

		var index = transformedFeatureMatrix.getIndex();
		var predictedValues = this.predictionContext.regressor()//
				.predict(transformedFeatureMatrix);

		return new Series<>(index, predictedValues);
	}
}
