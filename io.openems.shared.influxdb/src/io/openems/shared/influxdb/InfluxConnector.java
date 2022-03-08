package io.openems.shared.influxdb;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import org.influxdb.BatchOptions;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBException;
import org.influxdb.InfluxDBFactory;
import org.influxdb.InfluxDBIOException;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.influxdb.dto.QueryResult.Result;
import org.influxdb.dto.QueryResult.Series;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import io.openems.common.OpenemsOEM;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.StringUtils;
import io.openems.common.utils.ThreadPoolUtils;
import okhttp3.OkHttpClient;

public class InfluxConnector {

	public static final String MEASUREMENT = "data";

	private static final Logger LOG = LoggerFactory.getLogger(InfluxConnector.class);
	private static final int CONNECT_TIMEOUT = 10; // [s]
	private static final int READ_TIMEOUT = 60; // [s]
	private static final int WRITE_TIMEOUT = 10; // [s]

	private static final int EXECUTOR_MIN_THREADS = 1;
	private static final int EXECUTOR_MAX_THREADS = 50;
	private static final int EXECUTOR_QUEUE_SIZE = 100;

	private final Logger log = LoggerFactory.getLogger(InfluxConnector.class);

	private final String ip;
	private final int port;
	private final String username;
	private final String password;
	private final String database;
	private final String retentionPolicy;
	private final boolean isReadOnly;
	private final BiConsumer<Iterable<Point>, Throwable> onWriteError;
	private final ThreadPoolExecutor executor = new ThreadPoolExecutor(EXECUTOR_MIN_THREADS, EXECUTOR_MAX_THREADS, 60L,
			TimeUnit.SECONDS, //
			new ArrayBlockingQueue<>(EXECUTOR_QUEUE_SIZE), //
			new ThreadFactoryBuilder().setNameFormat("InfluxConnector-%d").build(), //
			new ThreadPoolExecutor.DiscardPolicy());
	private final ScheduledExecutorService debugLogExecutor = Executors.newSingleThreadScheduledExecutor();

	/**
	 * The Constructor.
	 *
	 * @param ip           IP-Address of the InfluxDB-Server
	 * @param port         Port of the InfluxDB-Server
	 * @param username     The username
	 * @param password     The password
	 * @param database     The database name. If it does not exist, it will be
	 *                     created
	 * @param isReadOnly   If true, a 'Read-Only-Mode' is activated, where no data
	 *                     is actually written to the database
	 * @param onWriteError A callback for write-errors, i.e. '(failedPoints,
	 *                     throwable) -&gt; {}'
	 */
	public InfluxConnector(String ip, int port, String username, String password, String database,
			String retentionPolicy, boolean isReadOnly, BiConsumer<Iterable<Point>, Throwable> onWriteError) {
		this.ip = ip;
		this.port = port;
		this.username = username;
		this.password = password;
		this.database = database;
		this.retentionPolicy = retentionPolicy;
		this.isReadOnly = isReadOnly;
		this.onWriteError = onWriteError;
		this.debugLogExecutor.scheduleWithFixedDelay(() -> {
			var queueSize = this.executor.getQueue().size();
			this.log.info(String.format("[monitor] Pool: %d, Active: %d, Pending: %d, Completed: %d %s",
					this.executor.getPoolSize(), //
					this.executor.getActiveCount(), //
					this.executor.getQueue().size(), //
					this.executor.getCompletedTaskCount(), //
					queueSize == EXECUTOR_QUEUE_SIZE ? "!!!BACKPRESSURE!!!" : "")); //
		}, 10, 10, TimeUnit.SECONDS);
	}

	private InfluxDB _influxDB = null;

	public String getDatabase() {
		return this.database;
	}

	/**
	 * Get InfluxDB Connection.
	 *
	 * @return the {@link InfluxDB} connection
	 */
	private InfluxDB getConnection() {
		if (this._influxDB == null) {
			var okHttpClientBuilder = new OkHttpClient().newBuilder() //
					.connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS) //
					.readTimeout(READ_TIMEOUT, TimeUnit.SECONDS) //
					.writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS);
			var influxDB = InfluxDBFactory.connect("http://" + this.ip + ":" + this.port, this.username, this.password,
					okHttpClientBuilder);
			try {
				influxDB.query(new Query("CREATE DATABASE " + this.database, ""));
			} catch (InfluxDBException e) {
				this.log.warn("InfluxDB-Exception: " + e.getMessage());
			}
			influxDB.setDatabase(this.database);
			influxDB.setRetentionPolicy(this.retentionPolicy);
			influxDB.enableBatch(BatchOptions.DEFAULTS //
					.precision(TimeUnit.MILLISECONDS) //
					.flushDuration(1_000 /* milliseconds */) //
					.jitterDuration(1_000 /* milliseconds */) //
					.actions(1_000 /* entries */) //
					.bufferLimit(1_000_000 /* entries */) //
					.exceptionHandler(this.onWriteError));
			this._influxDB = influxDB;
		}
		return this._influxDB;
	}

	public void deactivate() {
		// Shutdown executor
		ThreadPoolUtils.shutdownAndAwaitTermination(this.executor, 5);
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
	 * Copied from backend.timedata.influx.provider.
	 *
	 * @param query the Query
	 * @return the {@link QueryResult}
	 * @throws OpenemsException on error
	 */
	public QueryResult executeQuery(String query) throws OpenemsException {
		if (Math.random() < this.queryLimit.getLimit()) {
			throw new OpenemsException(
					"InfluxDB read is temporarily blocked [" + this.queryLimit + "]. Query: " + query);
		}

		var influxDB = this.getConnection();

		// Parse result
		QueryResult queryResult;
		try {
			queryResult = influxDB.query(new Query(query, this.database), TimeUnit.MILLISECONDS);
		} catch (RuntimeException e) {
			this.queryLimit.increase();
			this.log.error("InfluxDB query runtime error. Query: " + query + ", Error: " + e.getMessage());
			throw new OpenemsException(e.getMessage());
		}
		if (queryResult.hasError()) {
			this.queryLimit.increase();
			this.log.error("InfluxDB query error. Query: " + query + ", Error: " + queryResult.getError());
			throw new OpenemsException(queryResult.getError());
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

		// Prepare query string
		var b = new StringBuilder("SELECT ");
		b.append(InfluxConnector.toChannelAddressStringEnergy(channels));
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

		// Execute query
		var queryResult = this.executeQuery(query);

		return InfluxConnector.convertHistoricEnergyResult(query, queryResult, fromDate.getZone());
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
			int resolution) throws OpenemsNamedException {
		if (Math.random() * 4 < this.queryLimit.getLimit()) {
			throw new OpenemsException("InfluxDB read is temporarily blocked for Energy values [" + this.queryLimit
					+ "]. Edge [" + influxEdgeId + "] FromDate [" + fromDate + "] ToDate [" + toDate + "]");
		}

		// handle empty call
		if (channels.isEmpty()) {
			return new TreeMap<>();
		}

		// Prepare query string
		var b = new StringBuilder("SELECT ");
		b.append(InfluxConnector.toChannelAddressStringNonNegativeDifferenceLast(channels));
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
		b.append(" GROUP BY time(");
		b.append(resolution);
		b.append("s) fill(null)");
		var query = b.toString();

		// Execute query
		var queryResult = this.executeQuery(query);

		return InfluxConnector.convertHistoricDataQueryResult(queryResult, fromDate.getZone());
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
			int resolution) throws OpenemsNamedException {
		// Prepare query string
		var query = new StringBuilder("SELECT ");
		query.append(InfluxConnector.toChannelAddressStringData(channels));
		query.append(" FROM data WHERE ");
		if (influxEdgeId.isPresent()) {
			query.append(OpenemsOEM.INFLUXDB_TAG + " = '" + influxEdgeId.get() + "' AND ");
		}
		query.append("time > ");
		query.append(String.valueOf(fromDate.toEpochSecond()));
		query.append("s");
		query.append(" AND time < ");
		query.append(String.valueOf(toDate.toEpochSecond()));
		query.append("s");
		query.append(" GROUP BY time(");
		query.append(resolution);
		query.append("s) fill(null)");

		// Execute query
		var queryResult = this.executeQuery(query.toString());

		return InfluxConnector.convertHistoricDataQueryResult(queryResult, fromDate.getZone());
	}

	/**
	 * Converts the QueryResult of a Historic-Data query to a properly typed Table.
	 *
	 * @param queryResult the Query-Result
	 * @return the historic data as Map
	 * @throws OpenemsException on error
	 */
	private static SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> convertHistoricDataQueryResult(
			QueryResult queryResult, ZoneId timezone) throws OpenemsNamedException {
		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> table = new TreeMap<>();
		for (Result result : queryResult.getResults()) {
			var seriess = result.getSeries();
			if (seriess != null) {
				for (Series series : seriess) {
					// create ChannelAddress index
					var addressIndex = new ArrayList<ChannelAddress>();
					for (String column : series.getColumns()) {
						if (column.equals("time")) {
							continue;
						}
						addressIndex.add(ChannelAddress.fromString(column));
					}

					// add all data
					for (List<Object> values : series.getValues()) {
						SortedMap<ChannelAddress, JsonElement> tableRow = new TreeMap<>();
						// get timestamp
						var timestampInstant = Instant.ofEpochMilli((long) ((Double) values.get(0)).doubleValue());
						var timestamp = ZonedDateTime.ofInstant(timestampInstant, timezone);
						for (var columnIndex = 0; columnIndex < addressIndex.size(); columnIndex++) {
							// Note: ignoring index '0' here as it is the 'timestamp'
							var address = addressIndex.get(columnIndex);
							var valueObj = values.get(columnIndex + 1);
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
	 * @param queryResult the Query-Result
	 * @return the historic energy as Map
	 * @throws OpenemsException on error
	 */
	private static SortedMap<ChannelAddress, JsonElement> convertHistoricEnergyResult(String query,
			QueryResult queryResult, ZoneId timezone) throws OpenemsNamedException {
		SortedMap<ChannelAddress, JsonElement> map = new TreeMap<>();
		for (Result result : queryResult.getResults()) {
			var seriess = result.getSeries();
			if (seriess != null) {
				for (Series series : seriess) {
					// create ChannelAddress index
					var addressIndex = new ArrayList<ChannelAddress>();
					for (String column : series.getColumns()) {
						if (column.equals("time")) {
							continue;
						}
						addressIndex.add(ChannelAddress.fromString(column));
					}

					// add all data
					for (List<Object> values : series.getValues()) {
						for (var columnIndex = 0; columnIndex < addressIndex.size(); columnIndex++) {
							// Note: ignoring index '0' here as it is the 'timestamp'
							var address = addressIndex.get(columnIndex);
							var valueObj = values.get(columnIndex + 1);
							JsonElement value;
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
							map.put(address, value);
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
				throw new OpenemsException("Energy values are not available for query: " + query);
			}
		}

		return map;
	}

	protected static String toChannelAddressStringData(Set<ChannelAddress> channels) throws OpenemsException {
		var channelAddresses = new ArrayList<String>();
		for (ChannelAddress channel : channels) {
			channelAddresses.add("MEAN(\"" + channel.toString() + "\") AS \"" + channel.toString() + "\"");
		}
		return String.join(", ", channelAddresses);
	}

	protected static String toChannelAddressStringEnergy(Set<ChannelAddress> channels) throws OpenemsException {
		var channelAddresses = new ArrayList<String>();
		for (ChannelAddress channel : channels) {
			channelAddresses.add("LAST(\"" + channel.toString() + "\") - FIRST(\"" + channel.toString() + "\") AS \""
					+ channel.toString() + "\"");
		}
		return String.join(", ", channelAddresses);
	}

	protected static String toChannelAddressStringNonNegativeDifferenceLast(Set<ChannelAddress> channels)
			throws OpenemsException {
		var channelAddresses = new ArrayList<String>();
		for (ChannelAddress channel : channels) {
			channelAddresses.add(
					"NON_NEGATIVE_DIFFERENCE(LAST(\"" + channel.toString() + "\")) AS \"" + channel.toString() + "\"");
		}
		return String.join(", ", channelAddresses);
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
					+ StringUtils.toShortString(point.lineProtocol(), 100));
			return;
		}
		try {
			this.executor.execute(() -> {
				this.getConnection().write(point);
			});
		} catch (InfluxDBIOException e) {
			throw new OpenemsException("Unable to write point: " + e.getMessage());
		}
	}
}
