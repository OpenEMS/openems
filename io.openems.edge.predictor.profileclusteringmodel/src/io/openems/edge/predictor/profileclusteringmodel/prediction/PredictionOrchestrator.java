package io.openems.edge.predictor.profileclusteringmodel.prediction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import com.google.common.annotations.VisibleForTesting;

import io.openems.edge.predictor.api.common.PredictionException;
import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;
import io.openems.edge.predictor.api.mlcore.datastructures.Series;
import io.openems.edge.predictor.profileclusteringmodel.ColumnNames;
import io.openems.edge.predictor.profileclusteringmodel.CurrentProfile;
import io.openems.edge.predictor.profileclusteringmodel.Profile;
import io.openems.edge.predictor.profileclusteringmodel.services.FeatureEngineeringService;
import io.openems.edge.predictor.profileclusteringmodel.services.PredictionDataService;
import io.openems.edge.predictor.profileclusteringmodel.services.ProfileClusteringPredictionService;
import io.openems.edge.predictor.profileclusteringmodel.services.QueryWindow;
import io.openems.edge.predictor.profileclusteringmodel.services.TimeSeriesPreprocessingService;

public class PredictionOrchestrator {

	private static final QueryWindow WINDOW_ONE_DAY = new QueryWindow(1);

	private final PredictionContext predictionContext;

	private final PredictionDataService predictionDataService;
	private final TimeSeriesPreprocessingService timeSeriesPreprocessingService;
	private final ProfileClusteringPredictionService profileClusteringPredictionService;
	private final FeatureEngineeringService featureEngineeringService;

	public PredictionOrchestrator(PredictionContext predictionContext) {
		this.predictionContext = predictionContext;

		this.predictionDataService = new PredictionDataService(//
				predictionContext.timedata(), //
				predictionContext.clockSupplier(), //
				predictionContext.channelAddress());
		this.timeSeriesPreprocessingService = new TimeSeriesPreprocessingService(
				predictionContext.maxGapSizeInterpolation());
		this.profileClusteringPredictionService = new ProfileClusteringPredictionService(//
				predictionContext.clusterer());
		this.featureEngineeringService = new FeatureEngineeringService(//
				predictionContext.subdivisionCodeSupplier());
	}

	/**
	 * Predicts the daily profiles for the specified number of forecast days.
	 *
	 * @param forecastDays the number of days to predict profiles for; must be at
	 *                     least 1
	 * @return a list of predicted {@link Profile}s, each representing the expected
	 *         cluster profile for a day
	 * @throws PredictionException if fetching or preprocessing of time series data
	 *                             fails
	 */
	public List<Profile> predictProfiles(int forecastDays) throws PredictionException {
		if (forecastDays < 1) {
			throw new IllegalArgumentException("Forecast days must be at least 1");
		}

		var baseFeatureMatrix = this.getBaseFeatureMatrixForLastDay();

		baseFeatureMatrix = this.predictAndSetProfileForToday(baseFeatureMatrix);

		var today = LocalDate.now(this.predictionContext.clockSupplier().get());
		for (int dayOffset = 1; dayOffset < forecastDays; dayOffset++) {
			baseFeatureMatrix = this.predictNewProfileFor(today.plusDays(dayOffset), baseFeatureMatrix);
		}

		var predictedProfiles = new ArrayList<Profile>();
		for (int dayOffset = 0; dayOffset < forecastDays; dayOffset++) {
			var date = today.plusDays(dayOffset);
			int clusterIndex = baseFeatureMatrix.getValue(date, ColumnNames.LABEL).intValue();
			var centroid = this.predictionContext.clusterer().getCentroids().get(clusterIndex);
			predictedProfiles.add(Profile.fromArray(clusterIndex, centroid));
		}

		return predictedProfiles;
	}

	private DataFrame<LocalDate> getBaseFeatureMatrixForLastDay() throws PredictionException {
		var rawTimeSeries = this.predictionDataService.fetchSeriesForWindow(WINDOW_ONE_DAY);
		var timeSeriesByDate = this.timeSeriesPreprocessingService.preprocessTimeSeriesForPrediction(rawTimeSeries);
		var baseFeatureMatrix = this.profileClusteringPredictionService.predictClusterLabels(timeSeriesByDate);
		return baseFeatureMatrix;
	}

	private DataFrame<LocalDate> predictAndSetProfileForToday(DataFrame<LocalDate> baseFeatureMatrix)
			throws PredictionException {
		var today = LocalDate.now(this.predictionContext.clockSupplier().get());

		if (isOutdated(this.predictionContext.currentProfile(), today)) {
			return this.predictNewProfileFor(today, baseFeatureMatrix);
		}

		var selectedProfile = this.tryFindBetterProfileForToday()//
				.orElse(this.predictionContext.currentProfile().profile());
		baseFeatureMatrix.addEmptyRow(today);
		baseFeatureMatrix.setValue(today, ColumnNames.LABEL, (double) selectedProfile.clusterIndex());

		return baseFeatureMatrix;
	}

	private DataFrame<LocalDate> predictNewProfileFor(LocalDate date, DataFrame<LocalDate> baseFeatureMatrix) {
		baseFeatureMatrix.addEmptyRow(date);
		baseFeatureMatrix = this.featureEngineeringService.transformBaseFeatureMatrixForPrediction(baseFeatureMatrix);

		var predictionFeatureMatrix = this.featureEngineeringService.transformForPrediction(//
				baseFeatureMatrix, //
				this.predictionContext.oneHotEncoder());

		var sampleToPredict = predictionFeatureMatrix.getRow(date);
		var prediction = this.predictionContext.classifier().predict(sampleToPredict);
		baseFeatureMatrix.setValue(date, ColumnNames.LABEL, (double) prediction);
		return baseFeatureMatrix;
	}

	private Optional<Profile> tryFindBetterProfileForToday() throws PredictionException {
		var todaysValues = this.buildTodaySeries();
		if (todaysValues == null) {
			return Optional.empty();
		}

		var allProfiles = this.buildAllProfiles();
		var currentProfile = this.predictionContext.currentProfile().profile();

		var profileSwitcher = this.predictionContext.profileSwitcherFactory()//
				.create(allProfiles, currentProfile, todaysValues);
		return profileSwitcher.findBetterProfile();
	}

	private Series<Integer> buildTodaySeries() throws PredictionException {
		var rawValues = this.predictionDataService.fetchSeriesForToday().getValues();
		if (rawValues.isEmpty()) {
			return null;
		}

		var values = new ArrayList<>(rawValues);
		values.addAll(Collections.nCopies(Profile.LENGTH - values.size(), Double.NaN));
		var index = IntStream.range(0, Profile.LENGTH).boxed().toList();
		return new Series<>(index, values);
	}

	private List<Profile> buildAllProfiles() {
		var centroids = this.predictionContext.clusterer().getCentroids();
		return IntStream.range(0, centroids.size()).mapToObj(i -> Profile.fromArray(i, centroids.get(i))).toList();
	}

	private static boolean isOutdated(CurrentProfile currentProfile, LocalDate date) {
		return currentProfile == null || !date.equals(currentProfile.date());
	}

	@VisibleForTesting
	PredictionOrchestrator(//
			PredictionContext predictionContext, //
			PredictionDataService predictionDataService, //
			TimeSeriesPreprocessingService timeSeriesPreprocessingService, //
			ProfileClusteringPredictionService profileClusteringPredictionService, //
			FeatureEngineeringService featureEngineeringService) {
		this.predictionContext = predictionContext;
		this.predictionDataService = predictionDataService;
		this.timeSeriesPreprocessingService = timeSeriesPreprocessingService;
		this.profileClusteringPredictionService = profileClusteringPredictionService;
		this.featureEngineeringService = featureEngineeringService;
	}
}
