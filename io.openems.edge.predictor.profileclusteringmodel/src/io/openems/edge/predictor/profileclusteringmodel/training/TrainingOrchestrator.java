package io.openems.edge.predictor.profileclusteringmodel.training;

import java.time.LocalDate;

import com.google.common.annotations.VisibleForTesting;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.predictor.api.mlcore.classification.Classifier;
import io.openems.edge.predictor.api.mlcore.clustering.Clusterer;
import io.openems.edge.predictor.api.mlcore.transformer.OneHotEncoder;
import io.openems.edge.predictor.profileclusteringmodel.TrainingCallback.ModelBundle;
import io.openems.edge.predictor.profileclusteringmodel.services.ClassifierTrainingService;
import io.openems.edge.predictor.profileclusteringmodel.services.FeatureEngineeringService;
import io.openems.edge.predictor.profileclusteringmodel.services.ProfileClusteringTrainingService;
import io.openems.edge.predictor.profileclusteringmodel.services.RawTimeSeriesService;
import io.openems.edge.predictor.profileclusteringmodel.services.TimeSeriesPreprocessingService;

public class TrainingOrchestrator {

	private final TrainingContext trainingContext;

	private final RawTimeSeriesService rawTimeSeriesService;
	private final TimeSeriesPreprocessingService timeSeriesPreprocessingService;
	private final ProfileClusteringTrainingService profileClusteringTrainingService;
	private final FeatureEngineeringService featureEngineeringService;
	private final ClassifierTrainingService classifierTrainingService;

	public TrainingOrchestrator(TrainingContext trainingContext) {
		this.trainingContext = trainingContext;

		this.rawTimeSeriesService = new RawTimeSeriesService(//
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
	 * Runs the complete training process for the model. Trained models are saved in
	 * the parent component for later use in prediction.
	 * 
	 * @throws OpenemsNamedException if training fails
	 */
	public void runTraining() throws OpenemsNamedException {
		var rawTimeSeries = this.rawTimeSeriesService.fetchSeriesForWindow(//
				this.trainingContext.trainingWindow());

		var timeSeriesPerDay = this.timeSeriesPreprocessingService.preprocessTimeSeries(rawTimeSeries);

		var clusteringResult = this.profileClusteringTrainingService.clusterTimeSeries(timeSeriesPerDay);

		var featureEngineeringResult = this.featureEngineeringService
				.transformForTraining(clusteringResult.rawClusterLabelMatrix());

		var transformedFeatureLabelMatrix = featureEngineeringResult.featureLabelMatrix();
		var classifier = this.classifierTrainingService.trainClassifier(transformedFeatureLabelMatrix);

		this.saveModels(//
				clusteringResult.clusterer(), //
				classifier, //
				featureEngineeringResult.oneHotEncoder());
	}

	private void saveModels(//
			Clusterer clusterer, //
			Classifier classifier, //
			OneHotEncoder<LocalDate> oneHotEncoder) {
		var bundle = new ModelBundle(//
				clusterer, //
				classifier, //
				oneHotEncoder, //
				this.trainingContext.clockSupplier().get().instant());
		this.trainingContext.trainingCallback().onModelsTrained(bundle);
	}

	@VisibleForTesting
	TrainingOrchestrator(//
			TrainingContext trainingContext, //
			RawTimeSeriesService rawTimeSeriesService, //
			TimeSeriesPreprocessingService timeSeriesPreprocessingService, //
			ProfileClusteringTrainingService profileClusteringTrainingService, //
			FeatureEngineeringService featureEngineeringService, //
			ClassifierTrainingService classifierTrainingService) {
		this.trainingContext = trainingContext;

		this.rawTimeSeriesService = rawTimeSeriesService;
		this.timeSeriesPreprocessingService = timeSeriesPreprocessingService;
		this.profileClusteringTrainingService = profileClusteringTrainingService;
		this.featureEngineeringService = featureEngineeringService;
		this.classifierTrainingService = classifierTrainingService;
	}
}
