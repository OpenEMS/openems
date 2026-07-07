package io.openems.edge.predictor.production.linearmodel.services;

import static io.openems.common.utils.DateUtils.roundDownToQuarter;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.List;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.predictor.api.common.PredictionException;
import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;
import io.openems.edge.predictor.production.linearmodel.ColumnNames;
import io.openems.edge.predictor.production.linearmodel.prediction.PredictionError;
import io.openems.edge.weather.api.QuarterlyWeatherSnapshot;
import io.openems.edge.weather.api.Weather;

public class PredictionDataService {

	private final Weather weather;
	private final Clock clock;

	public PredictionDataService(//
			Weather weather, //
			Clock clock) {
		this.weather = weather;
		this.clock = clock;
	}

	/**
	 * Prepares a feature matrix for prediction based on the weather forecast.
	 *
	 * @param forecastQuarters the number of quarters to forecast
	 * @return a {@link DataFrame} containing the weather features
	 * @throws OpenemsException if no weather data available
	 */
	public DataFrame<ZonedDateTime> prepareFeatureMatrix(int forecastQuarters) throws PredictionException {
		try {
			var weatherData = this.weather.getQuarterlyWeatherForecast(forecastQuarters);

			if (weatherData == null || weatherData.isEmpty()) {
				throw new PredictionException(PredictionError.NO_WEATHER_DATA, "Weather data is null or empty");
			}

			this.validate(weatherData);

			return toDataFrame(weatherData);
		} catch (OpenemsException e) {
			throw new PredictionException(PredictionError.NO_WEATHER_DATA, e);
		}
	}

	private static DataFrame<ZonedDateTime> toDataFrame(List<QuarterlyWeatherSnapshot> weatherData) {
		var index = weatherData.stream()//
				.map(QuarterlyWeatherSnapshot::datetime)//
				.toList();
		var columnNames = List.of(//
				ColumnNames.SHORTWAVE_RADIATION, //
				ColumnNames.DIRECT_RADIATION, //
				ColumnNames.DIRECT_NORMAL_IRRADIANCE, //
				ColumnNames.DIFFUSE_RADIATION, //
				ColumnNames.TEMPERATURE);
		var values = weatherData.stream()//
				.map(s -> List.of(//
						s.shortwaveRadiation(), //
						s.directRadiation(), //
						s.directNormalIrradiance(), //
						s.diffuseRadiation(), //
						s.temperature()))//
				.toList();

		return new DataFrame<>(index, columnNames, values);
	}

	private void validate(List<QuarterlyWeatherSnapshot> weatherData) throws PredictionException {
		var expectedTimestamp = roundDownToQuarter(ZonedDateTime.now(this.clock));

		for (var snapshot : weatherData) {
			var actualTimestamp = snapshot.datetime();

			if (!actualTimestamp.equals(expectedTimestamp)) {
				throw new PredictionException(//
						PredictionError.INVALID_WEATHER_DATA, //
						"Weather data has gaps or missing intervals"//
				);
			}

			expectedTimestamp = expectedTimestamp.plusMinutes(15);
		}
	}
}
