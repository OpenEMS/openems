package io.openems.edge.predictor.production.linearmodel.prediction;

import static io.openems.common.utils.DateUtils.roundDownToQuarter;

import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.predictor.api.common.PredictionException;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.weather.api.QuarterlyWeatherSnapshot;
import io.openems.edge.weather.api.Weather;

public class SnowStateMachine {

	private static final int MINUTES_PER_QUARTER = 15;

	private final Weather weather;
	private final Timedata timedata;
	private final Supplier<Clock> clockSupplier;
	private final ChannelAddress productionChannelAddress;
	private final Supplier<Integer> maxProductionSupplier;

	private State currentState;
	private ZonedDateTime snowStart;

	public SnowStateMachine(//
			Weather weather, //
			Timedata timedata, //
			Supplier<Clock> clockSupplier, //
			ChannelAddress productionChannelAddress, //
			Supplier<Integer> maxProductionSupplier, //
			State initialState) {
		this.weather = weather;
		this.timedata = timedata;
		this.clockSupplier = clockSupplier;
		this.productionChannelAddress = productionChannelAddress;
		this.maxProductionSupplier = maxProductionSupplier;

		this.currentState = initialState;
		this.snowStart = switch (initialState) {
		case NORMAL -> null;
		case SNOW -> roundDownToQuarter(ZonedDateTime.now(this.clockSupplier.get())).minusHours(24);
		};
	}

	/**
	 * Executes the state machine once, potentially updating the current state
	 * between NORMAL and SNOW based on the upcoming weather forecast.
	 *
	 * @throws PredictionException if weather data is unavailable
	 */
	public void run() throws PredictionException {
		List<QuarterlyWeatherSnapshot> forecast;
		try {
			forecast = this.weather.getQuarterlyWeatherForecast(daysToQuarters(2 /* days */));
		} catch (OpenemsException e) {
			throw new PredictionException(PredictionError.NO_WEATHER_DATA, e);
		}

		this.currentState = switch (this.currentState) {
		case NORMAL -> {
			if (isSnowFalling(forecast) && willSnowStay(forecast)) {
				this.snowStart = roundDownToQuarter(ZonedDateTime.now(this.clockSupplier.get()));
				yield State.SNOW;
			}
			yield State.NORMAL;
		}
		case SNOW -> {
			if (!isSnowOnGround(forecast) && !willSnowStay(forecast)) {
				this.snowStart = null;
				yield State.NORMAL;
			}

			if (this.isSnowAtLeast24h() && this.isStableHighProduction()) {
				this.snowStart = null;
				yield State.NORMAL;
			}

			yield State.SNOW;
		}
		};
	}

	public State getCurrentState() {
		return this.currentState;
	}

	public ZonedDateTime getSnowStart() {
		return this.snowStart;
	}

	private static boolean isSnowFalling(List<QuarterlyWeatherSnapshot> forecast) {
		return forecast.getFirst().snowfall() > 0;
	}

	private static boolean willSnowStay(List<QuarterlyWeatherSnapshot> forecast) {
		return forecast.stream()//
				.filter(s -> s.snowDepth() > 0)//
				.count() >= 0.9 * forecast.size();
	}

	private static boolean isSnowOnGround(List<QuarterlyWeatherSnapshot> forecast) {
		return forecast.getFirst().snowDepth() > 0;
	}

	private boolean isSnowAtLeast24h() {
		var now = roundDownToQuarter(ZonedDateTime.now(this.clockSupplier.get()));

		return Duration.between(this.snowStart, now).toHours() >= 24;
	}

	private boolean isStableHighProduction() {
		var latestProductionValues = this.getNLatestProductionValues(6);

		var stableProductionThreshold = 0.3 * this.maxProductionSupplier.get();
		return latestProductionValues.stream()//
				.filter(v -> v >= stableProductionThreshold)//
				.count() >= 4;
	}

	private List<Integer> getNLatestProductionValues(int n) {
		var now = roundDownToQuarter(ZonedDateTime.now(this.clockSupplier.get()));

		try {
			var channelData = this.timedata.queryHistoricData(//
					null, //
					now.minusMinutes(n * MINUTES_PER_QUARTER), //
					now, //
					Set.of(this.productionChannelAddress), //
					new Resolution(MINUTES_PER_QUARTER, ChronoUnit.MINUTES));

			if (channelData == null || channelData.isEmpty()) {
				return List.of();
			}

			return channelData.values().stream()//
					.map(m -> m.get(this.productionChannelAddress))//
					.filter(json -> json != null && !json.isJsonNull())//
					.map(JsonElement::getAsInt)//
					.toList();
		} catch (OpenemsNamedException e) {
			return List.of();
		}
	}

	private static int daysToQuarters(int days) {
		return days * 96;
	}

	public enum State implements OptionsEnum {

		NORMAL(0, "Normal"), //
		SNOW(1, "Snow"),//
		;

		private final int value;
		private final String name;

		private State(int value, String name) {
			this.value = value;
			this.name = name;
		}

		@Override
		public int getValue() {
			return this.value;
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public OptionsEnum getUndefined() {
			return NORMAL;
		}
	}

	@VisibleForTesting
	void setCurrentState(State state) {
		this.currentState = state;
	}

	@VisibleForTesting
	void setSnowStart(ZonedDateTime snowStart) {
		this.snowStart = snowStart;
	}
}
