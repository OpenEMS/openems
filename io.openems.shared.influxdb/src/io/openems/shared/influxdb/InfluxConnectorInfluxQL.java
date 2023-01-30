package io.openems.shared.influxdb;

import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.influxdb.client.domain.InfluxQLQuery;
import com.influxdb.query.InfluxQLQueryResult;
import com.influxdb.query.InfluxQLQueryResult.Series.Record;

import io.openems.common.OpenemsOEM;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;

public class InfluxConnectorInfluxQL extends InfluxConnectorCommon implements InfluxConnector {
	private String database;

	private final Resolution fiveteenMinResolution = new Resolution(15, ChronoUnit.MINUTES);

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
	public InfluxConnectorInfluxQL(URI url, String org, String apiKey, String bucket, boolean isReadOnly,
			Consumer<Throwable> onWriteError) {
		super(url, org, apiKey, bucket, isReadOnly, onWriteError);

		String[] b = bucket.split("/");
		this.database = b[0];
	}

	@Override
	public synchronized void deactivate() {
		super.deactivate();
	}

	/**
	 * Execute given query.
	 *
	 * @param query to execute
	 * @return the {@link InfluxQLQueryResult}
	 * @throws OpenemsException on error
	 */
	public InfluxQLQueryResult executeQuery(String query) throws OpenemsException {
		if (Math.random() < this.queryLimit.getLimit()) {
			throw new OpenemsException(
					"InfluxDB read is temporarily blocked [" + this.queryLimit + "]. Query: " + query);
		}

		// see (https://github.com/influxdata/influxdb-client-java/tree/master/client ->
		// InfluxQL queries) for example request

		try {
			var influxDB = this.getInfluxConnection();
			InfluxQLQueryResult queryResult = influxDB.client.getInfluxQLQueryApi().query(
					new InfluxQLQuery(query, this.database).setPrecision(InfluxQLQuery.InfluxQLPrecision.MILLISECONDS)); // .getclient.getQueryApi().query(query);
			this.queryLimit.decrease();
			return queryResult;

		} catch (RuntimeException e) {
			this.queryLimit.increase();
			this.log.error("InfluxDB query runtime error. Query: " + query + ", Error: " + e.getMessage());
			throw new OpenemsException("InfluxDB query runtime error. Query: " + query + ", Error: " + e.getMessage());
		}
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

		if (channels.isEmpty()) {
			return new TreeMap<>();
		}

		// Prepare query string
		var b = new StringBuilder("SELECT ");
		b.append(this.toChannelAddressStringEnergy(channels));
		b.append(" FROM data WHERE ");
		if (influxEdgeId.isPresent()) {
			b.append(OpenemsOEM.INFLUXDB_TAG + " = '" + influxEdgeId.get() + "' AND ");
		}
		b.append("time > ");
		b.append(String.valueOf(fromDate.toEpochSecond()));
		b.append("s");
		b.append(" AND time < ");
		b.append(String.valueOf(toDate.toEpochSecond()));
		b.append("s");
		var query = b.toString();

		var queryResult = this.executeQuery(query);

		var result = this.convertHistoricEnergyResult(query, queryResult, fromDate.getZone());
		return result;
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
			// TODO use 31 days as approx. for 1 month, because influxQL is unable to use
			// exact month values
			resolution = new Resolution(31, ChronoUnit.DAYS);
		}

		if (channels.isEmpty()) {
			return new TreeMap<>();
		}
		final var resolutionOffset = this.getResolutionOffset(fromDate, resolution);

		// Prepare query string
		var b = new StringBuilder("SELECT ");
		b.append(this.toChannelAddressStringNonNegativeDifferenceLast(channels));
		b.append(" FROM data WHERE ");
		if (influxEdgeId.isPresent()) {
			b.append(OpenemsOEM.INFLUXDB_TAG + " = '" + influxEdgeId.get() + "' AND ");
		}
		b.append("time > ");
		b.append(String.valueOf(fromDate.toEpochSecond()));
		b.append("s");
		b.append(" AND time < ");
		b.append(String.valueOf(toDate.toEpochSecond()));
		b.append("s");
		b.append(" GROUP BY time( ");
		b.append(resolution.toSeconds());
		b.append("s, ");
		b.append(resolutionOffset);
		b.append("s) fill(null)");
		String query = b.toString();

		var queryResult = this.executeQuery(query);

		var result = InfluxConnectorInfluxQL.convertHistoricDataQueryResult(queryResult, fromDate, resolution);
		return result;
	}

	/**
	 * calculate the resolution offset for the given date and resolution.
	 * 
	 * @implNote 2022.02.23 Hint to understand the behavior of the influx "group by
	 *           time()" function: InfluxDB is using timestamp 0
	 *           (1970-01-01T00:00:00Z) as start time and for each timestamp that is
	 *           dividable by the group interval, it creates a boundary so if group
	 *           by time is 420 the boundarys are 0, timestamp 420, timestamp
	 *           840,... timestamp 1577836680 (2019-12-31 23:58:00) So if fromDate
	 *           starts at 2020-01-01 00:00:00 the first chunk goes from 2019-12-31
	 *           23:58:00 to 2020-01-01 00:05:00
	 * 
	 *           see
	 *           https://www.fromkk.com/posts/time-boundary-in-influxdb-group-by-time-statement/
	 * @param fromDate   the date to calculate the offset from
	 * @param resolution the resolution
	 * @return the resolution offset in seconds
	 */
	private int getResolutionOffset(ZonedDateTime fromDate, Resolution resolution) {
		var res = resolution.toSeconds();
		return (int) (fromDate.toEpochSecond() % res);
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

		if (channels.isEmpty()) {
			return new TreeMap<>();
		}

		// Prepare query string
		StringBuilder query = new StringBuilder("SELECT ");
		query.append(this.toChannelAddressStringData(channels));
		query.append(" FROM data WHERE ");
		if (influxEdgeId.isPresent()) {
			query.append(OpenemsOEM.INFLUXDB_TAG + " = '" + influxEdgeId.get() + "' AND ");
		}
		query.append("time > ");
		query.append(String.valueOf(fromDate.toInstant().getEpochSecond()));
		query.append("s");
		query.append(" AND time < ");
		query.append(String.valueOf(toDate.toEpochSecond()));
		query.append("s");
		query.append(" GROUP BY time(");
		query.append(resolution.toSeconds());
		query.append("s) fill(null)");

		var queryResult = this.executeQuery(query.toString());
		return InfluxConnectorInfluxQL.convertHistoricDataQueryResult(queryResult, fromDate, resolution);
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
	public SortedMap<ChannelAddress, JsonElement> queryChannelValues(Optional<Integer> influxEdgeId,
			Set<ChannelAddress> channelAddresses) {
		try {
			var query = new StringBuilder("SELECT ");
			query.append(this.toChannelAddressStringDataLast(channelAddresses));
			query.append(" FROM data WHERE ");
			if (influxEdgeId.isPresent()) {
				query.append(OpenemsOEM.INFLUXDB_TAG + " = '" + influxEdgeId.get() + "' AND ");
			}

			// peek into the last 7 days NOTE: within FLUX code 5min peek time is used
			var fromDate = ZonedDateTime.now().minus(7, ChronoUnit.DAYS);
			query.append("time > now() - 7d ");

			query.append(" GROUP BY time(");
			query.append(this.fiveteenMinResolution.toSeconds());
			query.append("s) fill(null) ");
			query.append("ORDER BY time DESC LIMIT 1;");

			var queryResult = this.executeQuery(query.toString());
			SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> convertedTable = InfluxConnectorInfluxQL
					.convertHistoricDataQueryResult(queryResult, fromDate, this.fiveteenMinResolution);
			return convertedTable.get(convertedTable.firstKey());
		} catch (OpenemsNamedException e) {
			this.log.error(e.getMessage());
			e.printStackTrace();

		}
		return new TreeMap<ChannelAddress, JsonElement>();
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
			InfluxQLQueryResult queryResult, ZonedDateTime fromDate, Resolution resolution)
			throws OpenemsNamedException {
		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> table = new TreeMap<>();

		for (InfluxQLQueryResult.Result result : queryResult.getResults()) {
			var seriess = result.getSeries();
			if (seriess != null) {
				for (InfluxQLQueryResult.Series series : seriess) {
					// create ChannelAddress index
					ArrayList<ChannelAddress> addressIndex = new ArrayList<>();
					for (Map.Entry<String, Integer> column : series.getColumns().entrySet()) {
						if (column.getKey().equals("time")) {
							continue;
						}
						addressIndex.add(ChannelAddress.fromString(column.getKey()));
					}
					// add all data
					List<Record> values = series.getValues();
					for (Record rec : values) {

						// get timestamp
						Double d = Double.parseDouble(rec.getValues()[0].toString());
						Instant timestampInstant = Instant.ofEpochMilli((long) d.longValue());
						ZonedDateTime timestamp = ZonedDateTime.ofInstant(timestampInstant, fromDate.getZone());
						if (timestamp.isBefore(fromDate)) {
							continue;
						}
						// timestamp = resolution.revertInfluxDbOffset(timestamp);

						SortedMap<ChannelAddress, JsonElement> tableRow = new TreeMap<>();
						Object[] objs = rec.getValues();
						for (int columnIndex = 0; columnIndex < addressIndex.size(); columnIndex++) {
							// Note: ignoring index '0' here as it is the 'timestamp'
							var address = addressIndex.get(columnIndex);
							var valueObj = objs[columnIndex + 1];
							JsonElement value;
							if (valueObj == null) {
								value = JsonNull.INSTANCE;
							} else if (valueObj instanceof Number) {
								value = new JsonPrimitive((Number) valueObj);
							} else {
								value = new JsonPrimitive(valueObj.toString());
							}
							tableRow.put(address, value);
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
	 * @param query       was executed
	 * @param queryResult the Query-Result
	 * @param timezone    the timezone
	 * @return the historic energy as Map
	 * @throws OpenemsException on error
	 */
	private SortedMap<ChannelAddress, JsonElement> convertHistoricEnergyResult(String query,
			InfluxQLQueryResult queryResult, ZoneId timezone) throws OpenemsNamedException {
		SortedMap<ChannelAddress, JsonElement> map = new TreeMap<>();
		for (InfluxQLQueryResult.Result result : queryResult.getResults()) {
			var seriess = result.getSeries();
			if (seriess != null) {
				for (InfluxQLQueryResult.Series series : seriess) {
					// create ChannelAddress index
					ArrayList<ChannelAddress> addressIndex = new ArrayList<>();
					for (Map.Entry<String, Integer> column : series.getColumns().entrySet()) {
						if (column.getKey().equals("time")) {
							continue;
						}
						addressIndex.add(ChannelAddress.fromString(column.getKey()));
					}

					// add all data
					List<Record> values = series.getValues();
					for (Record rec : values) {
						Object[] objs = rec.getValues();
						for (int columnIndex = 0; columnIndex < addressIndex.size(); columnIndex++) {
							// Note: ignoring index '0' here as it is the 'timestamp'
							var address = addressIndex.get(columnIndex);
							Object valueObj = objs[columnIndex + 1];
							JsonElement value;
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
							map.put(address, value);
						}
					}

				}
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

	protected String toChannelAddressStringData(Set<ChannelAddress> channels) throws OpenemsException {
		ArrayList<String> channelAddresses = new ArrayList<>();
		for (ChannelAddress channel : channels) {
			channelAddresses.add("MEAN(\"" + channel.toString() + "\") AS \"" + channel.toString() + "\"");
		}
		return String.join(", ", channelAddresses);
	}

	protected String toChannelAddressStringDataLast(Set<ChannelAddress> channels) throws OpenemsException {
		ArrayList<String> channelAddresses = new ArrayList<>();
		for (ChannelAddress channel : channels) {
			channelAddresses.add("LAST(\"" + channel.toString() + "\") AS \"" + channel.toString() + "\"");
		}
		return String.join(", ", channelAddresses);
	}

	protected String toChannelAddressStringEnergy(Set<ChannelAddress> channels) throws OpenemsException {
		ArrayList<String> channelAddresses = new ArrayList<>();
		for (var channel : channels) {
			channelAddresses.add("LAST(\"" + channel.toString() + "\") - FIRST(\"" + channel.toString() + "\") AS \""
					+ channel.toString() + "\"");
		}
		return String.join(", ", channelAddresses);
	}

	protected String toChannelAddressStringNonNegativeDifferenceLast(Set<ChannelAddress> channels)
			throws OpenemsException {
		ArrayList<String> channelAddresses = new ArrayList<>();
		for (var channel : channels) {
			channelAddresses.add(
					"NON_NEGATIVE_DIFFERENCE(LAST(\"" + channel.toString() + "\")) AS \"" + channel.toString() + "\"");
		}
		return String.join(", ", channelAddresses);
	}

}
