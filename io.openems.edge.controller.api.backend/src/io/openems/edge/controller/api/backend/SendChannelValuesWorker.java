package io.openems.edge.controller.api.backend;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.TreeBasedTable;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.openems.common.channel.AccessMode;
import io.openems.common.jsonrpc.notification.AggregatedDataNotification;
import io.openems.common.jsonrpc.notification.TimestampedDataNotification;
import io.openems.common.timedata.DurationUnit;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.ThreadPoolUtils;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.type.TypeUtils;

/**
 * Method {@link #collectData()} is called Synchronously with the Core.Cycle to
 * collect values of Channels. Sending of values is then delegated to an
 * asynchronous task.
 *
 * <p>
 * The logic tries to send changed values once per Cycle and all values once
 * every {@link #SEND_VALUES_OF_ALL_CHANNELS_AFTER_SECONDS}.
 */
public class SendChannelValuesWorker {

	private static final int AGGREGATION_MINUTES = 5;
	private static final int SEND_VALUES_OF_ALL_CHANNELS_AFTER_SECONDS = 300; /* 5 minutes */

	private final Logger log = LoggerFactory.getLogger(SendChannelValuesWorker.class);

	private final BackendApiImpl parent;
	private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.SECONDS,
			new ArrayBlockingQueue<>(1), //
			new ThreadFactoryBuilder().setNameFormat(BackendApiImpl.COMPONENT_NAME + ":SendWorker-%d").build(), //
			new ThreadPoolExecutor.DiscardOldestPolicy());

	/**
	 * If true: next 'send' sends all channel values.
	 */
	private final AtomicBoolean sendValuesOfAllChannels = new AtomicBoolean(true);

	/**
	 * Keeps the last timestamp when all channel values were sent.
	 */
	private Instant lastSendValuesOfAllChannels = Instant.MIN;

	/**
	 * Keeps the values of last successful send.
	 */
	private Map<String, JsonElement> lastAllValues = ImmutableMap.of();

	private Instant lastSendAggregatedDataTimestamp;

	protected SendChannelValuesWorker(BackendApiImpl parent) {
		this.parent = parent;
	}

	/**
	 * Triggers sending all Channel values once.
	 */
	public synchronized void sendValuesOfAllChannelsOnce() {
		this.sendValuesOfAllChannels.set(true);
	}

	/**
	 * Stops the {@link SendChannelValuesWorker}.
	 */
	public void deactivate() {
		// Shutdown executor
		ThreadPoolUtils.shutdownAndAwaitTermination(this.executor, 5);
	}

	/**
	 * Called synchronously on AFTER_PROCESS_IMAGE event. Collects all the data and
	 * triggers asynchronous sending.
	 */
	public synchronized void collectData() {
		var now = Instant.now(this.parent.componentManager.getClock());

		// Update the values of all channels
		final var enabledComponents = this.parent.componentManager.getEnabledComponents();
		final var allValues = this.collectData(enabledComponents);
		final var aggregatedValues = this.collectAggregatedData(enabledComponents);

		// Add to send Queue
		this.executor.execute(new SendTask(this, now, allValues));
		if (aggregatedValues != null && !aggregatedValues.isEmpty()) {
			aggregatedValues.rowMap().forEach((timestamp, data) -> {
				this.executor.execute(new SendAggregatedDataTask(this, Instant.ofEpochMilli(timestamp), data));
			});
		}
	}

	/**
	 * Cycles through all Channels and collects the value.
	 *
	 * @param enabledComponents the enabled components
	 * @return collected data
	 */
	private ImmutableMap<String, JsonElement> collectData(List<OpenemsComponent> enabledComponents) {
		try {
			return enabledComponents.parallelStream() //
					.flatMap(component -> component.channels().parallelStream()) //
					.filter(channel -> // Ignore WRITE_ONLY Channels
					channel.channelDoc().getAccessMode() != AccessMode.WRITE_ONLY //
							// Ignore Low-Priority Channels
							&& channel.channelDoc().getPersistencePriority()
									.isAtLeast(this.parent.config.persistencePriority()))
					.collect(//
							ImmutableMap.toImmutableMap(//
									c -> c.address().toString(), //
									c -> c.value().asJson(), //
									// simple/stupid merge function to avoid
									// 'java.lang.IllegalArgumentException Duplicate Key'
									(t, u) -> {
										this.parent.logWarn(this.log, "Duplicate Key [" + t.toString() + "]");
										return t;
									}));
		} catch (Exception e) {
			// ConcurrentModificationException can happen if Channels are dynamically added
			// or removed
			this.parent.logWarn(this.log, "Unable to collect date: " + e.getMessage());
			return ImmutableMap.of();
		}
	}

	private TreeBasedTable<Long, String, JsonElement> collectAggregatedData(List<OpenemsComponent> enabledComponents) {
		final var now = LocalDateTime.now(this.parent.componentManager.getClock());
		final var endTime = now.truncatedTo(DurationUnit.ofMinutes(AGGREGATION_MINUTES));
		final var startTime = endTime.minusMinutes(AGGREGATION_MINUTES);

		final var timestamp = Instant.now().truncatedTo(DurationUnit.ofMinutes(AGGREGATION_MINUTES)) //
				.minus(AGGREGATION_MINUTES, ChronoUnit.MINUTES);
		if (this.lastSendAggregatedDataTimestamp == null) {
			this.lastSendAggregatedDataTimestamp = timestamp;
			return null;
		}
		if (timestamp.equals(this.lastSendAggregatedDataTimestamp)) {
			return null;
		}
		this.lastSendAggregatedDataTimestamp = timestamp;
		final var timestampMillis = timestamp.toEpochMilli();

		final var table = TreeBasedTable.<Long, String, JsonElement>create();
		enabledComponents.stream() //
				.flatMap(component -> component.channels().stream()) //
				.filter(channel -> // Ignore WRITE_ONLY Channels
				channel.channelDoc().getAccessMode() != AccessMode.WRITE_ONLY //
						// Ignore Low-Priority Channels
						&& channel.channelDoc().getPersistencePriority()
								.isAtLeast(this.parent.config.aggregationPriority()))
				.forEach(channel -> {
					try {
						// This is the highest timestamp before `startTime`. If existing it is used for
						// the tailMap to make sure we get a Value even for Channels where the value has
						// not changed within the last 5 minutes.
						var channelStartTime = Optional.ofNullable(channel.getPastValues().floorKey(startTime))
								.orElse(startTime);

						var value = channel.getPastValues() //
								.tailMap(channelStartTime, true) //
								.entrySet() //
								.stream() //
								.filter(e -> e.getKey().isBefore(endTime)) //
								.filter(e -> e.getValue().isDefined()).map(e -> e.getValue().get()) //
								.collect(aggregateCollector(channel.channelDoc().getUnit().isCumulated(), //
										channel.getType()));

						if (value == null) {
							return;
						}
						table.put(timestampMillis, channel.address().toString(), value);
					} catch (IllegalArgumentException e) {
						// unable to collect data because types are not matching the expected one
						e.printStackTrace();
					}
				});
		return table;
	}

	protected static Collector<Object, ?, JsonElement> aggregateCollector(//
			final boolean isCumulated, //
			final OpenemsType type //
	) {
		return Collector.of(ArrayList::new, //
				(a, b) -> a.add(b), //
				(a, b) -> {
					b.addAll(a);
					return b;
				}, //
				t -> aggregate(isCumulated, type, t) //
		);
	}

	protected static JsonElement aggregate(boolean isCumulated, OpenemsType type, Collection<Object> values)
			throws IllegalArgumentException {
		switch (type) {
		case DOUBLE:
		case FLOAT: {
			final var stream = values.stream() //
					.mapToDouble(item -> TypeUtils.getAsType(OpenemsType.DOUBLE, item));
			if (isCumulated) {
				final var maxOpt = stream.max();
				if (maxOpt.isPresent()) {
					return new JsonPrimitive(maxOpt.getAsDouble());
				}
			} else {
				final var avgOpt = stream.average();
				if (avgOpt.isPresent()) {
					return new JsonPrimitive(avgOpt.getAsDouble());
				}
			}
			break;
		}
		// round averages to their type
		case BOOLEAN:
		case LONG:
		case INTEGER:
		case SHORT:
			final var stream = values.stream() //
					.mapToLong(item -> TypeUtils.getAsType(OpenemsType.LONG, item));
			if (isCumulated) {
				final var maxOpt = stream.max();
				if (maxOpt.isPresent()) {
					return new JsonPrimitive(maxOpt.getAsLong());
				}
			} else {
				final var avgOpt = stream.average();
				if (avgOpt.isPresent()) {
					return new JsonPrimitive(Math.round(avgOpt.getAsDouble()));
				}
			}
			break;
		case STRING:
			// return first string for now
			for (var item : values) {
				return new JsonPrimitive(TypeUtils.<String>getAsType(type, item));
			}
		}
		return null;
	}

	/*
	 * From here things run asynchronously.
	 */
	private static class SendTask implements Runnable {

		private final SendChannelValuesWorker parent;
		private final Instant timestamp;
		private final Map<String, JsonElement> allValues;

		public SendTask(SendChannelValuesWorker parent, Instant timestamp, Map<String, JsonElement> allValues) {
			this.parent = parent;
			this.timestamp = timestamp;
			this.allValues = allValues;
		}

		@Override
		public void run() {
			// Holds the data of the last successful send. If the table is empty, it is also
			// used as a marker to send all data.
			final Map<String, JsonElement> lastAllValues;

			if (this.parent.sendValuesOfAllChannels.getAndSet(false)) {
				// Send values of all Channels once in a while
				lastAllValues = ImmutableMap.of();

			} else if (Duration.between(this.parent.lastSendValuesOfAllChannels, this.timestamp)
					.getSeconds() > SEND_VALUES_OF_ALL_CHANNELS_AFTER_SECONDS) {
				// Send values of all Channels if explicitly asked for
				lastAllValues = ImmutableMap.of();

			} else {
				// Actually use the kept 'lastSentValues'
				// CHECKSTYLE:OFF
				lastAllValues = this.parent.lastAllValues;
				// CHECKSTYLE:ON
			}

			// Round timestamp to Global Cycle-Time
			final var cycleTime = this.parent.parent.cycle.getCycleTime();
			final var timestampMillis = this.timestamp.toEpochMilli() / cycleTime * cycleTime;

			// Prepare message values
			var sendValuesMap = new HashMap<String, JsonElement>();

			// Collect Changed values
			for (var entry : this.allValues.entrySet()) {
				var channelAddress = entry.getKey();
				var value = entry.getValue();
				if (!Objects.equals(value, lastAllValues.get(channelAddress))) {
					sendValuesMap.put(channelAddress, value);
				}
			}

			// Create JSON-RPC notification
			var message = new TimestampedDataNotification();
			message.add(timestampMillis, sendValuesMap);

			// Debug-Log
			if (this.parent.parent.config.debugMode()) {
				this.parent.parent.logInfo(this.parent.log,
						"Sending [" + sendValuesMap.size() + " values]: " + sendValuesMap);
			}

			// Try to send
			var wasSent = this.parent.parent.websocket.sendMessage(message);

			// Set the UNABLE_TO_SEND channel
			this.parent.parent.getUnableToSendChannel().setNextValue(!wasSent);

			if (wasSent) {
				// Successfully sent: update information for next runs
				this.parent.lastAllValues = this.allValues;
				if (lastAllValues.isEmpty()) {
					// 'lastSentValues' was empty, i.e. all values were sent
					this.parent.lastSendValuesOfAllChannels = this.timestamp;
				}
			}

		}

	}

	private static final class SendAggregatedDataTask implements Runnable {

		private final SendChannelValuesWorker parent;
		private final Instant timestamp;
		private final Map<String, JsonElement> allValues;

		public SendAggregatedDataTask(SendChannelValuesWorker parent, Instant timestamp,
				Map<String, JsonElement> allValues) {
			super();
			this.parent = parent;
			this.timestamp = timestamp;
			this.allValues = allValues;
		}

		@Override
		public void run() {
			final var message = new AggregatedDataNotification();
			message.add(this.timestamp.toEpochMilli(), this.allValues);

			this.parent.parent.websocket.sendMessage(message);
		}

	}

}