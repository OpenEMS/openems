package io.openems.edge.predictor.profileclusteringmodel;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import io.openems.edge.predictor.api.mlcore.classification.Classifier;
import io.openems.edge.predictor.api.mlcore.clustering.Clusterer;
import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;
import io.openems.edge.predictor.api.mlcore.datastructures.Series;
import io.openems.edge.predictor.profileclusteringmodel.prediction.PredictionContext;
import io.openems.edge.predictor.profileclusteringmodel.prediction.PredictionOrchestrator;
import io.openems.edge.predictor.profileclusteringmodel.prediction.ProfileSwitcher;

public interface PredictorConfig {

	@FunctionalInterface
	public interface PredictionOrchestratorFactory {

		/**
		 * Creates a PredictionOrchestrator using the given context.
		 *
		 * @param context the prediction context
		 * @return a new PredictionOrchestrator instance
		 */
		public PredictionOrchestrator create(PredictionContext context);
	}

	@FunctionalInterface
	public interface ProfileSwitcherFactory {

		/**
		 * Creates a ProfileSwitcher instance.
		 *
		 * @param allProfiles    list of all profiles
		 * @param currentProfile the current profile
		 * @param todaysValues   today's values series
		 * @return a new ProfileSwitcher
		 */
		public ProfileSwitcher create(//
				List<Profile> allProfiles, //
				Profile currentProfile, //
				Series<Integer> todaysValues);
	}

	@FunctionalInterface
	public interface ClustererFitter {

		/**
		 * Fits a clustering model using the provided feature data.
		 *
		 * @param features the feature data as a {@link DataFrame}
		 * @return a trained {@link Clusterer} instance
		 */
		public Clusterer fit(DataFrame<LocalDate> features);
	}

	@FunctionalInterface
	public interface ClassifierFitter {

		/**
		 * Fits a classification model using the provided feature data and labels.
		 *
		 * @param features the feature data as a {@link DataFrame}
		 * @param labels   the labels associated with the features
		 * @return a trained {@link Classifier} instance
		 */
		public Classifier fit(DataFrame<LocalDate> features, Series<LocalDate> labels);
	}

	/**
	 * The minimum number of days to be used for the training window.
	 *
	 * @return the minimum number of training days
	 */
	public int minTrainingWindowDays();

	/**
	 * The maximum number of days to be used for the training window.
	 *
	 * @return the maximum number of training days
	 */
	public int maxTrainingWindowDays();

	/**
	 * The maximum allowed size of gaps (in quarters) in the data that will still be
	 * filled by interpolation.
	 *
	 * @return the maximum gap size for interpolation, in quarters
	 */
	public int maxGapSizeInterpolationInQuarters();

	/**
	 * The minimum number of training samples (whole, valid days) required to train
	 * the classification model. If there are fewer samples, training should not
	 * proceed.
	 *
	 * @return the minimum number of training samples
	 */
	public int minTrainingSamplesRequired();

	/**
	 * Provides the {@link ClustererFitter} that is responsible for training
	 * clustering models.
	 *
	 * @return the clusterer fitter
	 */
	public ClustererFitter clustererFitter();

	/**
	 * Provides the {@link ClassifierFitter} that is responsible for training
	 * classification models.
	 *
	 * @return the classifier fitter
	 */
	public ClassifierFitter classifierFitter();

	/**
	 * The maximum age that a trained model is considered valid. After this
	 * duration, predictions will no longer be allowed.
	 *
	 * @return the maximum model age
	 */
	public Duration maxModelAge();

	/**
	 * Provides a factory that creates a new {@link ProfileSwitcher} instance.
	 *
	 * @return the profile switcher factory
	 */
	public ProfileSwitcherFactory profileSwitcherFactory();

	/**
	 * Provides a factory that creates a new {@link PredictionOrchestrator}
	 * instance.
	 *
	 * @return the prediction orchestrator factory
	 */
	public PredictionOrchestratorFactory predictionOrchestratorFactory();

	/**
	 * Defines how often (in days) the training process should be performed.
	 *
	 * @return the training interval in days
	 */
	public int trainingIntervalInDays();

	/**
	 * Defines the number of days for which predictions should be generated.
	 *
	 * @return the forecast horizon in days
	 */
	public int forecastDays();
}
