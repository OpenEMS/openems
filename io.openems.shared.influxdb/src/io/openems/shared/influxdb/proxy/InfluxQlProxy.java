package io.openems.shared.influxdb.proxy;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.influxdb.client.domain.InfluxQLQuery;
import com.influxdb.query.InfluxQLQueryResult;
import com.influxdb.query.InfluxQLQueryResult.Result;
import com.influxdb.query.InfluxQLQueryResult.Series;

import io.openems.common.OpenemsOEM;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.timedata.DurationUnit;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.CollectorUtils;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.StringUtils;
import io.openems.shared.influxdb.DbDataUtils;
import io.openems.shared.influxdb.InfluxConnector.InfluxConnection;

/**
 * Implements queries using InfluxQL, which is reported to be faster than Flux:
 * https://github.com/influxdata/influxdb/issues/18088.
 */
public class InfluxQlProxy extends QueryProxy {

	private static final Logger LOG = LoggerFactory.getLogger(InfluxQlProxy.class);

	@Override
	public SortedMap<ChannelAddress, JsonElement> queryHistoricEnergy(//
			InfluxConnection influxConnection, //
			String bucket, //
			String measurement, //
			Optional<Integer> influxEdgeId, //
			ZonedDateTime fromDate, //
			ZonedDateTime toDate, //
			Set<ChannelAddress> channels //
	) throws OpenemsNamedException {
		var query = this.buildHistoricEnergyQuery(bucket, measurement, influxEdgeId, fromDate, toDate, channels);
		var queryResult = this.executeQuery(influxConnection, bucket, query);
		return convertHistoricEnergyResult(queryResult, influxEdgeId, channels);
	}

	@Override
	public SortedMap<ChannelAddress, JsonElement> queryHistoricEnergySingleValueInDay(//
			InfluxConnection influxConnection, //
			String bucket, //
			String measurement, //
			Optional<Integer> influxEdgeId, //
			ZonedDateTime fromDate, //
			ZonedDateTime toDate, //
			Set<ChannelAddress> channels //
	) throws OpenemsNamedException {
		var query = this.buildHistoricEnergyQuerySingleValueInDay(bucket, measurement, influxEdgeId, fromDate, toDate,
				channels);
		var queryResult = this.executeQuery(influxConnection, bucket, query);
		var firstResult = convertHistoricEnergyResultSingleValueInDay(queryResult, influxEdgeId, channels);
		if (firstResult == null) {
			// return a map which has for every channel JsonNull.INSTANCE
			return channels.stream() //
					.collect(Collectors.toMap(Function.identity(), //
							t -> JsonNull.INSTANCE, //
							(t, u) -> u, TreeMap::new));
		}

		var channelsWithoutOldValues = firstResult.entrySet().stream() //
				.filter(t -> t.getValue().first().isJsonNull() && !t.getValue().second().isJsonNull()) //
				.map(Entry::getKey) //
				.collect(Collectors.toSet());

		final var beforeValues = this.queryFirstValueBefore(bucket, influxConnection, measurement, influxEdgeId,
				fromDate, channelsWithoutOldValues);

		return mergeEnergyValues(firstResult, beforeValues);
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricData(//
			InfluxConnection influxConnection, //
			String bucket, //
			String measurement, //
			Optional<Integer> influxEdgeId, //
			ZonedDateTime fromDate, //
			ZonedDateTime toDate, //
			Set<ChannelAddress> channels, //
			Resolution resolution //
	) throws OpenemsNamedException {
		var query = this.buildHistoricDataQuery(bucket, measurement, influxEdgeId, fromDate, toDate, channels,
				resolution);
		var queryResult = this.executeQuery(influxConnection, bucket, query);
		return convertHistoricDataQueryResult(queryResult, fromDate, resolution, channels, new Average());
	}

	// TODO maybe remove?
	private static class Average implements BiFunction<JsonElement, JsonElement, JsonElement> {

		private int count = 0;

		@Override
		public JsonElement apply(JsonElement first, JsonElement second) {
			if (!first.isJsonPrimitive() || !second.isJsonPrimitive() //
					|| !first.getAsJsonPrimitive().isNumber() || !second.getAsJsonPrimitive().isNumber()) {
				return second;
			}
			final var numberFirst = first.getAsNumber().longValue();
			final var numberSecond = first.getAsNumber().longValue();
			final var result = (numberFirst * this.count + numberSecond) / ++this.count;
			return new JsonPrimitive(result);
		}

	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricEnergyPerPeriod(//
			InfluxConnection influxConnection, //
			String bucket, //
			String measurement, //
			Optional<Integer> influxEdgeId, //
			ZonedDateTime fromDate, //
			ZonedDateTime toDate, //
			Set<ChannelAddress> channels, //
			Resolution resolution //
	) throws OpenemsNamedException {
		var query = this.buildHistoricEnergyPerPeriodQuery(bucket, measurement, influxEdgeId, fromDate, toDate,
				channels, resolution);
		var queryResult = this.executeQuery(influxConnection, bucket, query);
		var result = convertHistoricDataQueryResult(queryResult, fromDate, resolution, channels, InfluxQlProxy::last);
		return DbDataUtils.normalizeTable(result, channels, resolution, fromDate, toDate);
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryRawHistoricEnergyPerPeriodSingleValueInDay(//
			InfluxConnection influxConnection, //
			String bucket, //
			String measurement, //
			Optional<Integer> influxEdgeId, //
			ZonedDateTime fromDate, //
			ZonedDateTime toDate, //
			Set<ChannelAddress> channels, //
			Resolution resolution //
	) throws OpenemsNamedException {
		var query = this.buildHistoricEnergyPerPeriodQuerySingleValueInDay(bucket, measurement, influxEdgeId, fromDate,
				toDate, channels, resolution);
		var queryResult = this.executeQuery(influxConnection, bucket, query);

		final var result = convertHistoricDataQueryResultSingleValue(queryResult, fromDate, resolution, channels,
				InfluxQlProxy::last);

		if (result == null || result.isEmpty()) {
			return Collections.emptySortedMap();
		}

		final Set<ChannelAddress> channelsForBeforeValues;
		if (!result.firstKey().isBefore(fromDate)) {
			channelsForBeforeValues = channels;
		} else {
			final var first = result.get(result.firstKey());

			channelsForBeforeValues = first.entrySet().stream() //
					.filter(entry -> entry.getValue().isJsonNull()) //
					.map(Entry::getKey) //
					.collect(Collectors.toSet());
		}

		if (!channelsForBeforeValues.isEmpty()) {
			final var beforeValues = this.queryFirstValueBefore(bucket, influxConnection, measurement, influxEdgeId,
					fromDate, channelsForBeforeValues);

			if (result.firstKey().isBefore(fromDate)) {
				result.put(result.firstKey(), beforeValues);
			} else {
				result.put(fromDate.minusDays(1), beforeValues);
			}
		}

		return result;
	}

	private static JsonElement last(JsonElement first, JsonElement second) {
		return second;
	}

	@Override
	public Map<Integer, Map<String, Long>> queryAvailableSince(//
			InfluxConnection influxConnection, //
			String bucket //
	) throws OpenemsNamedException {
		final var query = this.buildFetchAvailableSinceQuery(bucket);
		final var queryResult = this.executeQuery(influxConnection, bucket, query);
		return convertAvailableSinceResult(queryResult);
	}

	@Override
	public SortedMap<ChannelAddress, JsonElement> queryFirstValueBefore(String bucket, //
			InfluxConnection influxConnection, //
			String measurement, //
			Optional<Integer> influxEdgeId, //
			ZonedDateTime date, //
			Set<ChannelAddress> channels //
	) throws OpenemsNamedException {
		if (channels.isEmpty()) {
			return Collections.emptySortedMap();
		}
		final var query = this.buildFetchFirstValueBefore(bucket, measurement, influxEdgeId, date, channels);
		final var queryResult = this.executeQuery(influxConnection, bucket, query);
		return convertHistoricEnergyResultRaw(queryResult, influxEdgeId, channels);
	}

	@Override
	protected String buildHistoricDataQuery(//
			String bucket, //
			String measurement, //
			Optional<Integer> influxEdgeId, //
			ZonedDateTime fromDate, //
			ZonedDateTime toDate, //
			Set<ChannelAddress> channels, //
			Resolution resolution //
	) throws OpenemsException {
		// Prepare query string
		var b = new StringBuilder("SELECT ") //
				.append(channels.stream() //
						.map(c -> "MEAN(\"" + c.toString() + "\") AS \"" + c.toString() + "\"") //
						.collect(Collectors.joining(", "))) //
				.append(" FROM ") //
				.append(measurement) //
				.append(" WHERE ");
		if (influxEdgeId.isPresent()) {
			b.append(OpenemsOEM.INFLUXDB_TAG + " = '" + influxEdgeId.get() + "' AND ");
		}
		b //
				.append("time >= ") //
				.append(String.valueOf(fromDate.toEpochSecond())) //
				.append("s") //
				.append(" AND time < ") //
				.append(String.valueOf(toDate.toEpochSecond())) //
				.append("s") //
				.append(" GROUP BY time(") //
				.append(resolution.toSeconds()) //
				.append("s)");
		return b.toString();
	}

	@Override
	public String buildHistoricEnergyQuery(//
			String bucket, //
			String measurement, //
			Optional<Integer> influxEdgeId, //
			ZonedDateTime fromDate, //
			ZonedDateTime toDate, //
			Set<ChannelAddress> channels //
	) throws OpenemsException {
		// Prepare query string
		var b = new StringBuilder("SELECT ") //
				.append(channels.stream() //
						.map(c -> "LAST(\"" + c.toString() + "\") - FIRST(\"" + c.toString() + "\") AS \""
								+ c.toString() + "\"") //
						.collect(Collectors.joining(", "))) //
				.append(" FROM ") //
				.append(measurement) //
				.append(" WHERE ");
		if (influxEdgeId.isPresent()) {
			b.append(OpenemsOEM.INFLUXDB_TAG + " = '" + influxEdgeId.get() + "' AND ");
		}
		b //
				.append("time > ") //
				.append(String.valueOf(fromDate.toEpochSecond())) //
				.append("s") //
				.append(" AND time < ") //
				.append(String.valueOf(toDate.toEpochSecond())) //
				.append("s");
		return b.toString();
	}

	@Override
	protected String buildHistoricEnergyQuerySingleValueInDay(//
			String bucket, //
			String measurement, //
			Optional<Integer> influxEdgeId, //
			ZonedDateTime fromDate, //
			ZonedDateTime toDate, //
			Set<ChannelAddress> channels //
	) throws OpenemsException {
		fromDate = fromDate.minusDays(1);
		// Prepare query string
		var b = new StringBuilder("SELECT ") //
				.append(channels.stream() //
						.map(c -> "LAST(\"" + c.toString() + "\") AS \"LAST(" + c.toString() + ")\"" + ", FIRST(\"" //
								+ c.toString() + "\") AS \"FIRST(" + c.toString() + ")\"") //
						.collect(Collectors.joining(", "))) //
				.append(" FROM ") //
				.append(measurement) //
				.append(" WHERE ");
		if (influxEdgeId.isPresent()) {
			b.append(OpenemsOEM.INFLUXDB_TAG + " = '" + influxEdgeId.get() + "' AND ");
		}
		b //
				.append("time >= ") //
				.append(String.valueOf(fromDate.toEpochSecond())) //
				.append("s") //
				.append(" AND time < ") //
				.append(String.valueOf(toDate.toEpochSecond())) //
				.append("s");
		return b.toString();
	}

	@Override
	public String buildHistoricEnergyPerPeriodQuery(//
			String bucket, //
			String measurement, //
			Optional<Integer> influxEdgeId, //
			ZonedDateTime fromDate, //
			ZonedDateTime toDate, //
			Set<ChannelAddress> channels, //
			Resolution resolution //
	) throws OpenemsException {
		// Prepare query string
		var b = new StringBuilder("SELECT ") //
				.append(channels.stream() //
						.map(c -> "NON_NEGATIVE_DIFFERENCE(LAST(\"" + c.toString() + "\")) AS \"" + c.toString() + "\"") //
						.collect(Collectors.joining(", "))) //
				.append(" FROM ") //
				.append(measurement) //
				.append(" WHERE ");
		if (influxEdgeId.isPresent()) {
			b.append(OpenemsOEM.INFLUXDB_TAG + " = '" + influxEdgeId.get() + "' AND ");
		}

		final long res;
		if (resolution.getUnit().isDurationEstimated()) {
			res = new Resolution(1, ChronoUnit.DAYS).toSeconds();
		} else {
			res = resolution.toSeconds();
		}
		b //
				.append("time >= ") //
				.append(String.valueOf(fromDate.toEpochSecond())) //
				.append("s") //
				.append(" AND time < ") //
				.append(String.valueOf(toDate.toEpochSecond())) //
				.append("s GROUP BY time(") //
				.append(res) //
				.append("s)");
		return b.toString();
	}

	@Override
	protected String buildHistoricEnergyPerPeriodQuerySingleValueInDay(//
			String bucket, //
			String measurement, //
			Optional<Integer> influxEdgeId, //
			ZonedDateTime fromDate, //
			ZonedDateTime toDate, //
			Set<ChannelAddress> channels, //
			Resolution resolution //
	) throws OpenemsException {
		fromDate = fromDate.minusDays(1);
		// Prepare query string
		var b = new StringBuilder("SELECT ") //
				.append(channels.stream() //
						.map(c -> "LAST(\"" + c.toString() + "\") AS \"" + c.toString() + "\"") //
						.collect(Collectors.joining(", "))) //
				.append(" FROM ") //
				.append(measurement) //
				.append(" WHERE ");
		if (influxEdgeId.isPresent()) {
			b.append(OpenemsOEM.INFLUXDB_TAG + " = '" + influxEdgeId.get() + "' AND ");
		}

		final long res;
		if (resolution.getUnit().isDurationEstimated()) {
			// group by hours to still get the right time stamp if we would group by days it
			// would use UTC and the result could be shifted by one day
			res = new Resolution(1, ChronoUnit.HOURS).toSeconds();
		} else {
			res = resolution.toSeconds();
		}
		b.append("time >= ") //
				.append(fromDate.toEpochSecond()) //
				.append("s") //
				.append(" AND time < ") //
				.append(toDate.toEpochSecond()) //
				.append("s GROUP BY time(") //
				.append(res) //
				.append("s) fill(none)"); //
		return b.toString();
	}

	@Override
	protected String buildFetchAvailableSinceQuery(//
			String bucket //
	) {
		return new StringBuilder("SELECT ") //
				.append(OpenemsOEM.INFLUXDB_TAG) //
				.append(", ") //
				.append(QueryProxy.CHANNEL_TAG) //
				.append(", ") //
				.append(QueryProxy.AVAILABLE_SINCE_COLUMN_NAME) //
				.append(" FROM ") //
				.append(QueryProxy.AVAILABLE_SINCE_MEASUREMENT) //
				.toString();
	}

	@Override
	protected String buildFetchFirstValueBefore(//
			String bucket, //
			String measurement, //
			Optional<Integer> influxEdgeId, //
			ZonedDateTime date, //
			Set<ChannelAddress> channels //
	) {
		final var builder = new StringBuilder("SELECT ") //
				.append(channels.stream().map(channel -> "LAST(\"" + channel + "\") as \"" + channel + "\"")
						.collect(Collectors.joining(", ")))
				.append(" FROM ") //
				.append(measurement) //
				.append(" WHERE time < ") //
				.append(date.toEpochSecond()) //
				.append("s");

		influxEdgeId.ifPresent(id -> {
			builder.append(" AND ") //
					.append(OpenemsOEM.INFLUXDB_TAG) //
					.append(" = '") //
					.append(id) //
					.append("'");
		});

		return builder.toString();
	}

	private InfluxQLQueryResult executeQuery(InfluxConnection influxConnection, String bucket, String query)
			throws OpenemsException {
		this.assertQueryLimit();

		var database = bucket.split("/")[0];

		// Parse result
		InfluxQLQueryResult queryResult;
		try {
			queryResult = influxConnection.client.getInfluxQLQueryApi().query(new InfluxQLQuery(query, database) //
					.setPrecision(InfluxQLQuery.InfluxQLPrecision.MILLISECONDS));
		} catch (RuntimeException e) {
			this.queryLimit.increase();
			LOG.error("InfluxDB query runtime error. Query: " + query + ", Error: " + e.getMessage());
			throw new OpenemsException(e.getMessage());
		}
		this.queryLimit.decrease();
		return queryResult;
	}

	/**
	 * Converts the QueryResult of a Historic-Data query to a properly typed Table.
	 *
	 * @param queryResult       the Query-Result
	 * @param fromDate          the From-Date
	 * @param resolution        the {@link Resolution}
	 * @param channels          the channels
	 * @param aggregateFunction the aggregation function
	 * @return the historic data as Map
	 * @throws OpenemsException on error
	 */
	private static SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> convertHistoricDataQueryResult(//
			InfluxQLQueryResult queryResult, //
			ZonedDateTime fromDate, //
			Resolution resolution, //
			Set<ChannelAddress> channels, //
			BiFunction<JsonElement, JsonElement, JsonElement> aggregateFunction //
	) throws OpenemsNamedException {
		if (queryResult == null) {
			return null;
		}

		final SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> table = new TreeMap<>();
		for (var result : queryResult.getResults()) {
			var seriess = result.getSeries();
			if (seriess != null) {
				for (var series : seriess) {
					// add all data
					for (var record : series.getValues()) {

						// get timestamp
						var timestampInstant = Instant
								.ofEpochMilli(Long.parseLong((String) record.getValueByKey("time")));
						var timestamp = ZonedDateTime.ofInstant(timestampInstant, fromDate.getZone());
						if (timestamp.isBefore(fromDate)) {
							// InfluxQL sometimes gives too early timestamps -> ignore
							continue;
						}

						SortedMap<ChannelAddress, JsonElement> existingData = null;
						if (resolution.getUnit() == ChronoUnit.MONTHS) {
							for (var entry : table.entrySet()) {
								final var date = entry.getKey();
								if (date.getMonth() == timestamp.getMonth() //
										&& date.getYear() == timestamp.getYear()) {
									existingData = entry.getValue();
									break;
								}
							}
						}
						SortedMap<ChannelAddress, JsonElement> tableRow;
						if (existingData != null) {
							tableRow = existingData;
						} else {
							tableRow = new TreeMap<>();
							table.put(timestamp, tableRow);
						}

						for (var column : series.getColumns().keySet()) {
							if (column.equals("time")) {
								continue;
							}
							// Note: ignoring index '0' here as it is the 'timestamp'
							final var channel = ChannelAddress.fromString(column);
							var value = convertToJsonElement(record.getValueByKey(column));
							final var existingValue = tableRow.get(channel);
							if (existingValue != null) {
								value = aggregateFunction.apply(existingValue, value);
							}

							tableRow.put(ChannelAddress.fromString(column), value);
						}
					}
				}
			}
		}
		return table;
	}

	private static SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> convertHistoricDataQueryResultSingleValue(//
			InfluxQLQueryResult queryResult, //
			ZonedDateTime fromDate, //
			Resolution resolution, //
			Set<ChannelAddress> channels, //
			BiFunction<JsonElement, JsonElement, JsonElement> aggregateFunction //
	) throws OpenemsNamedException {
		if (queryResult == null) {
			return null;
		}

		return queryResult.getResults().stream() //
				.flatMap(r -> r.getSeries().stream()) //
				.flatMap(s -> s.getValues().stream()) //
				.<Pair<ChannelAddress, com.influxdb.query.InfluxQLQueryResult.Series.Record>>mapMulti((r,
						f) -> channels.forEach(c -> f.accept(
								new Pair<ChannelAddress, com.influxdb.query.InfluxQLQueryResult.Series.Record>(c, r))))
				.collect(Collectors.groupingBy(t -> {
					var timestampInstant = Instant
							.ofEpochMilli(Long.parseLong((String) t.second().getValueByKey("time")));
					var zonedDateTime = ZonedDateTime.ofInstant(timestampInstant, fromDate.getZone());
					if (resolution.getUnit() == ChronoUnit.MONTHS) {
						zonedDateTime = zonedDateTime.withDayOfMonth(1);
					}
					return zonedDateTime.truncatedTo(DurationUnit.ofDays(1));
				}, TreeMap::new, Collectors.toMap(Pair::first, r -> {
					final var channel = r.first();
					final var value = convertToJsonElement(r.second().getValueByKey(channel.toString()));

					if (!JsonUtils.isNumber(value)) {
						return JsonNull.INSTANCE;
					}

					return value;
				}, (t, u) -> aggregateFunction.apply(t, u), TreeMap::new)));
	}

	private static JsonElement convertToJsonElement(Object valueObj) {
		if (valueObj == null) {
			return JsonNull.INSTANCE;
		}
		if (valueObj instanceof Number) {
			return new JsonPrimitive((Number) valueObj);
		}

		final String str;
		if (valueObj instanceof String) {
			str = (String) valueObj;
		} else {
			str = valueObj.toString();
		}
		if (str.isEmpty()) {
			return JsonNull.INSTANCE;
		}
		if (StringUtils.matchesFloatPattern(str)) {
			return new JsonPrimitive(Double.parseDouble(str));
		}
		if (StringUtils.matchesIntegerPattern(str)) {
			return new JsonPrimitive(Long.parseLong(str));
		}

		return new JsonPrimitive(valueObj.toString());
	}

	private static SortedMap<ChannelAddress, Pair<JsonElement, JsonElement>> convertHistoricEnergyResultSingleValueInDay(//
			InfluxQLQueryResult queryResult, //
			Optional<Integer> influxEdgeId, //
			Set<ChannelAddress> channels //
	) throws OpenemsNamedException {
		if (queryResult == null) {
			return null;
		}

		return queryResult.getResults().stream() //
				.flatMap(t -> t.getSeries().stream()) //
				.flatMap(t -> t.getValues().stream()) //
				.<Pair<ChannelAddress, com.influxdb.query.InfluxQLQueryResult.Series.Record>>mapMulti((r,
						f) -> channels.forEach(c -> f.accept(
								new Pair<ChannelAddress, com.influxdb.query.InfluxQLQueryResult.Series.Record>(c, r))))
				.collect(Collectors.toMap(Pair::first, t -> {
					final var channel = t.first();
					final var record = t.second();
					var first = record.getValueByKey("FIRST(" + channel.toString() + ")");
					final var last = record.getValueByKey("LAST(" + channel.toString() + ")");
					if (Objects.equals(first, last)) {
						first = null;
					}
					return new Pair<JsonElement, JsonElement>(convertToJsonElement(first), convertToJsonElement(last));
				}, (t, u) -> u, TreeMap::new));
	}

	// TODO move to common
	public static record Pair<A, B>(A first, B second) {

	}

	private static SortedMap<ChannelAddress, JsonElement> convertHistoricEnergyResultRaw(
			InfluxQLQueryResult queryResult, Optional<Integer> influxEdgeId, Set<ChannelAddress> channels)
			throws OpenemsNamedException {
		if (queryResult == null) {
			return null;
		}

		return queryResult.getResults().stream() //
				.flatMap(r -> r.getSeries().stream()) //
				.<Pair<ChannelAddress, JsonElement>>mapMulti((s, m) -> {
					for (var record : s.getValues()) {
						for (var column : s.getColumns().keySet()) {
							if (column.equals("time")) {
								continue;
							}
							var valueObj = record.getValueByKey(column);
							JsonElement value;
							if (valueObj == null) {
								value = JsonNull.INSTANCE;
							} else if (valueObj instanceof Number n) {
								value = new JsonPrimitive(n);
							} else {
								final String str;
								if (valueObj instanceof String) {
									str = (String) valueObj;
								} else {
									str = valueObj.toString();
								}
								if (str.isEmpty()) {
									value = JsonNull.INSTANCE;
								} else if (StringUtils.matchesFloatPattern(str)) {
									value = new JsonPrimitive(Double.parseDouble(str));
								} else if (StringUtils.matchesIntegerPattern(str)) {
									value = new JsonPrimitive(Long.parseLong(str));
								} else {
									value = new JsonPrimitive(valueObj.toString());
								}
							}
							try {
								m.accept(new Pair<>(ChannelAddress.fromString(column), value));
							} catch (OpenemsNamedException e) {
								e.printStackTrace();
							}
						}
					}
				}) //
				.filter(Objects::nonNull) //
				.collect(Collectors.toMap(Pair::first, Pair::second, (t, u) -> u, TreeMap::new));
	}

	/**
	 * Converts the QueryResult of a Historic-Energy query to a properly typed Map.
	 *
	 * @param queryResult  the Query-Result
	 * @param influxEdgeId the Edge-ID
	 * @param channels     the {@link ChannelAddress}es
	 * @return the historic energy as Map
	 * @throws OpenemsException on error
	 */
	private static SortedMap<ChannelAddress, JsonElement> convertHistoricEnergyResult(InfluxQLQueryResult queryResult,
			Optional<Integer> influxEdgeId, Set<ChannelAddress> channels) throws OpenemsNamedException {
		if (queryResult == null) {
			return null;
		}

		SortedMap<ChannelAddress, JsonElement> map = new TreeMap<>();
		for (Result result : queryResult.getResults()) {
			var seriess = result.getSeries();
			if (seriess != null) {
				for (Series series : seriess) {
					// add all data
					for (var record : series.getValues()) {
						for (var column : series.getColumns().keySet()) {
							if (column.equals("time")) {
								continue;
							}
							var valueObj = record.getValueByKey(column);
							JsonElement value;
							if (valueObj == null) {
								value = JsonNull.INSTANCE;
							} else if (valueObj instanceof Number) {
								value = assertPositive((Number) valueObj, influxEdgeId, channels);
							} else {
								final String str;
								if (valueObj instanceof String) {
									str = (String) valueObj;
								} else {
									str = valueObj.toString();
								}
								if (str.isEmpty()) {
									value = JsonNull.INSTANCE;
								} else if (StringUtils.matchesFloatPattern(str)) {
									value = assertPositive(Double.parseDouble(str), influxEdgeId, channels);
								} else if (StringUtils.matchesIntegerPattern(str)) {
									value = assertPositive(Long.parseLong(str), influxEdgeId, channels);
								} else {
									value = new JsonPrimitive(valueObj.toString());
								}
							}
							map.put(ChannelAddress.fromString(column), value);
						}
					}
				}
			}
		}

		{
			// Check if all values are null
			var areAllValuesNull = true;
			for (JsonElement value : map.values()) {
				if (!value.isJsonNull()) {
					areAllValuesNull = false;
					break;
				}
			}
			if (areAllValuesNull) {
				throw new OpenemsException("Energy values are not available");
			}
		}

		return map;
	}

	private static Map<Integer, Map<String, Long>> convertAvailableSinceResult(InfluxQLQueryResult queryResult)
			throws OpenemsNamedException {
		if (queryResult == null || queryResult.getResults() == null || queryResult.getResults().isEmpty()) {
			return new TreeMap<>();
		}
		return queryResult.getResults().stream() //
				.flatMap(result -> result.getSeries().stream()) //
				.flatMap(series -> series.getValues().stream()) //
				.collect(CollectorUtils.toDoubleMap(//
						record -> Integer.parseInt(//
								(String) record.getValueByKey(OpenemsOEM.INFLUXDB_TAG) //
						), //
						record -> (String) record.getValueByKey(QueryProxy.CHANNEL_TAG), //
						record -> Long.parseLong(//
								(String) record.getValueByKey(QueryProxy.AVAILABLE_SINCE_COLUMN_NAME) //
						)) //
				);
	}

	private static SortedMap<ChannelAddress, JsonElement> mergeEnergyValues(//
			SortedMap<ChannelAddress, Pair<JsonElement, JsonElement>> firstResult, //
			SortedMap<ChannelAddress, JsonElement> beforeValues //
	) {
		return firstResult.entrySet().stream() //
				.collect(Collectors.toMap(Entry::getKey, t -> {
					final var channel = t.getKey();
					final var pair = t.getValue();

					if (pair.second().isJsonNull()) {
						return JsonNull.INSTANCE;
					}
					var first = t.getValue().first();
					var last = t.getValue().second();
					if (first.isJsonNull() && beforeValues != null) {
						first = beforeValues.get(channel);
					}
					if (first == null || first.isJsonNull()) {
						return last;
					}
					if (!JsonUtils.isNumber(last)) {
						return last;
					}
					if (!JsonUtils.isNumber(first)) {
						return last;
					}
					return new JsonPrimitive(last.getAsDouble() - first.getAsDouble());
				}, (t, u) -> u, TreeMap::new));
	}

	private static JsonElement assertPositive(Number number, Optional<Integer> influxEdgeId,
			Set<ChannelAddress> channels) {
		if (number.longValue() < 0) {
			// do not consider negative values
			LOG.warn("Got negative Energy value [" + number + "] for [" + influxEdgeId.orElse(0) + "] "
					+ channels.stream().map(ChannelAddress::toString).collect(Collectors.joining(",")));
			return JsonNull.INSTANCE;
		} else {
			return new JsonPrimitive(number);
		}
	}
}