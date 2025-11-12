package io.openems.edge.predictor.profileclusteringmodel.training;

import com.google.common.annotations.VisibleForTesting;

import io.openems.edge.predictor.api.common.TrainingException;
import io.openems.edge.predictor.profileclusteringmodel.TrainingCallback.ModelBundle;
import io.openems.edge.predictor.profileclusteringmodel.services.ClassifierTrainingService;
import io.openems.edge.predictor.profileclusteringmodel.services.FeatureEngineeringService;
import io.openems.edge.predictor.profileclusteringmodel.services.ProfileClusteringTrainingService;
import io.openems.edge.predictor.profileclusteringmodel.services.TimeSeriesPreprocessingService;
import io.openems.edge.predictor.profileclusteringmodel.services.TrainingDataService;

public class TrainingOrchestrator {

	private final TrainingContext trainingContext;

	private final TrainingDataService trainingDataService;
	private final TimeSeriesPreprocessingService timeSeriesPreprocessingService;
	private final ProfileClusteringTrainingService profileClusteringTrainingService;
	private final FeatureEngineeringService featureEngineeringService;
	private final ClassifierTrainingService classifierTrainingService;

	public TrainingOrchestrator(TrainingContext trainingContext) {
		this.trainingContext = trainingContext;

		this.trainingDataService = new TrainingDataService(//
				trainingContext.timedata(), //
				trainingContext.clockSupplier(), //
				trainingContext.channelAddress());
		this.timeSeriesPreprocessingService = new TimeSeriesPreprocessingService(//
				trainingContext.maxGapSizeInterpolation());
		this.profileClusteringTrainingService = new ProfileClusteringTrainingService(//
				trainingContext.clustererFitter());
		this.featureEngineeringService = new FeatureEngineeringService(//
				trainingContext.subdivisionCodeSupplier());
		this.classifierTrainingService = new ClassifierTrainingService(//
				trainingContext.classifierFitter(), //
				trainingContext.minTrainingSamplesRequired());
	}

	/**
	 * Runs the complete training process for the model.
	 * 
	 * @return a {@link ModelBundle} containing the trained model components
	 * @throws TrainingException if training fails
	 */
	public ModelBundle runTraining() throws TrainingException {
		var rawTimeSeries = this.trainingDataService.fetchSeriesForWindow(//
				this.trainingContext.trainingWindow());

		var timeSeriesPerDay = this.timeSeriesPreprocessingService.preprocessTimeSeriesForTraining(rawTimeSeries);

		var clusteringResult = this.profileClusteringTrainingService.clusterTimeSeries(timeSeriesPerDay);

		var featureEngineeringResult = this.featureEngineeringService
				.transformForTraining(clusteringResult.rawClusterLabelMatrix());

		var transformedFeatureLabelMatrix = featureEngineeringResult.featureLabelMatrix();
		var classifier = this.classifierTrainingService.trainClassifier(transformedFeatureLabelMatrix);

		return new ModelBundle(//
				clusteringResult.clusterer(), //
				classifier, //
				featureEngineeringResult.oneHotEncoder(), //
				this.trainingContext.clockSupplier().get().instant());
	}

	@VisibleForTesting
	TrainingOrchestrator(//
			TrainingContext trainingContext, //
			TrainingDataService trainingDataService, //
			TimeSeriesPreprocessingService timeSeriesPreprocessingService, //
			ProfileClusteringTrainingService profileClusteringTrainingService, //
			FeatureEngineeringService featureEngineeringService, //
			ClassifierTrainingService classifierTrainingService) {
		this.trainingContext = trainingContext;

		this.trainingDataService = trainingDataService;
		this.timeSeriesPreprocessingService = timeSeriesPreprocessingService;
		this.profileClusteringTrainingService = profileClusteringTrainingService;
		this.featureEngineeringService = featureEngineeringService;
		this.classifierTrainingService = classifierTrainingService;
	}
}
