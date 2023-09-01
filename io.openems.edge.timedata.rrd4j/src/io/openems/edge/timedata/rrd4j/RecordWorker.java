package io.openems.edge.timedata.rrd4j;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.function.ToDoubleFunction;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.timedata.DurationUnit;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.common.worker.AbstractImmediateWorker;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.ComponentManager;

@Component(//
		scope = ServiceScope.PROTOTYPE, //
		service = RecordWorker.class //
)
public class RecordWorker extends AbstractImmediateWorker {

	public record Config(//
			String rrdDbId, //
			boolean readOnly, //
			boolean debugMode, //
			PersistencePriority persistencePriority, //
			Consumer<? super Boolean> onQueueFull, //
			Consumer<? super Boolean> onUnableToInsert //
	) {

	}

	private static record DataRecord(//
			long timestamp, //
			ChannelAddress address, //
			Unit unit, //
			double value //
	) {
	}

	private final Logger log = LoggerFactory.getLogger(RecordWorker.class);

	@Reference
	private Rrd4jSupplier rrd4jSupplier;

	@Reference
	private ComponentManager componentManager;

	private Config config;

	public void setConfig(Config config) {
		this.config = config;
	}

	// Record queue
	private final BlockingQueue<DataRecord> records = new LinkedBlockingQueue<>();

	// keeps the last recorded timestamp
	private Instant lastTimestamp = Instant.MIN;

	@Activate
	public RecordWorker() {
	}

	@Override
	@Deactivate
	public void deactivate() {
		super.deactivate();
	}

	/**
	 * Collects the data from Channels. This is called synchronously by the main
	 * OpenEMS cycle. On finish it triggers a next async task to write the data to
	 * RRD4J.
	 * 
	 * <p>
	 * Cumulated Channels are collected with a timestamp rounded to the current
	 * hour. e.g.
	 * 
	 * <pre>
	 * 08:00     08:35 09:00
	 *   |---------|-----|
	 * 08:00 -> timestamp of the data
	 * 08:35 -> timestamp the data gets collected
	 * </pre>
	 */
	public void collectData() {
		final var timestamp = Instant.now(this.componentManager.getClock()) //
				.truncatedTo(DurationUnit.ofSeconds(Rrd4jConstants.DEFAULT_HEARTBEAT_SECONDS)) //
				.minusSeconds(Rrd4jConstants.DEFAULT_HEARTBEAT_SECONDS);

		final var now = LocalDateTime.now(this.componentManager.getClock());

		// Increase CycleCount
		this.cycleCount += 1;

		// Same second as last run? -> RRD4j can only handle one sample per second per
		// database. Timestamps are all stored "truncated to seconds".
		if (timestamp.equals(this.lastTimestamp)) {
			return;
		}

		final var to = now.truncatedTo(DurationUnit.ofSeconds(Rrd4jConstants.DEFAULT_HEARTBEAT_SECONDS));
		final var from = to.minusSeconds(Rrd4jConstants.DEFAULT_HEARTBEAT_SECONDS);

		// RRD4j requires us to write one value per DEFAULT_HEARTBEAT_SECONDS
		if (this.lastTimestamp.equals(timestamp)) {
			return;
		}

		this.lastTimestamp = timestamp;

		this.componentManager.getEnabledComponents().stream() //
				.flatMap(component -> component.channels().stream()) //
				.filter(channel -> {
					final var doc = channel.channelDoc();
					return Optional.of(this.config.persistencePriority) //
							.map(p -> doc.getPersistencePriority().isAtLeast(p)
									&& doc.getAccessMode() != AccessMode.WRITE_ONLY) //
							.orElse(false);
				}).map(channel -> {
					final var channelMapFunction = getChannelMapFunction(channel.channelDoc().getType());
					final var channelAggregateFunction = channel.channelDoc().getUnit().getChannelAggregateFunction();

					final long writeSeconds;
					if (channel.channelDoc().getUnit().isCumulated()) {
						// Write every 1h
						writeSeconds = timestamp.truncatedTo(ChronoUnit.HOURS).getEpochSecond();
					} else {
						writeSeconds = timestamp.getEpochSecond();
					}

					// This is the highest timestamp before `startTime`. If existing it is used for
					// the tailMap to make sure we get a Value even for Channels where the value has
					// not changed within the last 5 minutes.
					var channelStartTime = Optional.ofNullable(channel.getPastValues().floorKey(from)) //
							.orElse(from);

					final var value = channelAggregateFunction.apply(//
							channel.getPastValues() //
									.tailMap(channelStartTime, true) //
									.entrySet().stream() //
									.filter(e -> e.getKey().isBefore(to)) //
									.map(Entry::getValue) //
									.map(Value::get) //
									.filter(Objects::nonNull) //
									.mapToDouble(channelMapFunction) // convert to double
					);

					if (!value.isPresent()) {
						// only available channels
						return null;
					}

					return new DataRecord(//
							writeSeconds, //
							channel.address(), //
							channel.channelDoc().getUnit(), //
							value.getAsDouble() //
					);
				}) //
				.filter(Objects::nonNull) //
				.forEach(dataRecord -> {
					this.config.onUnableToInsert.accept(!this.records.offer(dataRecord));
				});

	}

	@Override
	protected void forever() throws InterruptedException {
		final var record = this.records.take();

		if (this.config.readOnly() && this.config.debugMode()) {
			this.log.info("Read-Only-Mode is activated. Not writing record: " + record.toString());
			return;
		}

		try (var database = this.rrd4jSupplier.getRrdDb(this.config.rrdDbId, record.address, record.unit,
				record.timestamp - 1)) {
			if (database.getLastUpdateTime() == record.timestamp()) {
				// overwrite last value if same time stamp
				final var robin = database.getArchive(0).getRobin(0);
				robin.setValue(robin.getSize() - 1, record.value());
			} else if (database.getLastUpdateTime() < record.timestamp()) {
				// Avoid and silently ignore error "IllegalArgumentException: Bad sample time:
				// YYY. Last update time was ZZZ, at least one second step is required".

				// Add Sample to RRD4J
				database.createSample(record.timestamp()) //
						.setValue(0, record.value) //
						.update();
			}

			this.config.onQueueFull.accept(false);
		} catch (Throwable e) {
			this.config.onQueueFull.accept(true);
			if (this.config.debugMode()) {
				this.log.error("Unable to insert Sample [%s] %s: %s".formatted(record.address,
						e.getClass().getSimpleName(), e.getMessage()), e);
			}
		}
	}

	private static final ToDoubleFunction<? super Object> MAP_BOOLEAN_TO_DOUBLE //
			= value -> ((Boolean) value ? 1d : 0d);
	private static final ToDoubleFunction<? super Object> MAP_SHORT_TO_DOUBLE //
			= value -> ((Short) value).doubleValue();
	private static final ToDoubleFunction<? super Object> MAP_INTEGER_TO_DOUBLE //
			= value -> ((Integer) value).doubleValue();
	private static final ToDoubleFunction<? super Object> MAP_LONG_TO_DOUBLE //
			= value -> ((Long) value).doubleValue();
	private static final ToDoubleFunction<? super Object> MAP_FLOAT_TO_DOUBLE //
			= value -> ((Float) value).doubleValue();
	private static final ToDoubleFunction<? super Object> MAP_DOUBLE_TO_DOUBLE //
			= value -> ((Double) value);
	private static final ToDoubleFunction<? super Object> MAP_TO_DOUBLE_NOT_SUPPORTED //
			= value -> 0d;

	private static ToDoubleFunction<? super Object> getChannelMapFunction(OpenemsType openemsType) {
		return switch (openemsType) {
		case BOOLEAN -> MAP_BOOLEAN_TO_DOUBLE;
		case SHORT -> MAP_SHORT_TO_DOUBLE;
		case INTEGER -> MAP_INTEGER_TO_DOUBLE;
		case LONG -> MAP_LONG_TO_DOUBLE;
		case FLOAT -> MAP_FLOAT_TO_DOUBLE;
		case DOUBLE -> MAP_DOUBLE_TO_DOUBLE;
		case STRING -> MAP_TO_DOUBLE_NOT_SUPPORTED; // Strings are not supported by RRD4J
		};
	}

}
