package io.openems.edge.timedata.rrd4j;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.OptionalDouble;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.stream.DoubleStream;

import org.rrd4j.core.RrdDb;
import org.rrd4j.core.Sample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.common.worker.AbstractImmediateWorker;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.OpenemsComponent;

public class RecordWorker extends AbstractImmediateWorker {

	protected static final int DEFAULT_NO_OF_CYCLES = 60;

	private final Logger log = LoggerFactory.getLogger(RecordWorker.class);
	private final Rrd4jTimedataImpl parent;
	protected int noOfCycles = DEFAULT_NO_OF_CYCLES; // default, is going to be overwritten by config

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
	private LinkedBlockingQueue<Record> records = new LinkedBlockingQueue<>();

	// keeps the last recorded timestamp
	private Instant lastTimestamp = Instant.MIN;
	private LocalDateTime readChannelValuesSince = LocalDateTime.MIN;

	public RecordWorker(Rrd4jTimedataImpl parent) {
		this.parent = parent;
	}

	/**
	 * Collects the data from Channels. This is called synchronously by the main
	 * OpenEMS cycle. On finish it triggers a next async task to write the data to
	 * RRD4J.
	 */
	public void collectData() {
		Instant timestamp = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		final LocalDateTime nextReadChannelValuesSince = LocalDateTime.now();

		// Increase CycleCount
		if (++this.cycleCount < this.noOfCycles) {
			// Stop here if not reached CycleCount
			return;
		}
		// reset Cycle-Count
		this.cycleCount = 0;

		// Same second as last run? -> RRD4j can only handle one sample per second per
		// database. Timestamps are all stored "truncated to seconds".
		if (timestamp.equals(this.lastTimestamp)) {
			return;
		}
		this.lastTimestamp = timestamp;

		for (OpenemsComponent component : this.parent.componentManager.getEnabledComponents()) {
			for (Channel<?> channel : component.channels()) {
				if (channel.channelDoc().getAccessMode() != AccessMode.READ_ONLY
						&& channel.channelDoc().getAccessMode() != AccessMode.READ_WRITE) {
					// Ignore WRITE_ONLY Channels
					continue;
				}

				ToDoubleFunction<? super Object> channelMapFunction = this
						.getChannelMapFunction(channel.channelDoc().getType());
				Function<DoubleStream, OptionalDouble> channelAggregateFunction = this
						.getChannelAggregateFunction(channel.channelDoc().getUnit());

				OptionalDouble value = channelAggregateFunction.apply(//
						channel.getPastValues() //
								.tailMap(this.readChannelValuesSince, false) // new values since last recording
								.values().stream() //
								.map(v -> v.get()) //
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
					this.log.warn("Unable to add record [" + channel.address() + "]. Queue is full!");
					this.parent._setQueueIsFull(true);
				}
			}
		}
		this.readChannelValuesSince = nextReadChannelValuesSince;
		this.triggerNextRun();
	}

	@Override
	protected void forever() throws InterruptedException {
		Record record = this.records.take();
		RrdDb database = null;
		try {
			database = this.parent.getRrdDb(record.address, record.unit, record.timestamp - 1);

			if (database.getLastUpdateTime() < record.timestamp) {
				// Avoid and silently ignore error "IllegalArgumentException: Bad sample time:
				// XXX. Last update time was YYY, at least one second step is required".

				// Add Sample to RRD4J
				Sample sample = database.createSample(record.timestamp);
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

	private static final ToDoubleFunction<? super Object> MAP_BOOLEAN_TO_DOUBLE = (value) -> {
		return (Boolean) value ? 1d : 0d;
	};

	private static final ToDoubleFunction<? super Object> MAP_SHORT_TO_DOUBLE = (value) -> {
		return ((Short) value).doubleValue();
	};
	private static final ToDoubleFunction<? super Object> MAP_INTEGER_TO_DOUBLE = (value) -> {
		return ((Integer) value).doubleValue();
	};
	private static final ToDoubleFunction<? super Object> MAP_LONG_TO_DOUBLE = (value) -> {
		return ((Long) value).doubleValue();
	};
	private static final ToDoubleFunction<? super Object> MAP_FLOAT_TO_DOUBLE = (value) -> {
		return ((Float) value).doubleValue();
	};
	private static final ToDoubleFunction<? super Object> MAP_DOUBLE_TO_DOUBLE = (value) -> {
		return (Double) value;
	};
	private static final ToDoubleFunction<? super Object> MAP_TO_DOUBLE_NOT_SUPPORTED = (value) -> {
		return 0d;
	};

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

	private Function<DoubleStream, OptionalDouble> getChannelAggregateFunction(Unit channelUnit) {
		switch (channelUnit) {
		case AMPERE:
		case AMPERE_HOURS:
		case DEGREE_CELSIUS:
		case DEZIDEGREE_CELSIUS:
		case HERTZ:
		case HOUR:
		case KILOAMPERE_HOURS:
		case KILOOHM:
		case KILOVOLT_AMPERE:
		case KILOVOLT_AMPERE_REACTIVE:
		case KILOWATT:
		case MICROOHM:
		case MILLIAMPERE_HOURS:
		case MILLIAMPERE:
		case MILLIHERTZ:
		case MILLIOHM:
		case MILLISECONDS:
		case MILLIVOLT:
		case MILLIWATT:
		case MINUTE:
		case NONE:
		case WATT:
		case VOLT:
		case VOLT_AMPERE:
		case VOLT_AMPERE_REACTIVE:
		case WATT_HOURS_BY_WATT_PEAK:
		case OHM:
		case SECONDS:
		case THOUSANDTH:
		case PERCENT:
		case ON_OFF:
			return DoubleStream::average;
		case WATT_HOURS:
		case KILOWATT_HOURS:
		case VOLT_AMPERE_HOURS:
		case VOLT_AMPERE_REACTIVE_HOURS:
		case KILOVOLT_AMPERE_REACTIVE_HOURS:
			return DoubleStream::max;
		}
		throw new IllegalArgumentException("Channel Unit [" + channelUnit + "] is not supported.");
	}

	public void setNoOfCycles(int noOfCycles) {
		this.noOfCycles = noOfCycles;
	}

}
