package io.openems.backend.timedata.timescaledb.internal.write;

import java.sql.SQLException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.TreeBasedTable;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonElement;
import com.zaxxer.hikari.HikariDataSource;

import io.openems.backend.common.timedata.Timedata;
import io.openems.backend.timedata.timescaledb.Config;
import io.openems.backend.timedata.timescaledb.internal.Schema;
import io.openems.backend.timedata.timescaledb.internal.Type;
import io.openems.backend.timedata.timescaledb.internal.Utils;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.StringUtils;
import io.openems.common.utils.ThreadPoolUtils;
import io.openems.common.worker.AbstractWorker;

public class TimescaledbWriteHandler {

	public static final int POINTS_QUEUE_SIZE = 10_000;
	public static final int MAX_POINTS_PER_WRITE = 10_000;
	public static final int MAX_AGGREGATE_WAIT = 60; // [s]

	private final Logger log = LoggerFactory.getLogger(TimescaledbWriteHandler.class);

	/**
	 * A {@link HikariDataSource} used solely for writes.
	 */
	private final HikariDataSource dataSource;

	/**
	 * A {@link Executor} used solely for writes.
	 */
	private final ThreadPoolExecutor executor;

	private final boolean isReadOnly;

	// #1 step: split data to points
	private final SplitDataWorker splitPointsWorker;

	// #2 step: split points to typed queues
	private final EnumMap<Type, QueueHandler<?>> queueHandlers;

	public TimescaledbWriteHandler(Config config, Set<String> enableWriteChannelAddresses,
			Consumer<Schema> onInitializedSchema) throws SQLException {
		this.isReadOnly = config.isReadOnly();

		this.dataSource = Utils.getDataSource(//
				config.host(), config.port(), config.database(), //
				config.user(), config.password(), config.poolSize() + 1 /* one more than write threads */);

		this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(config.poolSize(),
				new ThreadFactoryBuilder().setNameFormat("TimescaleDB-%d").build());

		// Prepare typed merge points workers
		this.queueHandlers = new EnumMap<>(Type.class);
		for (var type : Type.values()) {
			this.queueHandlers.put(type, QueueHandler.of(type, this.dataSource, this.executor));
		}

		// Split incoming data to Points and add to typed queues
		this.splitPointsWorker = new SplitDataWorker(//
				this.dataSource, //
				this.executor, //
				this.queueHandlers, //
				enableWriteChannelAddresses, //
				(schema) -> {
					// only after Schema is initialized -> start all dependent workers
					onInitializedSchema.accept(schema);
					this.streamHandlers().forEach(h -> h.activate());
				});
		this.splitPointsWorker.activate("TimescaleDB-SplitPoints");
	}

	private final Stream<QueueHandler<?>> streamHandlers() {
		return this.queueHandlers.values().stream(); //
	}

	/**
	 * Called by TimescaledbImpl deactivate().
	 */
	public void deactivate() {
		ThreadPoolUtils.shutdownAndAwaitTermination(this.executor, 0);
		this.splitPointsWorker.deactivate();
		this.streamHandlers() //
				.map(QueueHandler::getMergePointsWorker) //
				.forEach(AbstractWorker::deactivate);
		if (this.dataSource != null) {
			this.dataSource.close();
		}
	}

	/**
	 * See {@link Timedata#write(String, TreeBasedTable)}.
	 *
	 * @param edgeId The unique Edge-ID
	 * @param data   Table of timestamp (epoch in milliseconds), Channel-Address and
	 *               the Channel value as JsonElement. Sorted by timestamp.
	 * @throws OpenemsException on error
	 */
	public void write(String edgeId, TreeBasedTable<Long, String, JsonElement> data) {
		if (this.isReadOnly) {
			this.log.info("Read-Only-Mode is activated. Not writing points: "
					+ StringUtils.toShortString(data.toString(), 100));
			return;
		}

		this.splitPointsWorker.addData(edgeId, data);
	}

	/**
	 * Returns a DebugLog String.
	 * 
	 * @return debug log
	 */
	public StringBuilder debugLog() {
		var sb = new StringBuilder() //
				.append(ThreadPoolUtils.debugLog(this.executor)) //
				.append(" SPLIT:").append(this.splitPointsWorker.debugLog());
		this.streamHandlers().forEach((t) -> {
			sb.append(" ").append(t.debugLog());
		});
		return sb;
	}

	/**
	 * Returns a DebugMetrics map.
	 * 
	 * @return metrics
	 */
	public Map<String, Number> debugMetrics() {
		return ThreadPoolUtils.debugMetrics(this.executor);
	}
}
