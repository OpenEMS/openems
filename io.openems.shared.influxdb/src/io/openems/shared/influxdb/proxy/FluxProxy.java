package io.openems.shared.influxdb.proxy;

import static io.openems.shared.influxdb.InfluxConnector.MEASUREMENT;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.influxdb.query.dsl.Flux;
import com.influxdb.query.dsl.functions.restriction.Restrictions;

import io.openems.common.OpenemsOEM;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.shared.influxdb.InfluxConnector.InfluxConnection;

/**
 * Implements queries using Flux.
 */
public class FluxProxy extends QueryProxy {

	private static final Logger LOG = LoggerFactory.getLogger(FluxProxy.class);

	@Override
	public SortedMap<ChannelAddress, JsonElement> queryHistoricEnergy(InfluxConnection influxConnection, String bucket,
			Optional<Integer> influxEdgeId, ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels)
			throws OpenemsNamedException {
		var query = this.buildHistoricEnergyQuery(bucket, influxEdgeId, fromDate, toDate, channels);
		var queryResult = this.executeQuery(influxConnection, query);
		return convertHistoricEnergyResult(query, queryResult);
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricData(
			InfluxConnection influxConnection, String bucket, Optional<Integer> influxEdgeId, ZonedDateTime fromDate,
			ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution) throws OpenemsNamedException {
		var query = this.buildHistoricDataQuery(bucket, influxEdgeId, fromDate, toDate, channels, resolution);
		var queryResult = this.executeQuery(influxConnection, query);
		return convertHistoricDataQueryResult(queryResult, fromDate, resolution);
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricEnergyPerPeriod(
			InfluxConnection influxConnection, String bucket, Optional<Integer> influxEdgeId, ZonedDateTime fromDate,
			ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution) throws OpenemsNamedException {
		var query = this.buildHistoricEnergyPerPeriodQuery(bucket, influxEdgeId, fromDate, toDate, channels,
				resolution);
		var queryResult = this.executeQuery(influxConnection, query);
		return convertHistoricDataQueryResult(queryResult, fromDate, resolution);
	}

	@Override
	protected String buildHistoricDataQuery(String bucket, Optional<Integer> influxEdgeId, ZonedDateTime fromDate,
			ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution) {
		// remove 5 minutes to prevent shifted timeline
		var fromInstant = fromDate.toInstant().minus(5, ChronoUnit.MINUTES);

		// prepare query
		Flux flux = Flux.from(bucket) //
				.range(fromInstant, toDate.toInstant()) //
				// TODO: TO_DATE is wrong, as it is inclusive
				.filter(Restrictions.measurement().equal(MEASUREMENT));

		if (influxEdgeId.isPresent()) {
			flux = flux.filter(Restrictions.tag(OpenemsOEM.INFLUXDB_TAG).equal(influxEdgeId.get().toString()));
		}

		flux = flux.filter(toChannelAddressFieldList(channels)) //
				.aggregateWindow(resolution.getValue(), resolution.getUnit(), "mean");
		return flux.toString();
	}

	@Override
	protected String buildHistoricEnergyQuery(String bucket, Optional<Integer> influxEdgeId, ZonedDateTime fromDate,
			ZonedDateTime toDate, Set<ChannelAddress> channels) {
		// prepare query
		var builder = new StringBuilder() //
				.append("data = from(bucket: \"").append(bucket).append("\")") //

				.append("|> range(start: ").append(fromDate.toInstant()) //
				.append(", stop: ").append(toDate.toInstant()).append(")") //
				.append("|> filter(fn: (r) => r._measurement == \"").append(MEASUREMENT).append("\")");

		if (influxEdgeId.isPresent()) {
			builder.append("|> filter(fn: (r) => r." + OpenemsOEM.INFLUXDB_TAG + " == \"" + influxEdgeId.get() + "\")");
		}

		builder //
				.append("|> filter(fn : (r) => ") //
				.append(toChannelAddressFieldList(channels).toString()) //
				.append(")")

				.append("first = data |> first()") //
				.append("last = data |> last()") //
				.append("union(tables: [first, last])") //
				.append("|> difference()");
		return builder.toString();
	}

	@Override
	protected String buildHistoricEnergyPerPeriodQuery(String bucket, Optional<Integer> influxEdgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution) {
		if (resolution.getUnit().equals(ChronoUnit.MONTHS)) {
			fromDate = fromDate.with(TemporalAdjusters.firstDayOfMonth());
			if (!toDate.equals(toDate.with(TemporalAdjusters.firstDayOfMonth()))) {
				toDate = toDate.with(TemporalAdjusters.lastDayOfMonth()).plusDays(1);
			}
		}

		// prepare query
		Flux flux = Flux.from(bucket) //
				.range(fromDate.toInstant(), toDate.toInstant()) //
				.filter(Restrictions.measurement().equal(MEASUREMENT));

		if (influxEdgeId.isPresent()) {
			flux = flux.filter(Restrictions.tag(OpenemsOEM.INFLUXDB_TAG).equal(influxEdgeId.get().toString()));
		}

		flux = flux.filter(toChannelAddressFieldList(channels)) //
				.aggregateWindow(resolution.getValue(), resolution.getUnit(), "last") //
				.difference(true);

		return flux.toString();
	}

	/**
	 * Converts given {@link Set} of {@link ChannelAddress} to {@link Restrictions}
	 * separated by or.
	 *
	 * @param channels {@link Set} of {@link ChannelAddress}
	 * @return {@link Restrictions} separated by or
	 */
	private static Restrictions toChannelAddressFieldList(Set<ChannelAddress> channels) {
		var restrictions = channels.stream() //
				.map(channel -> Restrictions.field().equal(channel.toString())) //
				.toArray(restriction -> new Restrictions[restriction]);

		return Restrictions.or(restrictions);
	}

	/**
	 * Execute given query.
	 * 
	 * @param influxConnection a Influx-Connection
	 * @param query            to execute
	 * @return Result from database as {@link List} of {@link FluxTable}
	 * @throws OpenemsException on error
	 */
	private List<FluxTable> executeQuery(InfluxConnection influxConnection, String query) throws OpenemsException {
		this.assertQueryLimit();

		// Parse result
		List<FluxTable> queryResult;
		try {
			queryResult = influxConnection.client.getQueryApi().query(query);
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
	 * @param fromDate    start date from query
	 * @param resolution  {@link Resolution} to revert InfluxDB offset
	 * @return the historic data as Map
	 * @throws OpenemsException on error
	 */
	private static SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> convertHistoricDataQueryResult(
			List<FluxTable> queryResult, ZonedDateTime fromDate, Resolution resolution) throws OpenemsNamedException {
		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> table = new TreeMap<>();

		for (FluxTable fluxTable : queryResult) {
			for (FluxRecord record : fluxTable.getRecords()) {
				var timestamp = ZonedDateTime.ofInstant(record.getTime(), fromDate.getZone());

				// ignore first timestamp is before from date
				if (timestamp.isBefore(fromDate)) {
					continue;
				}
				timestamp = resolution.revertInfluxDbOffset(timestamp);

				var valueObj = record.getValue();
				final JsonElement value;
				if (valueObj == null) {
					value = JsonNull.INSTANCE;
				} else if (valueObj instanceof Number) {
					value = new JsonPrimitive((Number) valueObj);
				} else {
					value = new JsonPrimitive(valueObj.toString());
				}

				var channelAddresss = ChannelAddress.fromString(record.getField());

				var row = table.get(timestamp);
				if (row == null) {
					row = new TreeMap<>();
				}
				row.put(channelAddresss, value);

				table.put(timestamp, row);
			}
		}

		return table;
	}

	/**
	 * Converts the QueryResult of a Historic-Energy query to a properly typed Map.
	 *
	 * @param query       was executed
	 * @param queryResult the Query-Result
	 * @return the historic energy as Map
	 * @throws OpenemsException on error
	 */
	private static SortedMap<ChannelAddress, JsonElement> convertHistoricEnergyResult(String query,
			List<FluxTable> queryResult) throws OpenemsNamedException {
		SortedMap<ChannelAddress, JsonElement> map = new TreeMap<>();

		for (FluxTable fluxTable : queryResult) {
			for (FluxRecord record : fluxTable.getRecords()) {

				var valueObj = record.getValue();
				final JsonElement value;
				if (valueObj == null) {
					value = JsonNull.INSTANCE;
				} else if (valueObj instanceof Number) {
					var number = (Number) valueObj;
					if (number.intValue() < 0) {
						// do not consider negative values
						LOG.warn("Got negative Energy value [" + number + "] for query: " + query);
						value = JsonNull.INSTANCE;
					} else {
						value = new JsonPrimitive(number);
					}
				} else {
					value = new JsonPrimitive(valueObj.toString());
				}

				var channelAddresss = ChannelAddress.fromString(record.getField());

				map.put(channelAddresss, value);
			}
		}

		// Check if all values are null
		var areAllValuesNull = true;
		for (JsonElement value : map.values()) {
			if (!value.isJsonNull()) {
				areAllValuesNull = false;
				break;
			}
		}
		if (areAllValuesNull) {
			throw new OpenemsException("Energy values are not available for query: " + query);
		}

		return map;
	}
}