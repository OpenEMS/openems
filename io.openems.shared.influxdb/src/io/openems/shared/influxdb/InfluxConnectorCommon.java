package io.openems.shared.influxdb;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.InfluxDBClientOptions;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.write.Point;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.StringUtils;
import io.openems.common.utils.ThreadPoolUtils;
import okhttp3.OkHttpClient;

public class InfluxConnectorCommon {

	private static final int CONNECT_TIMEOUT = 10; // [s]
	private static final int READ_TIMEOUT = 60; // [s]
	private static final int WRITE_TIMEOUT = 10; // [s]

	private static final int EXECUTOR_MIN_THREADS = 10;
	private static final int EXECUTOR_MAX_THREADS = 50;
	private static final int EXECUTOR_QUEUE_SIZE = 500;
	private static final int POINTS_QUEUE_SIZE = 1_000_000;
	private static final int MAX_POINTS_PER_WRITE = 1_000;
	private static final int MAX_AGGREGATE_WAIT = 10; // [s]

	protected final Logger log = LoggerFactory.getLogger(InfluxConnector.class);

	private final URI url;
	private final String org;
	private final String apiKey;
	protected final String bucket;
	private final boolean isReadOnly;
	private final Consumer<Throwable> onWriteError;
	private final ThreadPoolExecutor executor = new ThreadPoolExecutor(EXECUTOR_MIN_THREADS, EXECUTOR_MAX_THREADS, 60L,
			TimeUnit.SECONDS, //
			new ArrayBlockingQueue<>(EXECUTOR_QUEUE_SIZE), //
			new ThreadFactoryBuilder().setNameFormat("InfluxConnector-%d").build(), //
			new ThreadPoolExecutor.DiscardOldestPolicy());
	private final ScheduledExecutorService debugLogExecutor = Executors.newSingleThreadScheduledExecutor();
	private final ExecutorService mergePointsExecutor = Executors.newSingleThreadExecutor();
	private final BlockingQueue<Point> pointsQueue = new LinkedBlockingQueue<>(POINTS_QUEUE_SIZE);

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
	protected InfluxConnectorCommon(URI url, String org, String apiKey, String bucket, boolean isReadOnly,
			Consumer<Throwable> onWriteError) {
		this.url = url;
		this.org = org;
		this.apiKey = apiKey;
		this.bucket = bucket;
		this.isReadOnly = isReadOnly;
		this.onWriteError = onWriteError;

		this.debugLogExecutor.scheduleWithFixedDelay(() -> {
			int pointsQueueSize = this.pointsQueue.size();
			this.log.info(new StringBuilder("[InfluxDB] [monitor] ") //
					.append(ThreadPoolUtils.debugLog(this.executor)) //
					.append(" Queue:") //
					.append(pointsQueueSize) //
					.append("/") //
					.append(POINTS_QUEUE_SIZE) //
					.append((pointsQueueSize == POINTS_QUEUE_SIZE) ? " !!!POINTS BACKPRESSURE!!!" : "") //
					.toString());
		}, 10, 10, TimeUnit.SECONDS);

		this.mergePointsExecutor.execute(() -> {
			/**
			 * This task merges single Points to Lists of Points, which are then sent to
			 * InfluxDB. This approach improves speed as not every single Point gets sent
			 * via HTTP individually.
			 * 
			 * In theory the async implementation in the InfluxDB library would work also,
			 * but it fails in production (without providing any error message/exception).
			 */
			while (true) {
				try {
					/*
					 * Merge Points. Wait max 10 seconds in total.
					 */
					final Instant maxWait = Instant.now().plusSeconds(MAX_AGGREGATE_WAIT);
					List<Point> points = new ArrayList<>(MAX_POINTS_PER_WRITE);
					for (int i = 0; i < MAX_POINTS_PER_WRITE; i++) {
						var point = this.pointsQueue.poll(MAX_AGGREGATE_WAIT, TimeUnit.SECONDS);
						if (point == null) {
							break;
						}
						points.add(point);
						if (Instant.now().isAfter(maxWait)) {
							break;
						}
					}
					/*
					 * Write points async.
					 */
					if (!points.isEmpty()) {
						this.executor.execute(() -> {
							try {
								this.getInfluxConnection().writeApi.writePoints(points);
							} catch (Throwable t) {
								this.log.warn("Unable to write points. " + t.getMessage());
								this.onWriteError.accept(t);
							}
						});
					}

				} catch (InterruptedException e) {
					this.log.info("MergePointsExecutor was interrupted");
					break;

				} catch (Throwable e) {
					this.log.error("Unhandled Error in 'MergePointsExecutor': " + e.getClass().getName() + ". "
							+ e.getMessage());
					e.printStackTrace();
				}
			}
		});
	}

	protected static class InfluxConnection {
		protected final InfluxDBClient client;
		protected final WriteApiBlocking writeApi;

		public InfluxConnection(InfluxDBClient client, WriteApiBlocking writeApi) {
			this.client = client;
			this.writeApi = writeApi;
		}
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
		ThreadPoolUtils.shutdownAndAwaitTermination(this.executor, 0);
		ThreadPoolUtils.shutdownAndAwaitTermination(this.mergePointsExecutor, 0);
		ThreadPoolUtils.shutdownAndAwaitTermination(this.debugLogExecutor, 0);
		if (this.influxConnection != null) {
			this.influxConnection.client.close();
		}
	}

	public static class RandomLimit {
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

	protected final RandomLimit queryLimit = new RandomLimit();

	/**
	 * Actually write the Point to InfluxDB.
	 *
	 * @param point the InfluxDB Point
	 * @throws OpenemsException on error
	 */
	public void write(Point point) {
		if (this.isReadOnly) {
			this.log.info("Read-Only-Mode is activated. Not writing points: "
					+ StringUtils.toShortString(point.toLineProtocol(), 100));
			return;
		}
		this.pointsQueue.offer(point);
	}

}
