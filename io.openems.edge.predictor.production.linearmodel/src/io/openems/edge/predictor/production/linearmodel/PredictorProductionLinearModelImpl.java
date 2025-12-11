package io.openems.edge.predictor.production.linearmodel;

import static io.openems.common.utils.DateUtils.roundDownToQuarter;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tribuo.regression.rtree.impurity.MeanSquaredError;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Role;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.jsonapi.EdgeGuards;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.predictor.api.common.PredictionException;
import io.openems.edge.predictor.api.common.PredictionState;
import io.openems.edge.predictor.api.common.TrainingError;
import io.openems.edge.predictor.api.common.TrainingState;
import io.openems.edge.predictor.api.mlcore.datastructures.Series;
import io.openems.edge.predictor.api.mlcore.regression.RandomForestRegressor;
import io.openems.edge.predictor.api.prediction.AbstractPredictor;
import io.openems.edge.predictor.api.prediction.Prediction;
import io.openems.edge.predictor.api.prediction.Predictor;
import io.openems.edge.predictor.persistencemodel.PredictorPersistenceModel;
import io.openems.edge.predictor.production.linearmodel.jsonrpc.GetPredictionEndpoint;
import io.openems.edge.predictor.production.linearmodel.prediction.PredictionContext;
import io.openems.edge.predictor.production.linearmodel.prediction.PredictionOrchestrator;
import io.openems.edge.predictor.production.linearmodel.prediction.SnowStateMachine;
import io.openems.edge.predictor.production.linearmodel.training.TrainingContext;
import io.openems.edge.predictor.production.linearmodel.training.TrainingRunnable;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.weather.api.Weather;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Predictor.Production.LinearModel", //
		immediate = true, //
		configurationPolicy = REQUIRE //
)
public class PredictorProductionLinearModelImpl extends AbstractPredictor
		implements PredictorProductionLinearModel, TrainingCallback, Predictor, OpenemsComponent, ComponentJsonApi {

	private static final long NO_INITIAL_DELAY = 0L;
	private static final int MINUTES_PER_QUARTER = 15;

	private final Logger log = LoggerFactory.getLogger(PredictorProductionLinearModelImpl.class);
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

	@Reference
	private ComponentManager componentManager;

	@Reference
	private Timedata timedata;

	@Reference
	private Weather weather;

	@Reference
	private Sum sum;

	@Reference
	private PredictorConfig predictorConfig;

	@Reference(//
			cardinality = ReferenceCardinality.OPTIONAL, //
			policyOption = ReferencePolicyOption.GREEDY)
	private PredictorPersistenceModel predictorPersistenceModel;

	public PredictorProductionLinearModelImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				PredictorProductionLinearModel.ChannelId.values() //
		);
	}

	private ChannelAddress productionChannelAddress;
	private ModelBundle currentModel;
	private SnowStateMachine snowStateMachine;
	private PredictionPersistenceService predictionPersistenceService;
	private int maxProduction = Integer.MAX_VALUE;

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.logVerbosity(),
				config.sourceChannel().channelAddress);

		if (!config.enabled()) {
			return;
		}

		this.productionChannelAddress = config.sourceChannel().channelAddress;

		this.predictionPersistenceService = new PredictionPersistenceService(//
				this, //
				this.timedata, //
				() -> this.componentManager.getClock());

		final var trainingContext = this.createTrainingContext();
		this.scheduler.scheduleAtFixedRate(//
				new TrainingRunnable(trainingContext), //
				NO_INITIAL_DELAY, //
				this.predictorConfig.trainingIntervalInDays(), //
				TimeUnit.DAYS);

		this.scheduler.scheduleAtFixedRate(//
				this::computeAndSetMaxProduction, //
				NO_INITIAL_DELAY, //
				24, //
				TimeUnit.HOURS);

		// Initialize snowStateMachine with the latest state (or NORMAL if
		// unavailable)
		this.timedata.getLatestValue(this.getSnowStateChannel().address())//
		.<SnowStateMachine.State>thenApply(latestValueOpt -> {
			Integer latestStateValue = TypeUtils.getAsType(OpenemsType.INTEGER, latestValueOpt);
			return latestStateValue != null
					? OptionsEnum.getOptionOrUndefined(SnowStateMachine.State.class, latestStateValue)
					: SnowStateMachine.State.NORMAL;
		})//
				.exceptionally(ex -> SnowStateMachine.State.NORMAL)//
				.thenAccept(snowState -> {
					this.snowStateMachine = new SnowStateMachine(//
							this.weather, //
							this.timedata, //
							() -> this.componentManager.getClock(), //
							this.productionChannelAddress, //
							() -> this.maxProduction, //
							snowState);
				});

		this.predictionPersistenceService.startShiftingJob();
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.scheduler.shutdown();
		this.predictionPersistenceService.deactivateShiftingJob();
		this._setTrainingState(TrainingState.DEACTIVATED);
		this._setPredictionState(PredictionState.DEACTIVATED);
	}

	@Override
	protected Prediction createNewPrediction(ChannelAddress channelAddress) {
		if (this.snowStateMachine == null) {
			this._setPredictionState(PredictionState.FAILED_UNKNOWN);
			this.logPredictionError(PredictionState.FAILED_UNKNOWN, "SnowStateMachine is not initialized");
			return Prediction.EMPTY_PREDICTION;
		}

		try {
			this.snowStateMachine.run();
		} catch (PredictionException e) {
			this._setPredictionState(e.getError().getFailedState());
			this.logPredictionError(e.getError().getFailedState(), e.getMessage());
			return Prediction.EMPTY_PREDICTION;
		}

		var currentState = this.snowStateMachine.getCurrentState();
		this._setSnowState(currentState);

		var prediction = switch (currentState) {
		case NORMAL -> {
			yield this.createLongTermPrediction(channelAddress);
		}
		case SNOW -> {
			var snowStart = this.snowStateMachine.getSnowStart();
			var now = roundDownToQuarter(ZonedDateTime.now(this.componentManager.getClock()));

			boolean withinFirst24h = snowStart != null && Duration.between(snowStart, now).toHours() < 24;
			boolean persistenceModelAvailable = this.predictorPersistenceModel != null;

			if (withinFirst24h || !persistenceModelAvailable) {
				yield this.createLongTermPrediction(channelAddress);
			}

			yield this.predictorPersistenceModel.getPrediction(channelAddress);
		}
		};

		if (prediction == Prediction.EMPTY_PREDICTION) {
			return prediction;
		}

		this.predictionPersistenceService.updatePredictionAheadChannels(prediction);

		this._setPredictionState(PredictionState.SUCCESSFUL);

		return prediction;
	}

	@Override
	public void onTrainingSuccess(ModelBundle bundle) {
		this.currentModel = bundle;
		this._setTrainingState(TrainingState.SUCCESSFUL);
		this.logInfo(this.log, String.format(//
				"Training succeeded [%s]", //
				TrainingState.SUCCESSFUL.getName()));
	}

	@Override
	public void onTrainingError(TrainingError error, String message) {
		this._setTrainingState(error.getFailedState());
		this.logTrainingError(error.getFailedState(), message);
	}

	@VisibleForTesting
	Prediction createLongTermPrediction(ChannelAddress channelAddress) {
		if (this.currentModel == null) {
			this._setPredictionState(PredictionState.FAILED_NO_MODEL);
			this.logPredictionError(PredictionState.FAILED_NO_MODEL, "No trained model available");
			return Prediction.EMPTY_PREDICTION;
		}

		if (this.isModelTooOld(this.currentModel)) {
			this._setPredictionState(PredictionState.FAILED_MODEL_OUTDATED);
			this.logPredictionError(PredictionState.FAILED_MODEL_OUTDATED, "Trained model outdated");
			return Prediction.EMPTY_PREDICTION;
		}

		final var predictionContext = this.createPredictionContext();
		final var predictionOrchestrator = this.predictorConfig.predictionOrchestratorFactory()//
				.create(predictionContext);

		try {
			var predictedValues = predictionOrchestrator.runPrediction();
			return this.mapSeriesToPrediction(predictedValues, channelAddress);
		} catch (PredictionException e) {
			this._setPredictionState(e.getError().getFailedState());
			this.logPredictionError(e.getError().getFailedState(), e.getMessage());
			return Prediction.EMPTY_PREDICTION;
		} catch (Exception e) {
			this._setPredictionState(PredictionState.FAILED_UNKNOWN);
			this.logPredictionError(PredictionState.FAILED_UNKNOWN, e.getMessage());
			return Prediction.EMPTY_PREDICTION;
		}
	}

	private Prediction mapSeriesToPrediction(//
			Series<ZonedDateTime> series, //
			ChannelAddress channelAddress) {
		var now = roundDownToQuarter(ZonedDateTime.now(this.componentManager.getClock()));
		var timestampValueMap = series.toMap();

		var predictedValues = new Integer[this.predictorConfig.forecastQuarters()];
		for (int i = 0; i < this.predictorConfig.forecastQuarters(); i++) {
			var expectedTime = now.plus(i * MINUTES_PER_QUARTER, ChronoUnit.MINUTES);
			Double value = timestampValueMap.get(expectedTime);

			if (value == null || Double.isNaN(value)) {
				predictedValues[i] = null;
			} else {
				predictedValues[i] = (int) Math.round(value < 5 ? 0 : value);
			}
		}

		return Prediction.from(this.sum, channelAddress, now, predictedValues);
	}

	private TrainingContext createTrainingContext() {
		return new TrainingContext(//
				this, //
				() -> this.componentManager.getClock(), //
				this.timedata, //
				this.weather, //
				this.productionChannelAddress, //
				this.predictorConfig.trainingWindowInQuarters(), //
				this.predictorConfig.regressorFitter(), //
				this.predictorConfig.minTrainingSamples(), //
				this.predictorConfig.maxTrainingSamples());
	}

	private PredictionContext createPredictionContext() {
		return new PredictionContext(//
				this.weather, //
				this.componentManager.getClock(), //
				this.predictorConfig.forecastQuarters(), //
				this.currentModel.regressor());
	}

	private boolean isModelTooOld(ModelBundle modelBundle) {
		if (modelBundle == null) {
			return true;
		}
		return modelBundle.createdAt().isBefore(//
				this.componentManager.getClock().instant().minus(this.predictorConfig.maxModelAge()));
	}

	private void logTrainingError(TrainingState state, String message) {
		this.logError(this.log, String.format(//
				"Training failed [%s]: %s", //
				state.getName(), //
				message));
	}

	private void logPredictionError(PredictionState state, String message) {
		this.logError(this.log, String.format(//
				"Prediction failed [%s]: %s", //
				state.getName(), //
				message));
	}

	@VisibleForTesting
	void computeAndSetMaxProduction() {
		var now = roundDownToQuarter(ZonedDateTime.now(this.componentManager.getClock()));
		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> channelData;

		try {
			channelData = this.timedata.queryHistoricData(//
					null, //
					now.minusDays(90), //
					now, //
					Set.of(this.productionChannelAddress), //
					new Resolution(MINUTES_PER_QUARTER, ChronoUnit.MINUTES));
		} catch (OpenemsNamedException e) {
			this.maxProduction = Integer.MAX_VALUE;
			return;
		}

		if (channelData == null || channelData.isEmpty()) {
			this.maxProduction = Integer.MAX_VALUE;
			return;
		}

		var productionValues = channelData.values().stream()//
				.map(m -> m.get(this.productionChannelAddress))//
				.filter(json -> json != null && !json.isJsonNull())//
				.mapToDouble(JsonElement::getAsDouble)//
				.toArray();

		var percentile = new Percentile();
		percentile.setData(productionValues);
		double q95 = percentile.evaluate(95);

		this.maxProduction = (int) Math.round(q95);
	}

	@Override
	protected ClockProvider getClockProvider() {
		return this.componentManager;
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.handleRequest(new GetPredictionEndpoint(), endpoint -> {
			endpoint.setGuards(EdgeGuards.roleIsAtleast(Role.OWNER));
		}, call -> {
			return new GetPredictionEndpoint.Response(//
					this.createNewPrediction(this.productionChannelAddress));
		});
	}

	@Component(service = PredictorConfig.class)
	public static class DefaultPredictorConfig implements PredictorConfig {

		@Override
		public int trainingIntervalInDays() {
			return 1;
		}

		@Override
		public int trainingWindowInQuarters() {
			return daysToQuarters(90);
		}

		@Override
		public int minTrainingSamples() {
			return daysToQuarters(30);
		}

		@Override
		public int maxTrainingSamples() {
			return daysToQuarters(60);
		}

		@Override
		public RegressorFitter regressorFitter() {
			return (features, target) -> RandomForestRegressor.fit(features, target, this.regressorConfig());
		}

		@Override
		public Duration maxModelAge() {
			return Duration.ofDays(7);
		}

		@Override
		public int forecastQuarters() {
			return daysToQuarters(2);
		}

		@Override
		public PredictionOrchestratorFactory predictionOrchestratorFactory() {
			return PredictionOrchestrator::new;
		}

		private RandomForestRegressor.Config regressorConfig() {
			return new RandomForestRegressor.Config(//
					100, // numTrees
					Integer.MAX_VALUE, // maxDepth
					3.0f, // minChildWeight
					0.0f, // minImpurityDecrease
					3.0f / 7, // fractionFeaturesInSplit
					false, // useRandomSplitPoints
					new MeanSquaredError(), // impurity
					42L // seed
			);
		}

		private static int daysToQuarters(int days) {
			return days * 24 /* hours */ * 4 /* quarters per hour */;
		}
	}

	@VisibleForTesting
	int getMaxProduction() {
		return this.maxProduction;
	}

	@VisibleForTesting
	void setProductionChannelAddress(ChannelAddress productionChannelAddress) {
		this.productionChannelAddress = productionChannelAddress;
	}

	@VisibleForTesting
	void setPredictionPersistenceService(PredictionPersistenceService predictionPersistenceService) {
		this.predictionPersistenceService = predictionPersistenceService;
	}

	@VisibleForTesting
	void setSnowStateMachine(SnowStateMachine snowStateMachine) {
		this.snowStateMachine = snowStateMachine;
	}
}
