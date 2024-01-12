package io.openems.shared.influxdb;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonElement;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.InfluxDBClientOptions;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WriteConsistency;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.client.write.WriteParameters;
import com.influxdb.exceptions.BadRequestException;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.ThreadPoolUtils;
import io.openems.shared.influxdb.proxy.QueryProxy;
import okhttp3.OkHttpClient;

public class InfluxConnector {

	private static final Pattern NAME_NUMBER_PATTERN = Pattern.compile("[^0-9]+([0-9]+)$");

	private static final int CONNECT_TIMEOUT = 10; // [s]
	private static final int READ_TIMEOUT = 60; // [s]
	private static final int WRITE_TIMEOUT = 10; // [s]

	protected final ThreadPoolExecutor executor;

	private final Logger log = LoggerFactory.getLogger(InfluxConnector.class);

	protected final QueryProxy queryProxy;
	private final URI url;
	private final String org;
	private final String apiKey;
	private final String bucket;
	private final boolean isReadOnly;
	private final boolean safeWrite;

	private final WriteParameters defaultWriteParameters;
	private final Map<WriteParameters, MergePointsWorker> mergePointsWorkerByWriteParameters = new HashMap<>();
	private final AtomicInteger rejectedExecutionCount = new AtomicInteger();

	/**
	 * The Constructor.
	 *
	 * @param componentId   ID of the calling OpenEMS Component
	 * @param queryLanguage A {@link QueryLanguageConfig}
	 * @param url           URL of the InfluxDB-Server (http://ip:port)
	 * @param org           The organisation; '-' for InfluxDB v1
	 * @param apiKey        The apiKey; 'username:password' for InfluxDB v1
	 * @param bucket        The bucket name; 'database/retentionPolicy' for InfluxDB
	 *                      v1
	 * @param tag           The InfluxDB tag
	 * @param isReadOnly    If true, a 'Read-Only-Mode' is activated, where no data
	 *                      is actually written to the database
	 * @param poolSize      the number of threads dedicated to handle the tasks
	 * @param maxQueueSize  queue size limit for executor
	 * @param onWriteError  A consumer for write-errors
	 * @param safeWrite     Adds back points to the queue if a write fails
	 * @param parameters    the {@link WriteParameters} to create a
	 *                      {@link MergePointsWorker} for. All later used
	 *                      {@link WriteParameters} need to be passed here
	 */
	public InfluxConnector(String componentId, QueryLanguageConfig queryLanguage, URI url, String org, String apiKey,
			String bucket, String tag, boolean isReadOnly, int poolSize, int maxQueueSize,
			Consumer<BadRequestException> onWriteError, boolean safeWrite, WriteParameters... parameters) {
		this.queryProxy = QueryProxy.from(queryLanguage, tag);
		this.url = url;
		this.org = org;
		this.apiKey = apiKey;
		this.bucket = bucket;
		this.isReadOnly = isReadOnly;
		this.safeWrite = safeWrite;

		this.executor = new ThreadPoolExecutor(poolSize, poolSize, 0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>(maxQueueSize), //
				new ThreadFactoryBuilder().setNameFormat("InfluxDB-%d").build(), //
				(r, executor) -> {
					// Custom RejectedExecutionHandler; avoid throwing a RejectedExecutionException
					this.rejectedExecutionCount.incrementAndGet();
				});

		BiFunction<String, WriteParameters, MergePointsWorker> mergePointsWorkerFactory;
		if (this.isSafeWrite()) {
			mergePointsWorkerFactory = (name, params) -> new SafeMergePointsWorker(this, name, params, onWriteError);
		} else {
			mergePointsWorkerFactory = (name, params) -> new ForceMergePointsWorker(this, name, params, onWriteError);
		}

		// initialize default merge points worker
		// TODO most of the stuff can be omitted after update
		// https://github.com/influxdata/influxdb-client-java/pull/483
		this.defaultWriteParameters = new WriteParameters(this.bucket, this.org,
				WriteParameters.DEFAULT_WRITE_PRECISION, WriteConsistency.ALL);
		final var defaultMergePointsWorker = mergePointsWorkerFactory.apply("Default", this.defaultWriteParameters);
		defaultMergePointsWorker.activate();
		this.mergePointsWorkerByWriteParameters.put(this.defaultWriteParameters, defaultMergePointsWorker);

		final var defaultOptions = InfluxDBClientOptions.builder() //
				.url(this.url.toString()) //
				.org(this.org) //
				.bucket(this.bucket) //
				.build();
		// initialize merge points worker for specific write parameters
		for (var writeParameters : parameters) {
			final var mergePointsWorker = mergePointsWorkerFactory.apply(writeParameters.bucketSafe(defaultOptions),
					writeParameters);
			mergePointsWorker.activate();
			this.mergePointsWorkerByWriteParameters.put(writeParameters, mergePointsWorker);
		}
	}

	public InfluxConnector(String componentId, QueryLanguageConfig queryLanguage, URI url, String org, String apiKey,
			String bucket, String tag, boolean isReadOnly, int poolSize, int maxQueueSize,
			Consumer<BadRequestException> onWriteError, WriteParameters... parameters) {
		this(componentId, queryLanguage, url, org, apiKey, bucket, tag, isReadOnly, poolSize, maxQueueSize,
				onWriteError, false, parameters);
	}

	public static class InfluxConnection {
		public final InfluxDBClient client;
		public final WriteApiBlocking writeApi;

		public InfluxConnection(InfluxDBClient client, WriteApiBlocking writeApi) {
			this.client = client;
			this.writeApi = writeApi;
		}
	}

	/**
	 * Returns a debug log of the current influx connection state.
	 * 
	 * @return the debug log string
	 */
	public String debugLog() {
		return new StringBuilder("[monitor] ") //
				.append(ThreadPoolUtils.debugLog(this.executor)) //
				.append(", MergePointsWorker[") //
				.append(this.mergePointsWorkerByWriteParameters.values().stream().map(MergePointsWorker::debugLog)
						.collect(Collectors.joining(", ")))
				.append("], Limit:") //
				.append(this.queryProxy.queryLimit) //
				.append(", RejectedExecutions:") //
				.append(this.rejectedExecutionCount.get()) //
				.toString();
	}

	private InfluxConnection influxConnection = null;

	/**
	 * Get InfluxDB Connection.
	 *
	 * @return the {@link InfluxDB} connection
	 */
	protected synchronized InfluxConnection getInfluxConnection() {
		if (this.influxConnection != null) {
			// Use existing Singleton instance
			return this.influxConnection;
		}

		var okHttpClientBuilder = new OkHttpClient().newBuilder() //
				.connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS) //
				.readTimeout(READ_TIMEOUT, TimeUnit.SECONDS) //
				.writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS);

		// copied options from InfluxDBClientFactory.createV1
		// to set timeout
		var options = InfluxDBClientOptions.builder() //
				.url(this.url.toString()) //
				.org(this.org) //
				.bucket(this.bucket) //
				.okHttpClient(okHttpClientBuilder); //
		if (this.apiKey != null && !this.apiKey.isBlank()) {
			options.authenticateToken(String.format(this.apiKey).toCharArray()); //
		}
		var client = InfluxDBClientFactory //
				.create(options.build()) //
				.enableGzip();

		// Keep default WriteOptions from
		// https://github.com/influxdata/influxdb-client-java/tree/master/client#writes
		var writeApi = client.getWriteApiBlocking();

		this.influxConnection = new InfluxConnection(client, writeApi);
		return this.influxConnection;
	}

	/**
	 * Close current {@link InfluxDBClient}.
	 */
	public synchronized void deactivate() {
		if (this.influxConnection != null) {
			this.influxConnection.client.close();
		}
		this.mergePointsWorkerByWriteParameters.values() //
				.forEach(MergePointsWorker::deactivate);
	}

	/**
	 * Queries historic energy.
	 *
	 * @param influxEdgeId the unique, numeric Edge-ID; or Empty to query all Edges
	 * @param fromDate     the From-Date
	 * @param toDate       the To-Date
	 * @param channels     the Channels to query
	 * @param measurement  the measurement
	 * @return a map between ChannelAddress and value
	 * @throws OpenemsException on error
	 */
	public SortedMap<ChannelAddress, JsonElement> queryHistoricEnergy(Optional<Integer> influxEdgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, String measurement)
			throws OpenemsNamedException {
		// handle empty call
		if (channels.isEmpty()) {
			return new TreeMap<>();
		}

		return this.queryProxy.queryHistoricEnergy(this.getInfluxConnection(), this.bucket, measurement, influxEdgeId,
				fromDate, toDate, channels);
	}

	/**
	 * Queries historic energy where only one value per day is saved.
	 * 
	 * @param influxEdgeId the unique, numeric Edge-ID; or Empty to query all Edges
	 * @param fromDate     the From-Date
	 * @param toDate       the To-Date
	 * @param channels     the Channels to query
	 * @param measurement  the measurement
	 * @return a map between ChannelAddress and value
	 * @throws OpenemsException on error
	 */
	public SortedMap<ChannelAddress, JsonElement> queryHistoricEnergySingleValueInDay(//
			Optional<Integer> influxEdgeId, //
			ZonedDateTime fromDate, //
			ZonedDateTime toDate, //
			Set<ChannelAddress> channels, //
			String measurement //
	) throws OpenemsNamedException {
		// handle empty call
		if (channels.isEmpty()) {
			return new TreeMap<>();
		}

		return this.queryProxy.queryHistoricEnergySingleValueInDay(this.getInfluxConnection(), this.bucket, measurement,
				influxEdgeId, fromDate, toDate, channels);
	}

	/**
	 * Queries historic energy per period.
	 *
	 * @param influxEdgeId the unique, numeric Edge-ID; or Empty to query all Edges
	 * @param fromDate     the From-Date
	 * @param toDate       the To-Date
	 * @param channels     the Channels to query
	 * @param resolution   the resolution in seconds
	 * @param measurement  the measurement
	 * @return the historic data as Map
	 * @throws OpenemsException on error
	 */
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricEnergyPerPeriod(
			Optional<Integer> influxEdgeId, ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels,
			Resolution resolution, String measurement) throws OpenemsNamedException {
		// handle empty call
		if (channels.isEmpty()) {
			return new TreeMap<>();
		}

		return this.queryProxy.queryHistoricEnergyPerPeriod(this.getInfluxConnection(), this.bucket, measurement,
				influxEdgeId, fromDate, toDate, channels, resolution);
	}

	/**
	 * Queries the raw historic values without calculating the difference between
	 * two values.
	 * 
	 * @param influxEdgeId the unique, numeric Edge-ID; or Empty to query all Edges
	 * @param fromDate     the From-Date
	 * @param toDate       the To-Date
	 * @param channels     the Channels to query
	 * @param resolution   the resolution in seconds
	 * @param measurement  the measurement
	 * @return the historic data as Map
	 * @throws OpenemsException on error
	 */
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryRawHistoricEnergyPerPeriodSinglePerDay(
			Optional<Integer> influxEdgeId, ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels,
			Resolution resolution, String measurement) throws OpenemsNamedException {
		// handle empty call
		if (channels.isEmpty()) {
			return new TreeMap<>();
		}

		return this.queryProxy.queryRawHistoricEnergyPerPeriodSingleValueInDay(this.getInfluxConnection(), this.bucket,
				measurement, influxEdgeId, fromDate, toDate, channels, resolution);
	}

	/**
	 * Queries historic data.
	 *
	 * @param influxEdgeId the unique, numeric Edge-ID; or Empty to query all Edges
	 * @param fromDate     the From-Date
	 * @param toDate       the To-Date
	 * @param channels     the Channels to query
	 * @param resolution   the resolution in seconds
	 * @param measurement  the measurement
	 * @return the historic data as Map
	 * @throws OpenemsException on error
	 */
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricData(
			Optional<Integer> influxEdgeId, ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels,
			Resolution resolution, String measurement) throws OpenemsNamedException {

		// handle empty call
		if (channels.isEmpty()) {
			return new TreeMap<>();
		}

		return this.queryProxy.queryHistoricData(this.getInfluxConnection(), this.bucket, measurement, influxEdgeId,
				fromDate, toDate, channels, resolution);
	}

	/**
	 * Queries the last value for given channel address from now to 100 days in the
	 * past.
	 * 
	 * @param influxEdgeId   the unique, numeric Edge-ID; or Empty to query all
	 *                       Edges
	 * @param channelAddress the Channels to query
	 * @param measurement    the measurement
	 * @return the values mapped to their channel
	 * @throws OpenemsNamedException on error
	 */
	public SortedMap<ChannelAddress, JsonElement> queryLastData(Optional<Integer> influxEdgeId,
			ChannelAddress channelAddress, String measurement) throws OpenemsNamedException {

		// Check if channelAddress is not null
		if (channelAddress == null) {
			return new TreeMap<>();
		}

		// Create a set of ChannelAdresses thus we need only one
		Set<ChannelAddress> channels = Set.of(channelAddress);

		ZonedDateTime now = ZonedDateTime.now();

		// Use actual timestamp for queryFirstValueBefore-call
		return this.queryProxy.queryFirstValueBefore(//
				this.bucket, //
				this.getInfluxConnection(), //
				measurement, //
				influxEdgeId, //
				now, //
				channels//
		);
	}

	/**
	 * Queries the first valid values before the given date.
	 * 
	 * @param influxEdgeId the unique, numeric Edge-ID; or Empty to query all Edges
	 * @param date         the date
	 * @param channels     the Channels to query
	 * @param measurement  the measurement
	 * @return the values mapped to their channel
	 * @throws OpenemsNamedException on error
	 */
	public SortedMap<ChannelAddress, JsonElement> queryFirstValueBefore(//
			Optional<Integer> influxEdgeId, //
			ZonedDateTime date, //
			Set<ChannelAddress> channels, //
			String measurement //
	) throws OpenemsNamedException {
		if (channels.isEmpty()) {
			return new TreeMap<>();
		}

		return this.queryProxy.queryFirstValueBefore(//
				this.bucket, //
				this.getInfluxConnection(), //
				measurement, //
				influxEdgeId, //
				date, //
				channels//
		);
	}

	/**
	 * Actually write the Point to InfluxDB.
	 *
	 * @param point the InfluxDB Point
	 */
	public void write(Point point) {
		this.write(point, this.defaultWriteParameters);
	}

	/**
	 * Actually write the Point to InfluxDB.
	 * 
	 * @param point           the InfluxDB Point
	 * @param writeParameters the {@link WriteParameters} of the written point. The
	 *                        {@link WriteParameters} had to be passed in the
	 *                        constructor
	 */
	public void write(Point point, WriteParameters writeParameters) {
		if (!point.hasFields()) {
			return;
		}
		if (this.isReadOnly) {
			return;
		}
		final var mergePointsWorker = this.mergePointsWorkerByWriteParameters.get(writeParameters);
		if (mergePointsWorker == null) {
			this.log.info("Unknown write parameters: " + writeParameters);
			return;
		}
		mergePointsWorker.offer(point);
	}

	/**
	 * Gets the edges which already have the available since field set. Mapped from
	 * edgeId to timestamp of availableSince. The timestamp should be in epoch
	 * seconds.
	 * 
	 * @return the map, where the first key is the edge id the second key is the
	 *         channel and the value is the available since timestamp in epoch
	 *         seconds
	 * @throws OpenemsNamedException on error
	 */
	public Map<Integer, Map<String, Long>> queryAvailableSince() throws OpenemsNamedException {
		return this.queryProxy.queryAvailableSince(this.getInfluxConnection(), this.bucket);
	}

	/**
	 * Builds a {@link Point} which set the
	 * {@link QueryProxy.AVAILABLE_SINCE_COLUMN_NAME} field to the new value.
	 * 
	 * @param tag                     the InfluxDB tag
	 * @param influxEdgeId            the id of the edge
	 * @param availableSinceTimestamp the new timestamp in epoch seconds
	 * @param channel                 the channels
	 * @return the {@link Point}
	 */
	public static Point buildUpdateAvailableSincePoint(//
			String tag, //
			int influxEdgeId, //
			String channel, //
			long availableSinceTimestamp //
	) {
		return Point.measurement(QueryProxy.AVAILABLE_SINCE_MEASUREMENT) //
				.addTag(tag, String.valueOf(influxEdgeId)) //
				.addTag(QueryProxy.CHANNEL_TAG, channel) //
				.time(0, WritePrecision.S) //
				.addField(QueryProxy.AVAILABLE_SINCE_COLUMN_NAME, availableSinceTimestamp);
	}

	public boolean isSafeWrite() {
		return this.safeWrite;
	}

	/**
	 * Parses the number of an Edge from its name string.
	 *
	 * <p>
	 * e.g. translates "edge0" to "0".
	 *
	 * @param name the edge name
	 * @return the number
	 * @throws OpenemsException on error
	 */
	public static Integer parseNumberFromName(String name) throws OpenemsException {
		try {
			var matcher = NAME_NUMBER_PATTERN.matcher(name);
			if (matcher.find()) {
				var nameNumberString = matcher.group(1);
				return Integer.parseInt(nameNumberString);
			}
		} catch (NullPointerException e) {
			/* ignore */
		}
		throw new OpenemsException("Unable to parse number from name [" + name + "]");
	}

}
