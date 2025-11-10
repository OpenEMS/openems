package io.openems.edge.predictor.profileclusteringmodel;

import static io.openems.common.utils.ThreadPoolUtils.shutdownAndAwaitTermination;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.DateUtils;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.predictor.api.common.PredictionException;
import io.openems.edge.predictor.api.common.PredictionState;
import io.openems.edge.predictor.api.common.TrainingError;
import io.openems.edge.predictor.api.common.TrainingState;
import io.openems.edge.predictor.api.mlcore.classification.BernoulliNaiveBayesClassifier;
import io.openems.edge.predictor.api.mlcore.clustering.AutoKMeansClusterer;
import io.openems.edge.predictor.api.prediction.AbstractPredictor;
import io.openems.edge.predictor.api.prediction.Prediction;
import io.openems.edge.predictor.api.prediction.Predictor;
import io.openems.edge.predictor.profileclusteringmodel.prediction.PredictionContext;
import io.openems.edge.predictor.profileclusteringmodel.prediction.PredictionOrchestrator;
import io.openems.edge.predictor.profileclusteringmodel.prediction.ProfileSwitcher;
import io.openems.edge.predictor.profileclusteringmodel.services.QueryWindow;
import io.openems.edge.predictor.profileclusteringmodel.training.TrainingContext;
import io.openems.edge.predictor.profileclusteringmodel.training.TrainingRunnable;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Predictor.ProfileClusteringModel", //
		immediate = true, //
		configurationPolicy = REQUIRE //
)
public class PredictorProfileClusteringModelImpl extends AbstractPredictor
		implements PredictorProfileClusteringModel, TrainingCallback, Predictor, OpenemsComponent {

	private static final ChannelAddress SUM_UNMANAGED_CONSUMPTION_ACTIVE_POWER = new ChannelAddress(//
			"_sum", Sum.ChannelId.UNMANAGED_CONSUMPTION_ACTIVE_POWER.id());

	private static final int NO_INITIAL_DELAY = 0;
	private static final int MINUTES_PER_QUARTER = 15;

	private final Logger log = LoggerFactory.getLogger(PredictorProfileClusteringModelImpl.class);
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

	@Reference
	private ComponentManager componentManager;

	@Reference
	private Timedata timedata;

	@Reference
	private PredictorConfig predictorConfig;

	@Reference
	private Meta meta;

	private ModelBundle currentModels;
	private CurrentProfile currentProfile;
	private PredictionPersistenceService predictionPersistenceService;

	public PredictorProfileClusteringModelImpl() throws OpenemsNamedException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				PredictorProfileClusteringModel.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.logVerbosity(),
				SUM_UNMANAGED_CONSUMPTION_ACTIVE_POWER);

		if (!config.enabled()) {
			return;
		}

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

		this.predictionPersistenceService.startShiftingJob();
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		shutdownAndAwaitTermination(this.scheduler, 5);
		if (this.predictionPersistenceService != null) {
			this.predictionPersistenceService.deactivateShiftingJob();
		}
		this._setTrainingState(TrainingState.DEACTIVATED);
		this._setPredictionState(PredictionState.DEACTIVATED);
	}

	@Override
	protected Prediction createNewPrediction(ChannelAddress channelAddress) {
		if (this.currentModels == null) {
			this._setPredictionState(PredictionState.FAILED_NO_MODEL);
			this.logPredictionError(PredictionState.FAILED_NO_MODEL, "No trained model available");
			return Prediction.EMPTY_PREDICTION;
		}

		if (this.isModelTooOld()) {
			this._setPredictionState(PredictionState.FAILED_MODEL_OUTDATED);
			this.logPredictionError(PredictionState.FAILED_MODEL_OUTDATED, "Trained model outdated");
			return Prediction.EMPTY_PREDICTION;
		}

		var predictionContext = this.createPredictionContext();
		var predictionOchestrator = this.predictorConfig.predictionOrchestratorFactory().create(predictionContext);

		List<Profile> predictedProfiles;
		try {
			predictedProfiles = predictionOchestrator.predictProfiles(this.predictorConfig.forecastDays());
		} catch (PredictionException e) {
			this._setPredictionState(e.getError().getFailedState());
			this.logPredictionError(e.getError().getFailedState(), e.getMessage());
			return Prediction.EMPTY_PREDICTION;
		} catch (Exception e) {
			this._setPredictionState(PredictionState.FAILED_UNKNOWN);
			this.logPredictionError(PredictionState.FAILED_UNKNOWN, e.getMessage());
			return Prediction.EMPTY_PREDICTION;
		}

		this.currentProfile = new CurrentProfile(//
				LocalDate.now(this.componentManager.getClock()), //
				predictedProfiles.getFirst());

		var prediction = this.createNewPredictionFromProfiles(predictedProfiles);

		this.predictionPersistenceService.updatePredictionAheadChannels(prediction);

		this._setPredictionState(PredictionState.SUCCESSFUL);
		return prediction;
	}

	@Override
	public void onTrainingSuccess(ModelBundle bundle) {
		this.currentModels = bundle;
		this.currentProfile = null;
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

	private boolean isModelTooOld() {
		if (this.currentModels == null) {
			return true;
		}
		return this.currentModels.createdAt().isBefore(//
				this.componentManager.getClock().instant().minus(this.predictorConfig.maxModelAge()));
	}

	private Prediction createNewPredictionFromProfiles(List<Profile> profiles) {
		var clock = this.componentManager.getClock();
		var baseTime = ZonedDateTime.now(clock).truncatedTo(ChronoUnit.DAYS);
		var now = DateUtils.roundDownToQuarter(ZonedDateTime.now(clock));

		int quarterHourIndex = (int) ChronoUnit.MINUTES.between(baseTime, now) / MINUTES_PER_QUARTER;

		var values = IntStream.range(0, profiles.size())//
				.flatMap(i -> {
					var profileValues = profiles.get(i).values().getValues().stream();
					if (i == 0) {
						profileValues = profileValues.skip(quarterHourIndex);
					}
					return profileValues.mapToInt(v -> (int) Math.round(v));
				})//
				.boxed()//
				.toArray(Integer[]::new);

		return Prediction.from(now, values);
	}

	private TrainingContext createTrainingContext() {
		return new TrainingContext(//
				this, //
				() -> this.componentManager.getClock(), //
				this.timedata, //
				SUM_UNMANAGED_CONSUMPTION_ACTIVE_POWER, //
				new QueryWindow(//
						this.predictorConfig.minTrainingWindowDays(), //
						this.predictorConfig.maxTrainingWindowDays()), //
				this.predictorConfig.maxGapSizeInterpolationInQuarters(), //
				this.predictorConfig.minTrainingSamplesRequired(), //
				this.predictorConfig.clustererFitter(), //
				this.predictorConfig.classifierFitter(), //
				() -> this.meta.getSubdivisionCode());
	}

	private PredictionContext createPredictionContext() {
		return new PredictionContext(//
				() -> this.componentManager.getClock(), //
				this.timedata, //
				SUM_UNMANAGED_CONSUMPTION_ACTIVE_POWER, //
				this.predictorConfig.maxGapSizeInterpolationInQuarters(), //
				this.currentModels.clusterer(), //
				this.currentModels.classifier(), //
				this.currentModels.oneHotEncoder(), //
				() -> this.meta.getSubdivisionCode(), //
				this.predictorConfig.profileSwitcherFactory(), //
				this.currentProfile);
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

	@Override
	protected ClockProvider getClockProvider() {
		return this.componentManager;
	}

	@Component(service = PredictorConfig.class)
	public static class DefaultPredictorConfig implements PredictorConfig {

		@Override
		public int minTrainingWindowDays() {
			return 30;
		}

		@Override
		public int maxTrainingWindowDays() {
			return 90;
		}

		@Override
		public int maxGapSizeInterpolationInQuarters() {
			return 8;
		}

		@Override
		public int minTrainingSamplesRequired() {
			return 28;
		}

		@Override
		public ClustererFitter clustererFitter() {
			return (features) -> AutoKMeansClusterer.fit(features, 2, 8);
		}

		@Override
		public ClassifierFitter classifierFitter() {
			return BernoulliNaiveBayesClassifier::fit;
		}

		@Override
		public Duration maxModelAge() {
			return Duration.ofDays(7);
		}

		@Override
		public ProfileSwitcherFactory profileSwitcherFactory() {
			return ProfileSwitcher::new;
		}

		@Override
		public PredictionOrchestratorFactory predictionOrchestratorFactory() {
			return PredictionOrchestrator::new;
		}

		@Override
		public int trainingIntervalInDays() {
			return 1;
		}

		@Override
		public int forecastDays() {
			return 2;
		}
	}

	@VisibleForTesting
	void setPredictionPersistenceService(PredictionPersistenceService predictionPersistenceService) {
		this.predictionPersistenceService = predictionPersistenceService;
	}
}
