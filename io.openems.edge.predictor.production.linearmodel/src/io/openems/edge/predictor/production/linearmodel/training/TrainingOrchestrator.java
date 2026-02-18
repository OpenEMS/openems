package io.openems.edge.predictor.production.linearmodel.training;

import static io.openems.common.utils.DateUtils.roundDownToQuarter;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import com.google.common.annotations.VisibleForTesting;

import io.openems.edge.predictor.api.common.TrainingException;
import io.openems.edge.predictor.production.linearmodel.TrainingCallback.ModelBundle;
import io.openems.edge.predictor.production.linearmodel.services.FeatureEngineeringService;
import io.openems.edge.predictor.production.linearmodel.services.ModelTrainingService;
import io.openems.edge.predictor.production.linearmodel.services.TrainingDataService;

public class TrainingOrchestrator {

	private static final int MINUTES_PER_QUARTER = 15;

	private final TrainingContext trainingContext;

	private final TrainingDataService trainingDataService;
	private final FeatureEngineeringService featureEngineeringService;
	private final ModelTrainingService modelTrainingService;

	public TrainingOrchestrator(TrainingContext trainingContext) {
		this.trainingContext = trainingContext;
		this.trainingDataService = new TrainingDataService(//
				trainingContext.weather(), //
				trainingContext.timedata(), //
				trainingContext.productionChannelAddress());
		this.featureEngineeringService = new FeatureEngineeringService();
		this.modelTrainingService = new ModelTrainingService(//
				trainingContext.regressorFitter(), //
				trainingContext.minTrainingSamples(), //
				trainingContext.maxTrainingSamples());
	}

	@VisibleForTesting
	TrainingOrchestrator(//
			TrainingContext trainingContext, //
			TrainingDataService trainingDataService, //
			FeatureEngineeringService featureEngineeringService, //
			ModelTrainingService modelTrainingService) {
		this.trainingContext = trainingContext;
		this.trainingDataService = trainingDataService;
		this.featureEngineeringService = featureEngineeringService;
		this.modelTrainingService = modelTrainingService;
	}

	/**
	 * Runs the full training process: prepares the feature-target matrix, applies
	 * feature engineering, trains the regressor, and returns the resulting model.
	 *
	 * @return the trained {@link ModelBundle} containing the regressor and its
	 *         metadata
	 * @throws TrainingException if fetching data, transforming features, or
	 *                           training fails
	 */
	public ModelBundle runTraining() throws TrainingException {
		var now = roundDownToQuarter(ZonedDateTime.now(this.trainingContext.clockSupplier().get()));
		var trainingFrom = now.minus(//
				this.trainingContext.trainingWindowInQuarters() * MINUTES_PER_QUARTER, //
				ChronoUnit.MINUTES);

		var rawFeatureTargetMatrix = this.trainingDataService.prepareFeatureTargetMatrix(trainingFrom, now);

		var transformedFeatureTargetMatrix = this.featureEngineeringService
				.transformForTraining(rawFeatureTargetMatrix);

		var regressor = this.modelTrainingService.trainRegressor(transformedFeatureTargetMatrix);

		return new ModelBundle(//
				regressor, //
				this.trainingContext.clockSupplier().get().instant());
	}
}
