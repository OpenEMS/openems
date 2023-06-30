package io.openems.edge.timedata.rrd4j;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.ToDoubleFunction;

import org.rrd4j.core.RrdDb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.common.worker.AbstractImmediateWorker;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public class RecordWorker extends AbstractImmediateWorker {

	protected static final int DEFAULT_NO_OF_CYCLES = 60;

	private final Logger log = LoggerFactory.getLogger(RecordWorker.class);
	private final TimedataRrd4jImpl parent;

	// Counts the number of Cycles till data is recorded
	private int cycleCount = 0;

	private static class Record {
		private final long timestamp;
		private final ChannelAddress address;
		private final Unit unit;
		private final double value;

		public Record(long timestamp, ChannelAddress address, Unit unit, double value) {
			this.timestamp = timestamp;
			this.address = address;
			this.unit = unit;
			this.value = value;
		}
	}

	// Record queue
	private final LinkedBlockingQueue<Record> records = new LinkedBlockingQueue<>();

	// keeps the last recorded timestamp
	private Instant lastTimestamp = Instant.MIN;
	private LocalDateTime readChannelValuesSince = LocalDateTime.MIN;

	public RecordWorker(TimedataRrd4jImpl parent) {
		this.parent = parent;
	}

	/**
	 * Collects the data from Channels. This is called synchronously by the main
	 * OpenEMS cycle. On finish it triggers a next async task to write the data to
	 * RRD4J.
	 */
	public void collectData() {
		var timestamp = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		final var nextReadChannelValuesSince = LocalDateTime.now();

		// Increase CycleCount
		this.cycleCount += 1;

		// Same second as last run? -> RRD4j can only handle one sample per second per
		// database. Timestamps are all stored "truncated to seconds".
		if (timestamp.equals(this.lastTimestamp)) {
			return;
		}

		if (
		// No need to persist data, as it is still stored by the Channel itself. The
		// Channel keeps the last NO_OF_PAST_VALUES values
		this.cycleCount < Channel.NO_OF_PAST_VALUES
				// RRD4j requires us to write one value per DEFAULT_HEARTBEAT_SECONDS
				&& Duration.between(this.lastTimestamp, timestamp)
						.getSeconds() < TimedataRrd4jImpl.DEFAULT_HEARTBEAT_SECONDS - 1) {
			return;
		}
		this.cycleCount = 0; // Reset Cycle-Count

		this.lastTimestamp = timestamp;

		for (OpenemsComponent component : this.parent.componentManager.getEnabledComponents()) {
			for (Channel<?> channel : component.channels()) {
				var doc = channel.channelDoc();
				if (// Ignore Low-Priority Channels
				doc.getPersistencePriority().isLowerThan(this.parent.persistencePriority)
						// Ignore WRITE_ONLY Channels
						|| channel.channelDoc().getAccessMode() == AccessMode.WRITE_ONLY) {
					continue;
				}

				ToDoubleFunction<? super Object> channelMapFunction = this
						.getChannelMapFunction(channel.channelDoc().getType());
				var channelAggregateFunction = channel.channelDoc().getUnit().getChannelAggregateFunction();

				var value = channelAggregateFunction.apply(//
						channel.getPastValues() //
								.tailMap(this.readChannelValuesSince, false) // new values since last recording
								.values().stream() //
								.map(Value::get) //
								.filter(v -> v != null) // only not-null values
								.mapToDouble(channelMapFunction) // convert to double
				);
				if (!value.isPresent()) {
					// only available channels
					continue;
				}

				if (this.records.offer(//
						new Record(timestamp.getEpochSecond(), channel.address(), channel.channelDoc().getUnit(),
								value.getAsDouble()))) {
					this.parent._setQueueIsFull(false);

				} else {
					this.parent.logWarn(this.log, "Unable to add record [" + channel.address() + "]. Queue is full!");
					this.parent._setQueueIsFull(true);
				}
			}
		}

		this.readChannelValuesSince = nextReadChannelValuesSince;
	}

	@Override
	protected void forever() throws InterruptedException {
		var record = this.records.take();
		RrdDb database = null;

		try {
			database = this.parent.getRrdDb(record.address, record.unit, record.timestamp - 1);

			if (database.getLastUpdateTime() < record.timestamp) {
				// Avoid and silently ignore error "IllegalArgumentException: Bad sample time:
				// YYY. Last update time was ZZZ, at least one second step is required".

				// Add Sample to RRD4J
				var sample = database.createSample(record.timestamp);
				sample.setValue(0, record.value);
				sample.update();
			}

			this.parent._setUnableToInsertSample(false);

		} catch (Throwable e) {
			this.parent._setUnableToInsertSample(true);
			this.parent.logWarn(this.log, "Unable to insert Sample [" + record.address + "] "
					+ e.getClass().getSimpleName() + ": " + e.getMessage());
		} finally {
			if (database != null) {
				try {
					database.close();
				} catch (IOException e) {
					this.parent.logWarn(this.log,
							"Unable to close database [" + record.address + "]: " + e.getMessage());
				}
			}
		}
	}

	private static final ToDoubleFunction<? super Object> MAP_BOOLEAN_TO_DOUBLE = value -> ((Boolean) value ? 1d : 0d);

	private static final ToDoubleFunction<? super Object> MAP_SHORT_TO_DOUBLE = value -> ((Short) value).doubleValue();
	private static final ToDoubleFunction<? super Object> MAP_INTEGER_TO_DOUBLE = value -> ((Integer) value)
			.doubleValue();
	private static final ToDoubleFunction<? super Object> MAP_LONG_TO_DOUBLE = value -> ((Long) value).doubleValue();
	private static final ToDoubleFunction<? super Object> MAP_FLOAT_TO_DOUBLE = value -> ((Float) value).doubleValue();
	private static final ToDoubleFunction<? super Object> MAP_DOUBLE_TO_DOUBLE = value -> ((Double) value);
	private static final ToDoubleFunction<? super Object> MAP_TO_DOUBLE_NOT_SUPPORTED = value -> 0d;

	private ToDoubleFunction<? super Object> getChannelMapFunction(OpenemsType openemsType) {
		switch (openemsType) {
		case BOOLEAN:
			return MAP_BOOLEAN_TO_DOUBLE;
		case SHORT:
			return MAP_SHORT_TO_DOUBLE;
		case INTEGER:
			return MAP_INTEGER_TO_DOUBLE;
		case LONG:
			return MAP_LONG_TO_DOUBLE;
		case FLOAT:
			return MAP_FLOAT_TO_DOUBLE;
		case DOUBLE:
			return MAP_DOUBLE_TO_DOUBLE;
		case STRING:
			// Strings are not supported by RRD4J
			return MAP_TO_DOUBLE_NOT_SUPPORTED;
		}
		throw new IllegalArgumentException("Type [" + openemsType + "] is not supported.");
	}

}
