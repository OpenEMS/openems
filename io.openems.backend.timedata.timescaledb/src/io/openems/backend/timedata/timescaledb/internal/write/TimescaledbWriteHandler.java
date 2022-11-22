package io.openems.backend.timedata.timescaledb.internal.write;

import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;

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
import io.openems.backend.timedata.timescaledb.internal.write.Point.FloatPoint;
import io.openems.backend.timedata.timescaledb.internal.write.Point.IntPoint;
import io.openems.backend.timedata.timescaledb.internal.write.Point.StringPoint;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.StringUtils;
import io.openems.common.utils.ThreadPoolUtils;

public class TimescaledbWriteHandler {

	public static final int POINTS_QUEUE_SIZE = 1_000_000;
	public static final int MAX_POINTS_PER_WRITE = 10_000;
	public static final int MAX_AGGREGATE_WAIT = 10; // [s]

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
	private final MergePointsWorker<IntPoint> mergeIntegerPointsWorker;
	private final MergePointsWorker<FloatPoint> mergeFloatPointsWorker;
	private final MergePointsWorker<StringPoint> mergeStringPointsWorker;

	public TimescaledbWriteHandler(Config config, Consumer<Schema> onInitializedSchema) throws SQLException {
		this.isReadOnly = config.isReadOnly();

		this.dataSource = Utils.getDataSource(//
				config.host(), config.port(), config.database(), //
				config.user(), config.password(), config.poolSize());

		this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(config.poolSize(),
				new ThreadFactoryBuilder().setNameFormat("TimescaleDB-%d").build());

		// Prepare typed merge points workers
		this.mergeIntegerPointsWorker = new MergePointsWorker<IntPoint>(this.dataSource, this.executor, Type.INTEGER);
		this.mergeFloatPointsWorker = new MergePointsWorker<FloatPoint>(this.dataSource, this.executor, Type.FLOAT);
		this.mergeStringPointsWorker = new MergePointsWorker<StringPoint>(this.dataSource, this.executor, Type.STRING);

		// Split incoming data to Points and add to typed queues
		this.splitPointsWorker = new SplitDataWorker(//
				this.dataSource, //
				this.executor, //
				this.mergeIntegerPointsWorker.getQueue(), //
				this.mergeFloatPointsWorker.getQueue(), //
				this.mergeStringPointsWorker.getQueue(), //
				(schema) -> {
					// only after Schema is initialized -> start all dependent workers
					onInitializedSchema.accept(schema);
					this.mergeIntegerPointsWorker.activate("TimescaleDB-MergeIntegerPoints");
					this.mergeFloatPointsWorker.activate("TimescaleDB-MergeFloatPoints");
					this.mergeStringPointsWorker.activate("TimescaleDB-MergeStringPoints");
				});
		this.splitPointsWorker.activate("TimescaleDB-SplitPoints");
	}

	/**
	 * Called by TimescaledbImpl deactivate().
	 */
	public void deactivate() {
		ThreadPoolUtils.shutdownAndAwaitTermination(this.executor, 0);
		this.splitPointsWorker.deactivate();
		this.mergeIntegerPointsWorker.deactivate();
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
	public void write(String edgeId, TreeBasedTable<Long, ChannelAddress, JsonElement> data) {
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
		return new StringBuilder() //
				.append(ThreadPoolUtils.debugLog(this.executor)) //
				.append(" SPLIT:").append(this.splitPointsWorker.debugLog()) //
				.append(" INTEGER:").append(this.mergeIntegerPointsWorker.debugLog()) //
				.append(" FLOAT:").append(this.mergeFloatPointsWorker.debugLog()) //
				.append(" STRING:").append(this.mergeStringPointsWorker.debugLog()); //
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
