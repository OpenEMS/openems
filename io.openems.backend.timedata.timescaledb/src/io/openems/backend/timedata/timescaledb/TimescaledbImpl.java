package io.openems.backend.timedata.timescaledb;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.postgresql.Driver;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.TreeBasedTable;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonElement;
import com.zaxxer.hikari.HikariDataSource;

import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.backend.common.metadata.Metadata;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = "Timedata.TimescaleDB", //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class TimescaledbImpl extends AbstractOpenemsBackendComponent
		implements Timescaledb /* TODO implements Timedata */ {

	private static final int EXECUTOR_MIN_THREADS = 10;
	private static final int EXECUTOR_MAX_THREADS = 50;
	private static final int EXECUTOR_QUEUE_SIZE = 500;
	private static final int POINTS_QUEUE_SIZE = 1_000_000;
	private static final int MAX_POINTS_PER_WRITE = 1_000;
	private static final int MAX_AGGREGATE_WAIT = 10; // [s]

	private final Logger log = LoggerFactory.getLogger(TimescaledbImpl.class);
	private final HikariDataSource dataSource;
	private final Schema schema;
	private final BlockingQueue<Point> pointsQueue = new ArrayBlockingQueue<>(POINTS_QUEUE_SIZE);
	private final ThreadPoolExecutor executor = new ThreadPoolExecutor(EXECUTOR_MIN_THREADS, EXECUTOR_MAX_THREADS, 60L,
			TimeUnit.SECONDS, //
			new ArrayBlockingQueue<>(EXECUTOR_QUEUE_SIZE), //
			new ThreadFactoryBuilder().setNameFormat("Timescaledb-%d").build(), //
			new ThreadPoolExecutor.DiscardOldestPolicy());
	private final ScheduledExecutorService debugLogExecutor = Executors.newSingleThreadScheduledExecutor();
	private final ExecutorService mergePointsExecutor = Executors.newSingleThreadExecutor();

	@Activate
	public TimescaledbImpl(@Reference Metadata metadata, Config config) throws SQLException {
		super("Timedata.TimescaleDB");

		this.logInfo(this.log, "Activate [" + config.user() + (config.password() != null ? ":xxx" : "") + "@"
				+ config.host() + ":" + config.port() + "/" + config.database() + "]");

		this.dataSource = getDataSource(config.host(), config.port(), config.database(), config.user(),
				config.password());

		this.schema = Schema.initialize(this.dataSource);

		this.debugLogExecutor.scheduleWithFixedDelay(this.debugLogTask, 10, 10, TimeUnit.SECONDS);
		this.mergePointsExecutor.execute(() -> {
			/**
			 * This task merges single Points to Lists of Points, which are then sent to
			 * TimescaleDB. This approach improves speed as not every single Point gets sent
			 * via HTTP individually.
			 */
			while (true) {
				try {
					// Poll and merge Points. Wait max 10 seconds in total.
					var points = pollAndMergePoints(this.pointsQueue);

					if (points.isEmpty()) {
						continue;
					}

					// Write points async.
					this.executor.execute(() -> {
						var psts = new EnumMap<Type, PreparedStatement>(Type.class);
						try (var con = this.dataSource.getConnection()) {
							// Prepare a PreparedStatement for every type
							for (var type : Type.values()) {
								psts.put(type, type.prepareStatement(con));
							}
							// Add data from points to PreparedStatements
							for (var point : points) {
								var channel = this.schema.getChannel(point, con);
								if (channel == null) {
									continue;
								}
								var pst = psts.get(channel.type);
								channel.type.fillStatement(pst, point, channel);
								pst.addBatch();
							}
							// Execute all Batches
							for (var pst : psts.values()) {
								pst.executeBatch();
							}

						} catch (Exception e) {
							this.log.error("Unable to write Points: " + e.getMessage());

						} finally {
							// Close PreparedStatements (Connection is autoclosed)
							for (var pst : psts.values()) {
								try {
									pst.close();
								} catch (SQLException e) {
									this.logWarn(this.log, "Unable to close PreparedStatement: " + e.getMessage());
								}
							}
						}
					});

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

	@Deactivate
	private void deactivate() {
		this.logInfo(this.log, "Deactivate");
		this.dataSource.close();
	}

	// TODO @Override from Timedata
	public void write(String edgeId, TreeBasedTable<Long, ChannelAddress, JsonElement> data) throws OpenemsException {
		this.pointsQueue.addAll(data.cellSet().stream() //
				.map(cell -> new Point(cell.getRowKey(), edgeId, cell.getColumnKey(), cell.getValue())) //
				.collect(Collectors.toList()));
	}

	/**
	 * Creates a {@link HikariDataSource} connection pool.
	 *
	 * @param host     the database hostname
	 * @param port     the database port
	 * @param database the database name
	 * @param user     the database user
	 * @param password the database password
	 * @return the HikariDataSource
	 * @throws SQLException on error
	 */
	private static HikariDataSource getDataSource(String host, int port, String database, String user, String password)
			throws SQLException {
		if (!Driver.isRegistered()) {
			Driver.register();
		}
		var pgds = new PGSimpleDataSource();
		pgds.setServerNames(new String[] { host });
		pgds.setPortNumbers(new int[] { port });
		pgds.setDatabaseName(database);
		pgds.setUser(user);
		pgds.setPassword(password);
		var result = new HikariDataSource();
		result.setDataSource(pgds);
		return result;
	}

	/**
	 * Poll and merge Points. Wait max 10 seconds in total.
	 * 
	 * @param pointsQueue the Queue of Points
	 * @return a list of Points
	 * @throws InterruptedException on error
	 */
	private static List<Point> pollAndMergePoints(BlockingQueue<Point> pointsQueue) throws InterruptedException {
		final Instant maxWait = Instant.now().plusSeconds(MAX_AGGREGATE_WAIT);
		List<Point> points = new ArrayList<>(MAX_POINTS_PER_WRITE);
		for (int i = 0; i < MAX_POINTS_PER_WRITE; i++) {
			var point = pointsQueue.poll(MAX_AGGREGATE_WAIT, TimeUnit.SECONDS);
			if (point == null) {
				break;
			}
			points.add(point);
			if (Instant.now().isAfter(maxWait)) {
				break;
			}
		}
		return points;
	}

	private final Runnable debugLogTask = () -> {
		int executorQueueSize = this.executor.getQueue().size();
		int pointsQueueSize = this.pointsQueue.size();
		this.log.info(new StringBuilder("[monitor] TimescaleDB ") //
				.append("Pool: ").append(this.executor.getPoolSize()).append(", ") //
				.append("Active: ").append(this.executor.getActiveCount()).append(", ") //
				.append("Pending: ").append(this.executor.getQueue().size()).append(", ") //
				.append("Completed: ").append(this.executor.getCompletedTaskCount()).append(", ") //
				.append((executorQueueSize == EXECUTOR_QUEUE_SIZE) ? "!!!EXECUTOR BACKPRESSURE!!!" : "") //
				.append("QueuedPoints: ").append(this.pointsQueue.size()).append(", ") //
				.append((pointsQueueSize == POINTS_QUEUE_SIZE) ? "!!!POINTS BACKPRESSURE!!!" : "") //
				.toString());
	};
}
