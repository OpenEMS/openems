package io.openems.shared.influxdb;

import java.net.URI;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import io.openems.common.utils.JsonUtils;

public class InfluxConnectorFluxQL extends InfluxConnectorCommon implements InfluxConnector {

	/**
	 * The Constructor.
	 *
	 * @param url          URL of the InfluxDB-Server (http://ip:port)
	 * @param org          The organisation; '-' for InfluxDB v1
	 * @param apiKey       The apiKey; 'username:password' for InfluxDB v1
	 * @param bucket       The bucket name; 'database/retentionPolicy' for InfluxDB
	 *                     v1
	 * @param isReadOnly   If true, a 'Read-Only-Mode' is activated, where no data
	 *                     is actually written to the database
	 * @param onWriteError A consumer for write-errors
	 */
	public InfluxConnectorFluxQL(URI url, String org, String apiKey, String bucket, boolean isReadOnly,
			Consumer<Throwable> onWriteError) {
		super(url, org, apiKey, bucket, isReadOnly, onWriteError);

	}

	@Override
	public synchronized void deactivate() {
		super.deactivate();
	}

	/**
	 * Execute given {@link Flux} query.
	 *
	 * @param query {@link Flux} to execute
	 * @return Result from database as {@link List} of {@link FluxTable}
	 * @throws OpenemsException on error
	 */
	public List<FluxTable> executeQuery(Flux query) throws OpenemsException {
		return this.executeQuery(query.toString());
	}

	/**
	 * Execute given query.
	 *
	 * @param query to execute
	 * @return Result from database as {@link List} of {@link FluxTable}
	 * @throws OpenemsException on error
	 */
	public List<FluxTable> executeQuery(String query) throws OpenemsException {
		if (Math.random() < this.queryLimit.getLimit()) {
			throw new OpenemsException(
					"InfluxDB read is temporarily blocked [" + this.queryLimit + "]. Query: " + query);
		}

		// Parse result
		List<FluxTable> queryResult;
		try {
			queryResult = this.getInfluxConnection().client.getQueryApi().query(query);
		} catch (RuntimeException e) {
			this.queryLimit.increase();
			this.log.error("InfluxDB query runtime error. Query: " + query + ", Error: " + e.getMessage());
			throw new OpenemsException(e.getMessage());
		}
		this.queryLimit.decrease();
		return queryResult;
	}

	/**
	 * Queries historic energy.
	 *
	 * @param influxEdgeId the unique, numeric Edge-ID; or Empty to query all Edges
	 * @param fromDate     the From-Date
	 * @param toDate       the To-Date
	 * @param channels     the Channels to query
	 * @return a map between ChannelAddress and value
	 * @throws OpenemsException on error
	 */
	public SortedMap<ChannelAddress, JsonElement> queryHistoricEnergy(Optional<Integer> influxEdgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels) throws OpenemsNamedException {
		if (Math.random() * 4 < this.queryLimit.getLimit()) {
			throw new OpenemsException("InfluxDB read is temporarily blocked for Energy values [" + this.queryLimit
					+ "]. Edge [" + influxEdgeId + "] FromDate [" + fromDate + "] ToDate [" + toDate + "]");
		}

		// handle empty call
		if (channels.isEmpty()) {
			return new TreeMap<>();
		}

		// prepare query
		var builder = new StringBuilder() //
				.append("data = from(bucket: \"").append(this.bucket).append("\")") //

				.append("|> range(start: ").append(fromDate.toInstant()) //
				.append(", stop: ").append(toDate.toInstant()).append(")") //
				.append("|> filter(fn: (r) => r._measurement == \"").append(InfluxConnector.MEASUREMENT).append("\")");

		if (influxEdgeId.isPresent()) {
			builder.append("|> filter(fn: (r) => r." + OpenemsOEM.INFLUXDB_TAG + " == \"" + influxEdgeId.get() + "\")");
		}

		builder //
				.append("|> filter(fn : (r) => ") //
				.append(InfluxConnectorFluxQL.toChannelAddressFieldList(channels).toString()) //
				.append(")")

				.append("first = data |> first()") //
				.append("last = data |> last()") //
				.append("union(tables: [first, last])") //
				.append("|> difference()");
		var query = builder.toString();

		// Execute query
		var queryResult = this.executeQuery(query);

		return this.convertHistoricEnergyResult(query, queryResult);
	}

	/**
	 * Queries historic energy per period.
	 *
	 * @param influxEdgeId the unique, numeric Edge-ID; or Empty to query all Edges
	 * @param fromDate     the From-Date
	 * @param toDate       the To-Date
	 * @param channels     the Channels to query
	 * @param resolution   the resolution in seconds
	 * @return the historic data as Map
	 * @throws OpenemsException on error
	 */
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricEnergyPerPeriod(
			Optional<Integer> influxEdgeId, ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels,
			Resolution resolution) throws OpenemsNamedException {
		if (Math.random() * 4 < this.queryLimit.getLimit()) {
			throw new OpenemsException("InfluxDB read is temporarily blocked for Energy values [" + this.queryLimit
					+ "]. Edge [" + influxEdgeId + "] FromDate [" + fromDate + "] ToDate [" + toDate + "]");
		}

		if (resolution.getUnit().equals(ChronoUnit.MONTHS)) {
			fromDate = fromDate.with(TemporalAdjusters.firstDayOfMonth());
			if (!toDate.equals(toDate.with(TemporalAdjusters.firstDayOfMonth()))) {
				toDate = toDate.with(TemporalAdjusters.lastDayOfMonth()).plusDays(1);
			}
		}

		// handle empty call
		if (channels.isEmpty()) {
			return new TreeMap<>();
		}

		// prepare query
		Flux flux = Flux.from(this.bucket) //
				.range(fromDate.toInstant(), toDate.toInstant()) //
				.filter(Restrictions.measurement().equal(InfluxConnector.MEASUREMENT));

		if (influxEdgeId.isPresent()) {
			flux = flux.filter(Restrictions.tag(OpenemsOEM.INFLUXDB_TAG).equal(influxEdgeId.get().toString()));
		}

		flux = flux.filter(InfluxConnectorFluxQL.toChannelAddressFieldList(channels)) //
				.aggregateWindow(resolution.getValue(), resolution.getUnit(), "last") //
				.difference(true);

		var queryResult = this.executeQuery(flux);

		return InfluxConnectorFluxQL.convertHistoricDataQueryResult(queryResult, fromDate, resolution);
	}

	/**
	 * Queries historic data.
	 *
	 * @param influxEdgeId the unique, numeric Edge-ID; or Empty to query all Edges
	 * @param fromDate     the From-Date
	 * @param toDate       the To-Date
	 * @param channels     the Channels to query
	 * @param resolution   the resolution in seconds
	 * @return the historic data as Map
	 * @throws OpenemsException on error
	 */
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricData(
			Optional<Integer> influxEdgeId, ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels,
			Resolution resolution) throws OpenemsNamedException {

		// handle empty call
		if (channels.isEmpty()) {
			return new TreeMap<>();
		}

		// remove 5 minutes to prevent shifted timeline
		var fromInstant = fromDate.toInstant().minus(5, ChronoUnit.MINUTES);

		// prepare query
		Flux flux = Flux.from(this.bucket) //
				.range(fromInstant, toDate.toInstant()) //
				.filter(Restrictions.measurement().equal(InfluxConnector.MEASUREMENT));

		if (influxEdgeId.isPresent()) {
			flux = flux.filter(Restrictions.tag(OpenemsOEM.INFLUXDB_TAG).equal(influxEdgeId.get().toString()));
		}

		flux = flux.filter(InfluxConnectorFluxQL.toChannelAddressFieldList(channels)) //
				.aggregateWindow(resolution.getValue(), resolution.getUnit(), "mean");

		// Execute query
		var queryResult = this.executeQuery(flux);

		return InfluxConnectorFluxQL.convertHistoricDataQueryResult(queryResult, fromDate, resolution);
	}

	/**
	 * Queries the latest available channel values.
	 *
	 * @param influxEdgeId     the unique, numeric Edge-ID; or Empty to query all
	 *                         Edges
	 * @param channelAddresses the {@link ChannelAddress}es
	 * @return a map of {@link ChannelAddress}es and values
	 * @throws OpenemsException on error
	 */
	public Map<ChannelAddress, JsonElement> queryChannelValuesMap(Optional<Integer> influxEdgeId,
			Set<ChannelAddress> channelAddresses) {
		var result = channelAddresses.stream()
				.collect(Collectors.toMap(Function.identity(), c -> (JsonElement) JsonNull.INSTANCE));
		try {
			for (var channelAddress : channelAddresses) {
				// prepare query
				var builder = new StringBuilder() //
						.append("from(bucket: \"").append(this.bucket).append("\")") //
						.append("|> range(start: -16m)") //
						.append("|> filter(fn: (r) => ") //
						.append("r._measurement == \"").append(InfluxConnector.MEASUREMENT).append("\" ");
				if (influxEdgeId.isPresent()) {
					builder.append("and r." + OpenemsOEM.INFLUXDB_TAG + " == \"" + influxEdgeId.get() + "\" ");
				}
				builder //
						.append("and r._field == \"").append(channelAddress.toString()).append("\")") //
						.append("|> last()");
				var query = builder.toString();

				// Execute query
				var queryResult = this.executeQuery(query);

				for (FluxTable fluxTable : queryResult) {
					for (FluxRecord record : fluxTable.getRecords()) {
						result.put(channelAddress, JsonUtils.getAsJsonElement(record.getValue()));
					}
				}
			}

		} catch (OpenemsException e) {
			this.log.error(e.getMessage());
			e.printStackTrace();

		}
		return result;
	}

	/**
	 * queries the given channel values.
	 * 
	 * @param influxEdgeId     the edge id
	 * @param channelAddresses channel addresses to query
	 * @return the sorted map of results
	 */
	public SortedMap<ChannelAddress, JsonElement> queryChannelValues(Optional<Integer> influxEdgeId,
			Set<ChannelAddress> channelAddresses) {
		var map = this.queryChannelValuesMap(influxEdgeId, channelAddresses);
		var sortedMap = map.entrySet().stream().sorted(Map.Entry.<ChannelAddress, JsonElement>comparingByKey())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue,
						TreeMap::new));
		return sortedMap;
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
	private SortedMap<ChannelAddress, JsonElement> convertHistoricEnergyResult(String query,
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
						this.log.warn("Got negative Energy value [" + number + "] for query: " + query);
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

}
