package io.openems.edge.predictor.production.linearmodel.services;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.predictor.api.common.TrainingException;
import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;
import io.openems.edge.predictor.api.mlcore.datastructures.Series;
import io.openems.edge.predictor.api.mlcore.transformer.ColumnApplyTransformer;
import io.openems.edge.predictor.api.mlcore.transformer.DataFrameTransformerPipeline;
import io.openems.edge.predictor.api.mlcore.transformer.DropColumnsTransformer;
import io.openems.edge.predictor.api.mlcore.transformer.DropNaTransformer;
import io.openems.edge.predictor.production.linearmodel.ColumnNames;
import io.openems.edge.predictor.production.linearmodel.training.TrainingError;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.weather.api.QuarterlyWeatherSnapshot;
import io.openems.edge.weather.api.Weather;

public class TrainingDataService {

	private static final int MINUTES_PER_QUARTER = 15;

	private final Weather weather;
	private final Timedata timedata;
	private final ChannelAddress productionChannelAddress;

	public TrainingDataService(//
			Weather weather, //
			Timedata timedata, //
			ChannelAddress productionChannelAddress) {
		this.weather = weather;
		this.timedata = timedata;
		this.productionChannelAddress = productionChannelAddress;
	}

	/**
	 * Prepares a feature-target matrix for training within the specified time
	 * range.
	 *
	 * @param from the start timestamp (inclusive) of the training data
	 * @param to   the end timestamp (inclusive) of the training data
	 * @return a {@link DataFrame} containing features and target values for
	 *         training
	 * @throws ExecutionException    if fetching weather data fails
	 * @throws OpenemsNamedException if data retrieval fails
	 */
	public DataFrame<ZonedDateTime> prepareFeatureTargetMatrix(ZonedDateTime from, ZonedDateTime to)
			throws TrainingException {
		var weatherData = this.fetchHistoricalWeatherData(from, to);
		var featureMatrix = toDataFrame(weatherData);

		var productionData = this.fetchHistoricalProductionData(from, to);
		var targetMatrix = toDataFrame(productionData, this.productionChannelAddress);

		var featureTargetMatrix = featureMatrix.innerJoin(targetMatrix);

		// Clean negative production values and remove samples likely affected by snow
		// (snow depth >= 2cm)
		var pipeline = new DataFrameTransformerPipeline<ZonedDateTime>(List.of(//
				new ColumnApplyTransformer<>(v -> v < 0 ? Double.NaN : v, List.of(ColumnNames.TARGET)), //
				new ColumnApplyTransformer<>(v -> v >= 0.02 ? Double.NaN : v, List.of(ColumnNames.SNOW_DEPTH)), //
				new DropNaTransformer<>(), //
				new DropColumnsTransformer<>(List.of(ColumnNames.SNOW_DEPTH))));

		return pipeline.transform(featureTargetMatrix);
	}

	private List<QuarterlyWeatherSnapshot> fetchHistoricalWeatherData(//
			ZonedDateTime from, //
			ZonedDateTime to) throws TrainingException {
		try {
			var weatherData = this.weather.getHistoricalWeather(//
					from.toLocalDate(), //
					to.toLocalDate(), //
					from.getZone()//
			).join();

			if (weatherData == null || weatherData.isEmpty()) {
				throw new TrainingException(TrainingError.NO_WEATHER_DATA, "Weather data is null or empty");
			}

			return weatherData;
		} catch (CompletionException e) {
			throw new TrainingException(TrainingError.NO_WEATHER_DATA, e);
		}
	}

	private SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> fetchHistoricalProductionData(//
			ZonedDateTime from, //
			ZonedDateTime to) throws TrainingException {
		try {
			var productionData = this.timedata.queryHistoricData(//
					null, //
					from, //
					to, //
					Sets.newHashSet(this.productionChannelAddress), //
					new Resolution(MINUTES_PER_QUARTER, ChronoUnit.MINUTES));

			if (productionData == null || productionData.isEmpty()) {
				throw new TrainingException(TrainingError.NO_PRODUCTION_DATA, "Production data is null or empty");
			}

			return productionData;
		} catch (OpenemsNamedException e) {
			throw new TrainingException(TrainingError.NO_PRODUCTION_DATA, e);
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
				ColumnNames.TEMPERATURE, //
				ColumnNames.SNOW_DEPTH);
		var values = weatherData.stream()//
				.map(s -> List.of(//
						s.shortwaveRadiation(), //
						s.directRadiation(), //
						s.directNormalIrradiance(), //
						s.diffuseRadiation(), //
						s.temperature(), //
						s.snowDepth()))//
				.toList();

		return new DataFrame<>(index, columnNames, values);
	}

	private static DataFrame<ZonedDateTime> toDataFrame(
			SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> productionData,
			ChannelAddress channelAddress) {
		var index = new ArrayList<>(productionData.keySet());
		var values = index.stream()//
				.map(idx -> {
					var elem = productionData.get(idx).get(channelAddress);
					return JsonUtils.getAsOptionalDouble(elem).orElse(Double.NaN);
				})//
				.toList();

		var series = new Series<>(index, values);
		return DataFrame.fromSeriesMap(Map.of(ColumnNames.TARGET, series));
	}
}
