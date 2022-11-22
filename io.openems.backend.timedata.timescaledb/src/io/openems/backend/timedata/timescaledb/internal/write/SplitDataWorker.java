package io.openems.backend.timedata.timescaledb.internal.write;

import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.TreeBasedTable;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.zaxxer.hikari.HikariDataSource;

import io.openems.backend.timedata.timescaledb.internal.Schema;
import io.openems.backend.timedata.timescaledb.internal.Schema.ChannelRecord;
import io.openems.backend.timedata.timescaledb.internal.write.Point.FloatPoint;
import io.openems.backend.timedata.timescaledb.internal.write.Point.IntPoint;
import io.openems.backend.timedata.timescaledb.internal.write.Point.StringPoint;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.JsonUtils;
import io.openems.common.worker.AbstractImmediateWorker;

/**
 * {@link SplitDataWorker} manages an internal Queue which can be filled via
 * {@link #addData(String, TreeBasedTable)}. The worker then splits the data
 * into typed queues for integer, float and string.
 */
public class SplitDataWorker extends AbstractImmediateWorker {

	private static class WriteData {
		private final String edgeId;
		private final TreeBasedTable<Long, ChannelAddress, JsonElement> table;

		public WriteData(String edgeId, TreeBasedTable<Long, ChannelAddress, JsonElement> table) {
			this.edgeId = edgeId;
			this.table = table;
		}
	}

	private final Logger log = LoggerFactory.getLogger(SplitDataWorker.class);

	private final HikariDataSource dataSource;
	private final ExecutorService executor;
	private final BlockingQueue<WriteData> sourceQueue = new ArrayBlockingQueue<>(
			TimescaledbWriteHandler.POINTS_QUEUE_SIZE);
	private final BlockingQueue<IntPoint> integerPointsQueue;
	private final BlockingQueue<FloatPoint> floatPointsQueue;
	private final BlockingQueue<StringPoint> stringPointsQueue;
	private final Consumer<Schema> onInitializedSchema;

	private Schema schema;

	public SplitDataWorker(HikariDataSource dataSource, //
			ExecutorService executor, //
			BlockingQueue<IntPoint> integerPointsQueue, //
			BlockingQueue<FloatPoint> floatPointsQueue, //
			BlockingQueue<StringPoint> stringPointsQueue, //
			Consumer<Schema> onInitializedSchema) {
		this.dataSource = dataSource;
		this.executor = executor;
		this.integerPointsQueue = integerPointsQueue;
		this.floatPointsQueue = floatPointsQueue;
		this.stringPointsQueue = stringPointsQueue;
		this.onInitializedSchema = onInitializedSchema;
	}

	/**
	 * Adds new 'write' data to the Queue.
	 * 
	 * @param edgeId the Edge-ID
	 * @param table  the data table
	 */
	public void addData(String edgeId, TreeBasedTable<Long, ChannelAddress, JsonElement> table) {
		this.sourceQueue.offer(new WriteData(edgeId, table));
	}

	@Override
	protected void forever() throws SQLException, InterruptedException {
		var schema = this.getOrInitializeSchema();
		if (schema == null) {
			return;
		}

		// Retrieve next element in of Queue; waits till an element is available.
		var data = this.sourceQueue.take();

		for (var cell : data.table.cellSet()) {
			// Cache-Lookup
			var channel = schema.getChannelFromCache(data.edgeId, cell.getColumnKey().getComponentId(),
					cell.getColumnKey().getChannelId());
			if (channel != null) {
				// Channel exists in Cache -> immediately forward to typed queue
				this.addToTypedQueue(channel, cell.getRowKey(), cell.getValue());

			} else {
				// Channel missing in Cache -> async
				this.executor.execute(() -> {
					try (var con = this.dataSource.getConnection()) {
						var channelRecord = schema.getChannel(con, data.edgeId, cell.getColumnKey(), cell.getValue());
						if (channelRecord != null) {
							// Ok -> add to queue
							this.addToTypedQueue(channelRecord, cell.getRowKey(), cell.getValue());
							return;
						}

						if (cell.getValue() != null && cell.getValue() != JsonNull.INSTANCE) {
							// Error and value was not null
							this.log.error("Unable to get ChannelRecord for Channel " //
									+ "[" + data.edgeId + "/" + cell.getColumnKey() + "=" + cell.getValue() + "]");
						}

					} catch (SQLException e) {
						this.log.error("Unable to get ChannelRecord for Channel " //
								+ "[" + data.edgeId + "/" + cell.getColumnKey() + "=" + cell.getValue() + "]: "
								+ e.getMessage());
					}
				});

			}
		}
	}

	/**
	 * Adds the data to the typed queue, ready for writing it to the database.
	 * 
	 * @param channel   the {@link ChannelRecord}e
	 * @param timestamp the timestamp
	 * @param json      the value as {@link JsonElement}
	 */
	private void addToTypedQueue(ChannelRecord channel, long timestamp, JsonElement json) {
		// Convert timestamp to ZonedDateTime
		var time = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC);

		try {
			switch (channel.type) {
			case INTEGER: {
				Long value = JsonUtils.getAsType(OpenemsType.LONG, json);
				if (value == null) {
					return;
				}
				this.integerPointsQueue.offer(new IntPoint(channel.id, time, value));
				return;
			}

			case FLOAT: {
				Double value = JsonUtils.getAsType(OpenemsType.DOUBLE, json);
				if (value == null) {
					return;
				}
				this.floatPointsQueue.offer(new FloatPoint(channel.id, time, value));
				return;
			}

			case STRING: {
				String value = JsonUtils.getAsType(OpenemsType.STRING, json);
				if (value == null) {
					return;
				}
				this.stringPointsQueue.offer(new StringPoint(channel.id, time, value));
				return;
			}
			}
		} catch (OpenemsNamedException e) {
			this.log.warn("Unable to parse [" + json + "] to [" + channel.type + "] for Channel-ID [" + channel.id
					+ "]: " + e.getMessage());
		}
	}

	/**
	 * Gets the initialized Schema; or null on error.
	 * 
	 * <p>
	 * If Schema can be initialized, the onInitializedSchema is also set.
	 * 
	 * @return {@link Schema} or null
	 */
	private synchronized Schema getOrInitializeSchema() {
		var schema = this.schema;
		if (schema != null) {
			return schema;
		}

		try {
			schema = Schema.initialize(this.dataSource);
			this.schema = schema;
			this.onInitializedSchema.accept(schema);
			return schema;

		} catch (SQLException e) {
			this.log.error("Unable to cache Schema: " + e.getMessage());

			try {
				Thread.sleep(10000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			return null;
		}
	}

	/**
	 * Returns a DebugLog String.
	 * 
	 * @return debug log
	 */
	public String debugLog() {
		return new StringBuilder() //
				.append(this.sourceQueue.size()) //
				.append("/") //
				.append(TimescaledbWriteHandler.POINTS_QUEUE_SIZE) //
				.toString();
	}

}
