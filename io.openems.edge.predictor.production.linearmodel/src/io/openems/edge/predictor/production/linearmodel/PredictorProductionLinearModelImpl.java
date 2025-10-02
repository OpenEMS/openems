package io.openems.edge.predictor.production.linearmodel;

import static io.openems.common.utils.DateUtils.roundDownToQuarter;
import static io.openems.common.utils.ThreadPoolUtils.shutdownAndAwaitTermination;
import static io.openems.edge.common.jsonapi.EdgeGuards.roleIsAtleast;
import static io.openems.edge.predictor.api.prediction.Prediction.EMPTY_PREDICTION;
import static io.openems.edge.predictor.production.linearmodel.PredictionState.PREDICTING;
import static io.openems.edge.predictor.production.linearmodel.PredictionState.PREDICTION_FAILED;
import static io.openems.edge.predictor.production.linearmodel.PredictionState.PREDICTION_FAILED_MODEL_LOADING;
import static io.openems.edge.predictor.production.linearmodel.PredictionState.PREDICTION_FAILED_MODEL_TOO_OLD;
import static io.openems.edge.predictor.production.linearmodel.PredictionState.PREDICTION_FAILED_WEATHER_FORECAST;
import static io.openems.edge.predictor.production.linearmodel.Utils.getWeatherDataFeatureMatrix;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.OpenemsConstants;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.Role;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.FunctionUtils;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.predictor.api.manager.PredictorManager;
import io.openems.edge.predictor.api.prediction.AbstractPredictor;
import io.openems.edge.predictor.api.prediction.Prediction;
import io.openems.edge.predictor.api.prediction.Predictor;
import io.openems.edge.predictor.production.linearmodel.jsonrpc.GetPrediction;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.weather.api.Weather;
import io.openems.edge.weather.api.WeatherData;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Predictor.Production.LinearModel", //
		immediate = true, //
		configurationPolicy = REQUIRE //
)
public class PredictorProductionLinearModelImpl extends AbstractPredictor
		implements PredictorProductionLinearModel, Predictor, OpenemsComponent, ComponentJsonApi {

	private final Logger log = LoggerFactory.getLogger(PredictorProductionLinearModelImpl.class);
	private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

	@Component
	public static class DefaultLocalConfig implements LocalConfig {

		@Override
		public Path modelsDirectoryPath() {
			return Paths.get(OpenemsConstants.getOpenemsDataDir(), "models");
		}

		@Override
		public int trainingIntervalInDays() {
			return 1;
		}

		@Override
		public int trainingWindowInQuarters() {
			return daysToQuarters(30);
		}

		@Override
		public int maxAgeOfModelInDays() {
			return 7;
		}

		@Override
		public int halfLifeInQuarters() {
			return daysToQuarters(3);
		}

		@Override
		public double minTrainingDataRatio() {
			return 0.9;
		}

		private static int daysToQuarters(int days) {
			return days * 24 /* hours */ * 4 /* quarters per hour */;
		}
	}

	@Reference
	private Sum sum;

	@Reference
	private ComponentManager componentManager;

	@Reference
	private Timedata timedata;

	@Reference
	private Weather weather;

	@Reference
	private LocalConfig localConfig;

	@Reference
	private PredictorManager predictorManager;

	public PredictorProductionLinearModelImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				PredictorProductionLinearModel.ChannelId.values() //
		);
	}

	private ChannelAddress channelAddress;
	private ModelSerializer modelSerializer;
	private PredictionPersistenceService predictionPersistenceService;

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.logVerbosity(),
				config.sourceChannel().channelAddress);

		this.predictionPersistenceService = new PredictionPersistenceService(//
				this, //
				this.timedata, //
				() -> this.componentManager.getClock());
		
		if (!config.enabled()) {
			return;
		}

		this.modelSerializer = new ModelSerializer(this.localConfig.modelsDirectoryPath());

		this.executor.scheduleAtFixedRate(//
				new ModelFitRunnable(//
						this.componentManager, //
						this.timedata, //
						this.weather, //
						this.localConfig, //
						this::_setTrainingState, //
						this.modelSerializer, //
						config.sourceChannel().channelAddress),
				0, //
				this.localConfig.trainingIntervalInDays(), //
				TimeUnit.DAYS //
		);

		this.predictionPersistenceService.startShiftingJob();
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		shutdownAndAwaitTermination(this.executor, 5);
		this.predictionPersistenceService.deactivateShiftingJob();
	}

	@Override
	protected ClockProvider getClockProvider() {
		return this.componentManager;
	}

	@Override
	protected Prediction createNewPrediction(ChannelAddress channelAddress) {
		var modelConfigState = this.loadModelConfigState();
		if (modelConfigState == null) {
			return EMPTY_PREDICTION;
		}

		var now = roundDownToQuarter(ZonedDateTime.now(this.componentManager.getClock()));

		if (this.isModelTooOld(modelConfigState, now)) {
			this.logModelTooOldError();
			return EMPTY_PREDICTION;
		}

		final WeatherData weatherForecast;
		try {
			weatherForecast = this.getWeatherForecast();
		} catch (Exception e) {
			this._setPredictionState(PREDICTION_FAILED_WEATHER_FORECAST);
			this.logError(this.log, "Weather Forecast failed: " + e.getMessage());
			return EMPTY_PREDICTION;
		}

		var weatherFeatures = getWeatherDataFeatureMatrix(//
				weatherForecast, //
				ModelConfigState.getWeatherInputFeatures(), //
				ModelConfigState.isIncludeDayTimeFeatures()//
		);

		var predictedValues = this.predictValues(weatherFeatures, modelConfigState.betas());
		if (predictedValues == null) {
			this._setPredictionState(PREDICTION_FAILED);
			return EMPTY_PREDICTION;
		}

		var prediction = Prediction.from(this.sum, channelAddress, now, predictedValues);
		this._setPredictionState(PREDICTING);

		switch (this.getLogVerbosity()) {
		case NONE, REQUESTED_PREDICTIONS -> FunctionUtils.doNothing();
		case ARCHIVE_LOCALLY -> this.archivePrediction(prediction);
		}

		this.predictionPersistenceService.updatePredictionAheadChannels(prediction);

		return prediction;
	}

	private ModelConfigState loadModelConfigState() {
		try {
			return this.modelSerializer.readModelConfigState();
		} catch (IOException e) {
			this._setPredictionState(PREDICTION_FAILED_MODEL_LOADING);
			this.logError(this.log, "Failed to load linear model: " + e.getMessage());
			return null;
		}
	}

	private boolean isModelTooOld(ModelConfigState modelConfigState, ZonedDateTime now) {
		return modelConfigState.lastTrainedDate().isBefore(now.minusDays(this.localConfig.maxAgeOfModelInDays()));
	}

	private void logModelTooOldError() {
		this._setPredictionState(PREDICTION_FAILED_MODEL_TOO_OLD);
		this.logError(this.log, "Failed to predict. " //
				+ "Available Linear model is older than " + this.localConfig.maxAgeOfModelInDays() + " days");
	}

	private WeatherData getWeatherForecast() throws OpenemsException {
		var weatherForecast = this.weather.getWeatherForecast();

		if (weatherForecast == null || WeatherData.EMPTY_WEATHER_DATA.equals(weatherForecast)) {
			throw new OpenemsException("Weather data is null or empty");
		}

		return weatherForecast;
	}

	private Integer[] predictValues(double[][] features, double[] betas) {
		try {
			return Arrays.stream(predict(features, betas))//
					.mapToInt(value -> value < 0 ? 0 : (int) Math.round(value)) //
					.boxed()//
					.toArray(Integer[]::new);
		} catch (IllegalArgumentException e) {
			this.logError(this.log, "Failed to predict with linear model: " + e.getMessage());
			return null;
		}
	}

	private static double[] predict(double[][] features, double[] betas) throws IllegalArgumentException {
		int numFeatures = features[0].length;
		int numBetas = betas.length - 1;

		if (numFeatures != numBetas) {
			throw new IllegalArgumentException(String.format(
					"Mismatch: X has %d features, but betas expects %d (excluding intercept)", numFeatures, numBetas));
		}

		int numSamples = features.length;
		double[] predictions = new double[numSamples];

		for (int i = 0; i < numSamples; i++) {
			double prediction = betas[0]; // Intercept

			for (int j = 0; j < numFeatures; j++) {
				prediction += betas[j + 1] * features[i][j];
			}

			predictions[i] = prediction;
		}

		return predictions;
	}

	private void archivePrediction(Prediction prediction) {
		var datetime = prediction.getFirstTime();
		var data = prediction.asArray();

		var line = datetime.toString() + "," //
				+ Arrays.stream(data)//
						.map(String::valueOf)//
						.collect(Collectors.joining(","));

		var modelsDirectoryPath = this.localConfig.modelsDirectoryPath();
		var file = modelsDirectoryPath.resolve("predictions_linearmodel.csv");

		try {
			Files.createDirectories(modelsDirectoryPath);

			try (var writer = Files.newBufferedWriter(file, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
				writer.write(line);
				writer.newLine();
			}
		} catch (IOException e) {
			this.log.error("Failed to archive prediction", e);
		}
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.handleRequest(new GetPrediction(), //
				endpoint -> {
					endpoint.setGuards(roleIsAtleast(Role.OWNER));
				}, call -> {
					var prediction = this.createNewPrediction(this.channelAddress);
					return new GetPrediction.Response(prediction);
				});
	}

	public interface LocalConfig {

		/**
		 * Path to the directory where models are stored.
		 * 
		 * @return the path to the models directory.
		 */
		Path modelsDirectoryPath();

		/**
		 * The interval in days at which the model will be retrained.
		 * 
		 * @return the training interval in days.
		 */
		int trainingIntervalInDays();

		/**
		 * The training window in quarters, specifying how many historical data points
		 * are used for training.
		 * 
		 * @return the training window in quarters.
		 */
		int trainingWindowInQuarters();

		/**
		 * The maximum age in days that a model can have before it is considered
		 * outdated.
		 * 
		 * @return the maximum model age in days.
		 */
		int maxAgeOfModelInDays();

		/**
		 * The half-life period in quarters for exponential weighting in the regression.
		 * This defines the time interval after which the weight is reduced to half.
		 *
		 * @return the half-life duration in quarters
		 */
		int halfLifeInQuarters();

		/**
		 * The minimum required ratio of training data for the model to be trainable.
		 *
		 * @return the minimum training data ratio
		 */
		double minTrainingDataRatio();
	}
}
