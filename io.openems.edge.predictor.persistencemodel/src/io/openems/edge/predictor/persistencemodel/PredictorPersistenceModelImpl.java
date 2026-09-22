package io.openems.edge.predictor.persistencemodel;

import static io.openems.common.utils.DateUtils.QUARTERS_PER_DAY;
import static io.openems.edge.predictor.api.prediction.Prediction.EMPTY_PREDICTION;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.SortedMap;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.DateUtils;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.predictor.api.mlcore.datastructures.Series;
import io.openems.edge.predictor.api.mlcore.interpolation.Interpolator;
import io.openems.edge.predictor.api.mlcore.interpolation.LinearInterpolator;
import io.openems.edge.predictor.api.mlcore.smoothing.GaussianKernels;
import io.openems.edge.predictor.api.mlcore.smoothing.GaussianSmoother;
import io.openems.edge.predictor.api.mlcore.smoothing.Smoother;
import io.openems.edge.predictor.api.mlcore.transformer.InterpolationTransformer;
import io.openems.edge.predictor.api.prediction.AbstractPredictor;
import io.openems.edge.predictor.api.prediction.Prediction;
import io.openems.edge.predictor.api.prediction.Predictor;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Predictor.PersistenceModel", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE//
)
public class PredictorPersistenceModelImpl extends AbstractPredictor
		implements Predictor, PredictorPersistenceModel, OpenemsComponent {

	private static final int MINUTES_PER_QUARTER = 15;
	private static final int FORECAST_QUARTERS = QUARTERS_PER_DAY * 2;
	private static final int HISTORY_DAYS = 7;
	private static final int MAX_INTERPOLATION_GAP_QUARTERS = 4;
	private static final int TRANSITION_QUARTERS = 2;

	private final Logger log = LoggerFactory.getLogger(PredictorPersistenceModelImpl.class);

	private final InterpolationTransformer<Instant> interpolationTransformer;
	private final Smoother smoother;

	@Reference
	private Sum sum;

	@Reference
	private Timedata timedata;

	@Reference
	private ComponentManager componentManager;

	PredictorPersistenceModelImpl(//
			Interpolator interpolator, //
			Smoother smoother) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				PredictorPersistenceModel.ChannelId.values()//
		);
		this.interpolationTransformer = new InterpolationTransformer<>(interpolator);
		this.smoother = smoother;
	}

	PredictorPersistenceModelImpl(//
			Interpolator interpolator, //
			Smoother smoother, //
			Sum sum, //
			Timedata timedata, //
			ComponentManager componentManager) {
		this(interpolator, smoother);
		this.sum = sum;
		this.timedata = timedata;
		this.componentManager = componentManager;
	}

	public PredictorPersistenceModelImpl() {
		this(//
				new LinearInterpolator(MAX_INTERPOLATION_GAP_QUARTERS), //
				new GaussianSmoother(GaussianKernels.SIZE_9)//
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.logVerbosity(),
				config.channelAddresses());
	}

	@Override
	public Prediction createNewPrediction(ChannelAddress channelAddress) {
		final var now = DateUtils.roundDownToQuarter(ZonedDateTime.now(this.componentManager.getClock()));
		final var historyStart = now.minus(Duration.ofDays(HISTORY_DAYS));

		final SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryResult;
		try {
			queryResult = this.timedata.queryHistoricData(//
					null, //
					historyStart, //
					now, //
					Set.of(channelAddress), //
					new Resolution(MINUTES_PER_QUARTER, ChronoUnit.MINUTES));
		} catch (OpenemsNamedException e) {
			this.logWarn(this.log, "Historic data is not available: " + e.getMessage());
			return EMPTY_PREDICTION;
		}
		if (queryResult == null) {
			this.logWarn(this.log, "Historic data is not available: query result is null");
			return EMPTY_PREDICTION;
		}

		final var historicSeries = extractHistoricValues(queryResult, channelAddress);
		if (!hasExpectedResolution(historicSeries)) {
			this.logWarn(this.log, "Historic data has gaps in timestamps");
			return EMPTY_PREDICTION;
		}

		final var lastDaySeries = getValuesOfLastDay(historicSeries);
		final var interpolatedValues = this.interpolationTransformer.transform(lastDaySeries);
		this.fillRemainingGapsWithPeriodAverage(interpolatedValues, historicSeries);
		if (interpolatedValues.getValues().stream()//
				.anyMatch(v -> v == null || v.isNaN())) {
			this.logWarn(this.log, "Historic data is not available: unable to fill all gaps");
			return EMPTY_PREDICTION;
		}

		final var forecastValues = this.buildForecast(interpolatedValues);
		final Double latestMeasuredValue = extractValue(queryResult.get(queryResult.lastKey()), channelAddress);
		if (latestMeasuredValue != null) {
			this.applyTransitionSmoothing(forecastValues, latestMeasuredValue);
		}

		final var smoothedForecastValues = this.smoother.smooth(forecastValues);
		final var result = Arrays.stream(smoothedForecastValues)//
				.mapToObj(value -> (int) Math.round(value))//
				.toArray(Integer[]::new);
		return Prediction.from(this.sum, channelAddress, now.toInstant(), result);
	}

	private static Series<Instant> extractHistoricValues(//
			SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryResult, //
			ChannelAddress channelAddress) {
		final var index = new ArrayList<Instant>(queryResult.size());
		final var values = new ArrayList<Double>(queryResult.size());

		for (var entry : queryResult.entrySet()) {
			index.add(entry.getKey().toInstant());
			values.add(extractValue(entry.getValue(), channelAddress));
		}

		return new Series<>(index, values);
	}

	static boolean hasExpectedResolution(Series<Instant> values) {
		if (values.size() < QUARTERS_PER_DAY) {
			return false;
		}

		Instant previous = null;

		for (var timestamp : values.getIndex()) {
			if (previous != null && !previous.plus(Duration.ofMinutes(MINUTES_PER_QUARTER)).equals(timestamp)) {
				return false;
			}

			previous = timestamp;
		}

		return true;
	}

	static Series<Instant> getValuesOfLastDay(Series<Instant> historicSeries) {
		final var end = historicSeries.getIndex().getLast();
		final var start = end.minus(Duration.ofDays(1));

		final var index = new ArrayList<Instant>();
		final var values = new ArrayList<Double>();

		for (int i = 0; i < historicSeries.size(); i++) {
			final var timestamp = historicSeries.getIndex().get(i);
			if (!timestamp.isBefore(start)) {
				index.add(timestamp);
				values.add(historicSeries.getAt(i));
			}
		}

		return new Series<>(index, values);
	}

	void fillRemainingGapsWithPeriodAverage(//
			Series<Instant> values, //
			Series<Instant> historicValues) {
		for (int i = 0; i < values.size(); i++) {
			final var timestamp = values.getIndex().get(i);
			final Double value = values.getAt(i);

			if (value != null && !value.isNaN()) {
				continue;
			}

			double sum = 0.0;
			int count = 0;

			for (int days = 1; days <= HISTORY_DAYS; days++) {
				final var historicTimestamp = timestamp.minus(Duration.ofDays(days));
				final Double historicValue = historicValues.get(historicTimestamp);

				if (historicValue != null && !historicValue.isNaN()) {
					sum += historicValue;
					count++;
				}
			}

			if (count > 0) {
				values.setValueAt(i, sum / count);
			}
		}
	}

	double[] buildForecast(Series<Instant> lastDaySeries) {
		final var lastDayValues = lastDaySeries.getValues().stream()//
				.mapToDouble(Double::doubleValue)//
				.toArray();

		final var result = new double[FORECAST_QUARTERS];

		for (var i = 0; i < FORECAST_QUARTERS; i++) {
			result[i] = lastDayValues[i % lastDayValues.length];
		}

		return result;
	}

	void applyTransitionSmoothing(double[] forecastValues, double latestMeasuredValue) {
		final int limit = Math.min(TRANSITION_QUARTERS, forecastValues.length);
		for (int i = 0; i < limit; i++) {
			final double weight = (double) (i + 1) / (TRANSITION_QUARTERS + 1);
			forecastValues[i] = latestMeasuredValue * (1 - weight) + forecastValues[i] * weight;
		}
	}

	private static Double extractValue(//
			SortedMap<ChannelAddress, JsonElement> valuesByChannel, ChannelAddress channelAddress) {
		if (valuesByChannel == null) {
			return null;
		}

		final var value = valuesByChannel.get(channelAddress);
		if (value == null || value.isJsonNull()) {
			return null;
		}

		try {
			return value.getAsDouble();
		} catch (RuntimeException e) {
			return null;
		}
	}

	@Override
	protected ClockProvider getClockProvider() {
		return this.componentManager;
	}
}
