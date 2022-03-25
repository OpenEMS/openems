package io.openems.shared.influxdb;

import java.net.URI;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.InfluxDBClientOptions;
import com.influxdb.client.WriteApi;
import com.influxdb.client.WriteOptions;
import com.influxdb.client.write.Point;
import com.influxdb.client.write.events.BackpressureEvent;
import com.influxdb.client.write.events.WriteErrorEvent;
import com.influxdb.exceptions.InfluxException;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.influxdb.query.dsl.Flux;
import com.influxdb.query.dsl.functions.restriction.Restrictions;

import io.openems.common.OpenemsOEM;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.StringUtils;
import io.reactivex.BackpressureOverflowStrategy;
import okhttp3.OkHttpClient;

public class InfluxConnector {

	public static final String MEASUREMENT = "data";

	private static final Logger LOG = LoggerFactory.getLogger(InfluxConnector.class);
	private static final int CONNECT_TIMEOUT = 10; // [s]
	private static final int READ_TIMEOUT = 60; // [s]
	private static final int WRITE_TIMEOUT = 10; // [s]

	private final Logger log = LoggerFactory.getLogger(InfluxConnector.class);

	private final URI url;
	private final String org;
	private final String apiKey;
	private final String bucket;
	private final boolean isReadOnly;
	private final Consumer<Throwable> onWriteError;

	/**
	 * The Constructor.
	 *
	 * @param url             URL of the InfluxDB-Server (http://ip:port)
	 * @param org             The Influx Organisation
	 * @param apiKey          The apiKey or username:password
	 * @param bucket          The bucket name.
	 * @param isReadOnly      If true, a 'Read-Only-Mode' is activated, where no
	 *                        data is actually written to the database
	 * @param onWriteError    A callback for write-errors, i.e. '(failedPoints,
	 *                        throwable) -&gt; {}'
	 */
	public InfluxConnector(URI url, String org, String apiKey, String bucket,
			boolean isReadOnly, Consumer<Throwable> onWriteError) {
		this.url = url;
		this.org = org;
		this.apiKey = apiKey;
		this.bucket = bucket;
		this.isReadOnly = isReadOnly;
		this.onWriteError = onWriteError;
	}

	/**
	 * The Constructor (legacy).
	 *
	 * @param ip              IP-Address of the InfluxDB-Server
	 * @param port            Port of the InfluxDB-Server
	 * @param username        The username
	 * @param password        The password
	 * @param database        The database name. If it does not exist, it will be
	 *                        created
	 * @param retentionPolicy For the InfluxDB database setting
	 * @param isReadOnly      If true, a 'Read-Only-Mode' is activated, where no
	 *                        data is actually written to the database
	 * @param onWriteError    A callback for write-errors, i.e. '(failedPoints,
	 *                        throwable) -&gt; {}'
	 */
	public InfluxConnector(String ip, int port, String username, String password, String database,
			String retentionPolicy, boolean readOnly, Consumer<Throwable> onWriteError) {
		this.url = URI.create("http://" + ip + ":" + port);
		this.org = "-";
		this.apiKey = String.format("%s:%s", username == null ? "" : username, 
				password == null ? "" : String.valueOf(password));
		this.bucket = String.format("%s/%s", database,
				retentionPolicy == null ? "" : retentionPolicy);
		this.isReadOnly = readOnly;
		this.onWriteError = onWriteError;
	}

	private InfluxDBClient _influxDB = null;
	private WriteApi _writeApi = null;

	public String getDatabase() {
		return this.bucket;
	}

	/**
	 * Get InfluxDB Connection.
	 *
	 * @return the {@link InfluxDB} connection
	 */
	private synchronized InfluxDBClient getConnection() {
		if (this._influxDB == null) {
			var okHttpClientBuilder = new OkHttpClient().newBuilder() //
					.connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS) //
					.readTimeout(READ_TIMEOUT, TimeUnit.SECONDS) //
					.writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS);

			// copied options from InfluxDBClientFactory.createV1
			// to set timeout
			var options = InfluxDBClientOptions.builder() //
					.url(this.url.toString()) //
					.org(org) //
					.authenticateToken(String.format(this.apiKey).toCharArray()) //
					.bucket(this.bucket) //
					.okHttpClient(okHttpClientBuilder) //
					.build();

			this._influxDB = InfluxDBClientFactory.create(options);
		}
		return this._influxDB;
	}

	/**
	 * Get InfluxDB write api.
	 *
	 * @return the {@link WriteApi}
	 */
	private synchronized WriteApi getWriteApi() {
		if (this._writeApi == null) {
			var writeOptions = WriteOptions.builder() //
					.jitterInterval(1_000 /* milliseconds */) //
					.bufferLimit(2 /* entries */) //
					.backpressureStrategy(BackpressureOverflowStrategy.DROP_OLDEST) //
					.build();

			var writeApi = this.getConnection().makeWriteApi(writeOptions);

			// add listeners
			writeApi.listenEvents(BackpressureEvent.class, event -> {
				this.log.info("!!!BACKPRESSURE!!!");
			});
			writeApi.listenEvents(WriteErrorEvent.class, event -> {
				this.onWriteError.accept(event.getThrowable());
			});

			this._writeApi = writeApi;
		}
		return this._writeApi;
	}

	/**
	 * Close current {@link InfluxDBClient}.
	 */
	public void deactivate() {
		if (this._influxDB != null) {
			this._influxDB.close();
		}
	}

	private static class RandomLimit {
		private static final double MAX_LIMIT = 0.95;
		private static final double MIN_LIMIT = 0;
		private static final double STEP = 0.01;

		private double limit = 0;

		protected synchronized void increase() {
			this.limit += STEP;
			if (this.limit > MAX_LIMIT) {
				this.limit = MAX_LIMIT;
			}
		}

		protected synchronized void decrease() {
			this.limit -= STEP;
			if (this.limit <= MIN_LIMIT) {
				this.limit = MIN_LIMIT;
			}
		}

		protected double getLimit() {
			return this.limit;
		}

		@Override
		public String toString() {
			return String.format("%.3f", this.limit);
		}
	}

	private final RandomLimit queryLimit = new RandomLimit();

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

		var influxDB = this.getConnection();

		// Parse result
		List<FluxTable> queryResult;
		try {
			queryResult = influxDB.getQueryApi().query(query);
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
				.append("|> filter(fn: (r) => r._measurement == \"").append(MEASUREMENT).append("\")");

		if (influxEdgeId.isPresent()) {
			builder.append("|> filter(fn: (r) => r.fems == \"" + influxEdgeId.get() + "\")");
		}

		builder //
				.append("|> filter(fn : (r) => ") //
				.append(InfluxConnector.toChannelAddressFieldList(channels).toString()) //
				.append(")")

				.append("first = data |> first()") //
				.append("last = data |> last()") //
				.append("union(tables: [first, last])") //
				.append("|> difference()");
		var query = builder.toString();

		// Execute query
		var queryResult = this.executeQuery(query);

		return InfluxConnector.convertHistoricEnergyResult(query, queryResult);
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
				.filter(Restrictions.measurement().equal(MEASUREMENT));

		if (influxEdgeId.isPresent()) {
			flux = flux.filter(Restrictions.tag(OpenemsOEM.INFLUXDB_TAG).equal(influxEdgeId.get().toString()));
		}

		flux = flux.filter(InfluxConnector.toChannelAddressFieldList(channels)) //
				.aggregateWindow(resolution.getValue(), resolution.getUnit(), "last") //
				.difference(true);

		var queryResult = this.executeQuery(flux);

		return InfluxConnector.convertHistoricDataQueryResult(queryResult, fromDate, resolution);
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
				.filter(Restrictions.measurement().equal(MEASUREMENT));

		if (influxEdgeId.isPresent()) {
			flux = flux.filter(Restrictions.tag(OpenemsOEM.INFLUXDB_TAG).equal(influxEdgeId.get().toString()));
		}

		flux = flux.filter(InfluxConnector.toChannelAddressFieldList(channels)) //
				.aggregateWindow(resolution.getValue(), resolution.getUnit(), "mean");

		// Execute query
		var queryResult = this.executeQuery(flux);

		return InfluxConnector.convertHistoricDataQueryResult(queryResult, fromDate, resolution);
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
				timestamp = resolution.revertInfluxDBOffset(timestamp);

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
	 * Actually write the Point to InfluxDB.
	 *
	 * @param point the InfluxDB Point
	 * @throws OpenemsException on error
	 */
	public void write(Point point) throws OpenemsException {
		if (this.isReadOnly) {
			this.log.info("Read-Only-Mode is activated. Not writing points: "
					+ StringUtils.toShortString(point.toLineProtocol(), 100));
			return;
		}
		try {
			this.getWriteApi().writePoint(point);
		} catch (InfluxException e) {
			throw new OpenemsException("Unable to write point: " + e.getMessage());
		}
	}
}
