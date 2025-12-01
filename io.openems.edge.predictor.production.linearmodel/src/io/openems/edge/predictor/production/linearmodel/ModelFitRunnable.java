package io.openems.edge.predictor.production.linearmodel;

import static io.openems.common.utils.DateUtils.roundDownToQuarter;
import static io.openems.edge.predictor.production.linearmodel.TrainingState.TRAINED;
import static io.openems.edge.predictor.production.linearmodel.TrainingState.TRAINING;
import static io.openems.edge.predictor.production.linearmodel.TrainingState.TRAINING_FAILED_DATA_VALIDATION;
import static io.openems.edge.predictor.production.linearmodel.TrainingState.TRAINING_FAILED_INSUFFICIENT_DATA;
import static io.openems.edge.predictor.production.linearmodel.TrainingState.TRAINING_FAILED_MODEL_SAVING;
import static io.openems.edge.predictor.production.linearmodel.TrainingState.TRAINING_FAILED_MODEL_TRAINING;
import static io.openems.edge.predictor.production.linearmodel.TrainingState.TRAINING_FAILED_PRODUCTION_FETCH;
import static io.openems.edge.predictor.production.linearmodel.TrainingState.TRAINING_FAILED_WEATHER_FETCH;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.predictor.production.linearmodel.PredictorProductionLinearModelImpl.LocalConfig;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.weather.api.QuarterlyWeatherSnapshot;
import io.openems.edge.weather.api.Weather;

public class ModelFitRunnable implements Runnable {

	private static final double LN2 = Math.log(2);
	private static final int MINUTES_PER_QUARTER = 15;

	private final Logger log = LoggerFactory.getLogger(ModelFitRunnable.class);

	private final ComponentManager componentManager;
	private final Timedata timedata;
	private final Weather weather;
	private final LocalConfig localConfig;

	private final Consumer<TrainingState> setTrainingStateChannel;
	private final ModelSerializer modelSerializer;
	private final ChannelAddress channelAddress;

	public ModelFitRunnable(//
			ComponentManager componentManager, //
			Timedata timedata, //
			Weather weather, //
			LocalConfig localConfig, //
			Consumer<TrainingState> setTrainingStateChannel, //
			ModelSerializer modelSerializer, //
			ChannelAddress channelAddress) {
		this.componentManager = componentManager;
		this.timedata = timedata;
		this.weather = weather;
		this.localConfig = localConfig;
		this.setTrainingStateChannel = setTrainingStateChannel;
		this.modelSerializer = modelSerializer;
		this.channelAddress = channelAddress;
	}

	@Override
	public void run() {
		this.setTrainingStateChannel.accept(TRAINING);

		var now = roundDownToQuarter(ZonedDateTime.now(this.componentManager.getClock()));
		var trainingFrom = now.minus(//
				this.localConfig.trainingWindowInQuarters() * MINUTES_PER_QUARTER, //
				ChronoUnit.MINUTES);

		try {
			var trainingData = this.fetchTrainingData(trainingFrom, now);
			var modelConfigState = this.trainModel(trainingData, now);
			this.saveModel(modelConfigState);
			this.setTrainingStateChannel.accept(TRAINED);
			this.log.info("Successfully trained and saved linear model on {}", modelConfigState.lastTrainedDate());
		} catch (Exception e) {
			this.log.error("Cannot train linear model: ", e);
		}
	}

	private TrainingData fetchTrainingData(ZonedDateTime from, ZonedDateTime to) throws Exception {
		final List<QuarterlyWeatherSnapshot> weatherData;
		try {
			weatherData = this.fetchHistoricalWeatherData(from, to);
		} catch (Exception e) {
			this.setTrainingStateChannel.accept(TRAINING_FAILED_WEATHER_FETCH);
			throw new OpenemsException("Cannot fetch historical weather data", e);
		}

		final SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> productionData;
		try {
			productionData = this.fetchHistoricalProductionData(from, to);
		} catch (Exception e) {
			this.setTrainingStateChannel.accept(TRAINING_FAILED_PRODUCTION_FETCH);
			throw new OpenemsException("Cannot fetch historical production data", e);
		}

		var retained = retainCommonTimestamps(weatherData, productionData);
		return new TrainingData(retained.getFirst(), retained.getSecond());
	}

	private List<QuarterlyWeatherSnapshot> fetchHistoricalWeatherData(ZonedDateTime from, ZonedDateTime to)
			throws InterruptedException, ExecutionException, OpenemsException {
		var weatherData = this.weather.getHistoricalWeather(from.toLocalDate(), to.toLocalDate(), from.getZone()).get();

		if (weatherData == null || weatherData.isEmpty()) {
			throw new OpenemsException("Weather data is null or empty");
		}

		return weatherData;
	}

	private SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> fetchHistoricalProductionData(
			ZonedDateTime from, ZonedDateTime to) throws OpenemsNamedException {
		var productionData = this.timedata.queryHistoricData(//
				null, //
				from, //
				to, //
				Sets.newHashSet(this.channelAddress), //
				new Resolution(MINUTES_PER_QUARTER, ChronoUnit.MINUTES));

		if (productionData == null || productionData.isEmpty()) {
			throw new OpenemsException("Production data is null or empty");
		}

		return productionData;
	}

	private ModelConfigState trainModel(TrainingData data, ZonedDateTime now) throws OpenemsException {
		var features = Utils.getWeatherDataFeatureMatrix(//
				data.weatherData(), //
				ModelConfigState.getWeatherInputFeatures(), //
				ModelConfigState.isIncludeDayTimeFeatures());
		var targets = getTargetVector(data.productionData());

		final CleanedData cleaned;
		try {
			cleaned = validateAndCleanData(features, targets);
		} catch (Exception e) {
			this.setTrainingStateChannel.accept(TRAINING_FAILED_DATA_VALIDATION);
			throw new OpenemsException("Error validating an cleaning training data", e);
		}
		features = cleaned.features;
		targets = cleaned.targets;

		if (features.length < this.localConfig.minTrainingDataRatio() * this.localConfig.trainingWindowInQuarters()) {
			this.setTrainingStateChannel.accept(TRAINING_FAILED_INSUFFICIENT_DATA);
			throw new OpenemsException("Insufficient data points available for training");
		}

		var weights = computeExponentialWeights(features.length, this.localConfig.halfLifeInQuarters());
		var weighted = applyWeights(features, targets, weights);
		features = weighted.features;
		targets = weighted.targets;

		var model = new OLSMultipleLinearRegression();

		try {
			model.newSampleData(targets, features);
			var coefficients = model.estimateRegressionParameters();
			return new ModelConfigState(now, coefficients);
		} catch (Exception e) {
			this.setTrainingStateChannel.accept(TRAINING_FAILED_MODEL_TRAINING);
			throw new OpenemsException("Failed to train linear regression model", e);
		}
	}

	private void saveModel(ModelConfigState modelConfigState) throws OpenemsException {
		try {
			this.modelSerializer.saveModelConfigState(modelConfigState);
		} catch (IOException e) {
			this.setTrainingStateChannel.accept(TRAINING_FAILED_MODEL_SAVING);
			throw new OpenemsException("Cannot save linear model", e);
		}
	}

	private static Pair<List<QuarterlyWeatherSnapshot>, SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>>> retainCommonTimestamps(
			List<QuarterlyWeatherSnapshot> weatherData,
			SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> productionData) {
		var weatherMap = weatherData.stream()//
				.collect(Collectors.toMap(//
						QuarterlyWeatherSnapshot::datetime, //
						Function.identity(), //
						(first, duplicate) -> first));

		var commonTimestamps = new TreeSet<>(weatherMap.keySet());
		commonTimestamps.retainAll(productionData.keySet());

		var retainedWeather = commonTimestamps.stream()//
				.map(weatherMap::get)//
				.toList();

		var retainedProduction = new TreeMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>>();
		commonTimestamps.forEach(ts -> retainedProduction.put(ts, productionData.get(ts)));

		return new Pair<>(retainedWeather, retainedProduction);
	}

	private static double[] getTargetVector(
			SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> channelValues) {
		return channelValues.values().stream()//
				.map(SortedMap::values)//
				.flatMap(Collection::stream)//
				.mapToDouble(elem -> elem.isJsonNull() ? Double.NaN : elem.getAsDouble())//
				.toArray();
	}

	private static CleanedData validateAndCleanData(double[][] features, double[] targets) throws OpenemsException {
		if (features.length != targets.length) {
			throw new OpenemsException(String.format("Mismatched data sizes: features has %d rows, targets has %d rows",
					features.length, targets.length));
		}

		double[][] tempFeatures = new double[features.length][];
		double[] tempTargets = new double[targets.length];
		int validRowCount = 0;

		for (int i = 0; i < features.length; i++) {
			double[] sample = features[i];
			double target = targets[i];

			// Check feature values for NaN or infinite values
			if (Arrays.stream(sample).anyMatch(value -> Double.isNaN(value) || Double.isInfinite(value))) {
				continue;
			}

			// Check target value for NaN, infinite, or negative values
			if (Double.isNaN(target) || Double.isInfinite(target) || target < 0) {
				continue;
			}

			tempFeatures[validRowCount] = sample;
			tempTargets[validRowCount] = target;
			validRowCount++;
		}

		double[][] cleanedFeatures = new double[validRowCount][];
		double[] cleanedTargets = new double[validRowCount];

		System.arraycopy(tempFeatures, 0, cleanedFeatures, 0, validRowCount);
		System.arraycopy(tempTargets, 0, cleanedTargets, 0, validRowCount);

		return new CleanedData(cleanedFeatures, cleanedTargets);
	}

	private static double[] computeExponentialWeights(int length, int halfLife) {
		if (halfLife <= 0) {
			throw new IllegalArgumentException("Half-life must be > 0");
		}
		final double decayRate = LN2 / halfLife;
		final int offset = length - 1;
		final double[] weights = new double[length];
		for (int i = 0; i < length; i++) {
			weights[i] = Math.exp(-decayRate * (offset - i));
		}
		return weights;
	}

	private static WeightedData applyWeights(double[][] features, double[] targets, double[] weights) {
		final int nSamples = targets.length;
		final int nFeatures = features[0].length;

		double[][] weightedFeatures = new double[nSamples][nFeatures];
		double[] weightedTargets = new double[nSamples];

		for (int i = 0; i < nSamples; i++) {
			final double sqrtWeight = Math.sqrt(weights[i]);

			// Apply weight to all features in the i-th sample
			for (int j = 0; j < nFeatures; j++) {
				weightedFeatures[i][j] = sqrtWeight * features[i][j];
			}

			// Apply weight to the target value
			weightedTargets[i] = sqrtWeight * targets[i];
		}

		return new WeightedData(weightedFeatures, weightedTargets);
	}

	private record TrainingData(List<QuarterlyWeatherSnapshot> weatherData,
			SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> productionData) {
	}

	private record CleanedData(double[][] features, double[] targets) {
	}

	private record WeightedData(double[][] features, double[] targets) {
	}
}
