package io.openems.backend.timedata.timescaledb.internal.write;

import java.sql.SQLException;
import java.util.EnumMap;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.TreeBasedTable;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.zaxxer.hikari.HikariDataSource;

import io.openems.backend.timedata.timescaledb.internal.Schema;
import io.openems.backend.timedata.timescaledb.internal.Schema.ChannelRecord;
import io.openems.backend.timedata.timescaledb.internal.Type;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.worker.AbstractImmediateWorker;

/**
 * {@link SplitDataWorker} manages an internal Queue which can be filled via
 * {@link #addData(String, TreeBasedTable)}. The worker then splits the data
 * into typed queues for integer, float and string.
 */
public class SplitDataWorker extends AbstractImmediateWorker {

	private static class WriteData {
		private final String edgeId;
		private final TreeBasedTable<Long, String, JsonElement> table;

		public WriteData(String edgeId, TreeBasedTable<Long, String, JsonElement> table) {
			this.edgeId = edgeId;
			this.table = table;
		}
	}

	private final Logger log = LoggerFactory.getLogger(SplitDataWorker.class);

	private final HikariDataSource dataSource;
	private final ExecutorService executor;
	private final BlockingQueue<WriteData> sourceQueue = new LinkedBlockingQueue<>(
			TimescaledbWriteHandler.POINTS_QUEUE_SIZE);
	private final EnumMap<Type, QueueHandler<?>> queueHandlers;
	private final Consumer<Schema> onInitializedSchema;
	private final Set<String> enableWriteChannelAddresses;

	private Schema schema;

	public SplitDataWorker(HikariDataSource dataSource, //
			ExecutorService executor, //
			EnumMap<Type, QueueHandler<?>> queueHandlers, //
			Set<String> enableWriteChannelAddresses, //
			Consumer<Schema> onInitializedSchema) {
		this.dataSource = dataSource;
		this.executor = executor;
		this.queueHandlers = queueHandlers;
		this.enableWriteChannelAddresses = enableWriteChannelAddresses;
		this.onInitializedSchema = onInitializedSchema;
	}

	/**
	 * Adds new 'write' data to the Queue.
	 * 
	 * @param edgeId the Edge-ID
	 * @param table  the data table
	 */
	public void addData(String edgeId, TreeBasedTable<Long, String, JsonElement> table) {
		this.sourceQueue.offer(new WriteData(edgeId, table));
	}

	@Override
	protected void forever() throws SQLException, InterruptedException {
		var schema = this.getOrInitializeSchema();
		if (schema == null) {
			return;
		}

		if (this.sourceQueue.isEmpty()) {
			return;
		}

		// Retrieve next element in of Queue; waits till an element is available.
		var data = this.sourceQueue.take();

		for (var cell : data.table.cellSet()) {
			final var channelAddress = cell.getColumnKey();
			// Write this Channel?
			if (!this.enableWriteChannelAddresses.contains(channelAddress)) {
				continue;
			}

			final var value = cell.getValue();
			final var timestamp = cell.getRowKey();

			// Cache-Lookup
			var channel = schema.getChannelFromCache(data.edgeId, channelAddress);
			if (channel != null) {
				// Channel exists in Cache -> immediately forward to typed queue
				this.addToTypedQueue(channel, timestamp, cell.getValue());

			} else {
				// Channel missing in Cache -> async
				this.executor.execute(() -> {
					try (var con = this.dataSource.getConnection()) {
						var channelRecord = schema.getChannel(con, data.edgeId, channelAddress, value);
						if (channelRecord != null) {
							// Ok -> add to queue
							this.addToTypedQueue(channelRecord, timestamp, cell.getValue());
							return;
						}

						if (cell.getValue() != null && cell.getValue() != JsonNull.INSTANCE) {
							// Error and value was not null
							this.log.error("Unable to get ChannelRecord for Channel " //
									+ "[" + data.edgeId + "/" + channelAddress + "=" + cell.getValue() + "]");
						}

					} catch (SQLException e) {
						this.log.error("Unable to get ChannelRecord for Channel " //
								+ "[" + data.edgeId + "/" + channelAddress + "=" + cell.getValue() + "]: "
								+ e.getMessage());
					} catch (Throwable e) {
						this.log.error("Unable to get ChannelRecord for Channel " //
								+ "[" + data.edgeId + "/" + channelAddress + "=" + cell.getValue() + "]: "
								+ e.getMessage());
						e.printStackTrace();
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
		try {
			this.queueHandlers.get(channel.type).offer(channel, timestamp, json);
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
			e.printStackTrace();
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
