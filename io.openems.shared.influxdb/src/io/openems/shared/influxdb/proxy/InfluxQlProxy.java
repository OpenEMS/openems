package io.openems.shared.influxdb.proxy;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
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
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.StringUtils;
import io.openems.shared.influxdb.InfluxConnector.InfluxConnection;

/**
 * Implements queries using InfluxQL, which is reported to be faster than Flux:
 * https://github.com/influxdata/influxdb/issues/18088.
 */
public class InfluxQlProxy extends QueryProxy {

	private static final Logger LOG = LoggerFactory.getLogger(InfluxQlProxy.class);

	@Override
	public SortedMap<ChannelAddress, JsonElement> queryHistoricEnergy(InfluxConnection influxConnection, String bucket,
			Optional<Integer> influxEdgeId, ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels)
			throws OpenemsNamedException {
		var query = this.buildHistoricEnergyQuery(bucket, influxEdgeId, fromDate, toDate, channels);
		var queryResult = this.executeQuery(influxConnection, bucket, query);
		return convertHistoricEnergyResult(queryResult, influxEdgeId, channels);
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricData(
			InfluxConnection influxConnection, String bucket, Optional<Integer> influxEdgeId, ZonedDateTime fromDate,
			ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution) throws OpenemsNamedException {
		var query = this.buildHistoricDataQuery(bucket, influxEdgeId, fromDate, toDate, channels, resolution);
		var queryResult = this.executeQuery(influxConnection, bucket, query);
		return convertHistoricDataQueryResult(queryResult, fromDate);
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricEnergyPerPeriod(
			InfluxConnection influxConnection, String bucket, Optional<Integer> influxEdgeId, ZonedDateTime fromDate,
			ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution) throws OpenemsNamedException {
		var query = this.buildHistoricEnergyPerPeriodQuery(bucket, influxEdgeId, fromDate, toDate, channels,
				resolution);
		var queryResult = this.executeQuery(influxConnection, bucket, query);
		return convertHistoricDataQueryResult(queryResult, fromDate);
	}

	@Override
	protected String buildHistoricDataQuery(String bucket, Optional<Integer> influxEdgeId, ZonedDateTime fromDate,
			ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution) throws OpenemsException {
		// Prepare query string
		var b = new StringBuilder("SELECT ") //
				.append(channels.stream() //
						.map(c -> "MEAN(\"" + c.toString() + "\") AS \"" + c.toString() + "\"") //
						.collect(Collectors.joining(", "))) //
				.append(" FROM data WHERE ");
		if (influxEdgeId.isPresent()) {
			b.append(OpenemsOEM.INFLUXDB_TAG + " = '" + influxEdgeId.get() + "' AND ");
		}
		b //
				.append("time > ") //
				.append(String.valueOf(fromDate.toEpochSecond())) //
				.append("s") //
				.append(" AND time < ") //
				.append(String.valueOf(toDate.toEpochSecond())) //
				.append("s") //
				.append(" GROUP BY time(") //
				.append(resolution.toSeconds()) //
				.append("s) fill(null)");

		// Execute query
		return b.toString();
	}

	@Override
	public String buildHistoricEnergyQuery(String bucket, Optional<Integer> influxEdgeId, ZonedDateTime fromDate,
			ZonedDateTime toDate, Set<ChannelAddress> channels) throws OpenemsException {
		// Prepare query string
		var b = new StringBuilder("SELECT ") //
				.append(channels.stream() //
						.map(c -> "LAST(\"" + c.toString() + "\") - FIRST(\"" + c.toString() + "\") AS \""
								+ c.toString() + "\"") //
						.collect(Collectors.joining(", "))) //
				.append(" FROM data WHERE ");
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
	public String buildHistoricEnergyPerPeriodQuery(String bucket, Optional<Integer> influxEdgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution)
			throws OpenemsException {
		// Prepare query string
		var b = new StringBuilder("SELECT ") //
				.append(channels.stream() //
						.map(c -> "NON_NEGATIVE_DIFFERENCE(LAST(\"" + c.toString() + "\")) AS \"" + c.toString() + "\"") //
						.collect(Collectors.joining(", "))) //
				.append(" FROM data WHERE ");
		if (influxEdgeId.isPresent()) {
			b.append(OpenemsOEM.INFLUXDB_TAG + " = '" + influxEdgeId.get() + "' AND ");
		}
		b //
				.append("time > ") //
				.append(String.valueOf(fromDate.toEpochSecond())) //
				.append("s") //
				.append(" AND time < ") //
				.append(String.valueOf(toDate.toEpochSecond())) //
				.append("s") //
				.append(" GROUP BY time(") //
				.append(resolution.toSeconds()) //
				.append("s) fill(null)");
		return b.toString();
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
	 * @param queryResult the Query-Result
	 * @param fromDate    the From-Date
	 * @return the historic data as Map
	 * @throws OpenemsException on error
	 */
	private static SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> convertHistoricDataQueryResult(
			InfluxQLQueryResult queryResult, ZonedDateTime fromDate) throws OpenemsNamedException {
		if (queryResult == null) {
			throw new OpenemsException("Historic data values are not available. QueryResult is null");
		}

		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> table = new TreeMap<>();
		for (var result : queryResult.getResults()) {
			var seriess = result.getSeries();
			if (seriess != null) {
				for (var series : seriess) {
					// add all data
					for (var record : series.getValues()) {
						SortedMap<ChannelAddress, JsonElement> tableRow = new TreeMap<>();
						// get timestamp
						var timestampInstant = Instant
								.ofEpochMilli(Long.parseLong((String) record.getValueByKey("time")));
						var timestamp = ZonedDateTime.ofInstant(timestampInstant, fromDate.getZone());
						if (timestamp.isBefore(fromDate)) {
							// InfluxQL sometimes gives too early timestamps -> ignore
							continue;
						}
						for (var column : series.getColumns().keySet()) {
							if (column.equals("time")) {
								continue;
							}
							// Note: ignoring index '0' here as it is the 'timestamp'
							var valueObj = record.getValueByKey(column);
							JsonElement value;
							if (valueObj == null) {
								value = JsonNull.INSTANCE;
							} else if (valueObj instanceof Number) {
								value = new JsonPrimitive((Number) valueObj);
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
									value = new JsonPrimitive(Integer.parseInt(str));
								} else {
									value = new JsonPrimitive(valueObj.toString());
								}
							}
							tableRow.put(ChannelAddress.fromString(column), value);
						}
						table.put(timestamp, tableRow);
					}
				}
			}
		}
		return table;
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
			throw new OpenemsException("Energy values are not available. QueryResult is null");
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
									value = assertPositive(Integer.parseInt(str), influxEdgeId, channels);
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

	private static JsonElement assertPositive(Number number, Optional<Integer> influxEdgeId,
			Set<ChannelAddress> channels) {
		if (number.intValue() < 0) {
			// do not consider negative values
			LOG.warn("Got negative Energy value [" + number + "] for [" + influxEdgeId.orElse(0) + "] "
					+ channels.stream().map(ChannelAddress::toString).collect(Collectors.joining(",")));
			return JsonNull.INSTANCE;
		} else {
			return new JsonPrimitive(number);
		}
	}
}