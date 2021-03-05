package io.openems.edge.controller.api.backend;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableTable;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import io.openems.common.channel.AccessMode;
import io.openems.common.jsonrpc.notification.TimestampedDataNotification;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.OpenemsComponent;

public class BackendWorker {

	private static final int SEND_VALUES_OF_ALL_CHANNELS_AFTER_SECONDS = 300; /* 5 minutes */

	private final Logger log = LoggerFactory.getLogger(BackendWorker.class);
	private final BackendApiImpl parent;
	private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.SECONDS,
			new ArrayBlockingQueue<>(1), new ThreadPoolExecutor.DiscardOldestPolicy());

	// Component-ID to Channel-ID to value
	private ImmutableTable<String, String, JsonElement> lastValues = ImmutableTable.of();

	private Instant lastSendValuesOfAllChannels = Instant.MIN;

	protected BackendWorker(BackendApiImpl parent) {
		this.parent = parent;
	}

	/**
	 * Called synchronously on AFTER_PROCESS_IMAGE event. Collects all the data and
	 * triggers asynchronous sending.
	 */
	public synchronized void collectData() {
		Instant now = Instant.now(this.parent.componentManager.getClock());

		// Send values of all Channels once in a while
		if (Duration.between(this.lastSendValuesOfAllChannels, now)
				.getSeconds() > SEND_VALUES_OF_ALL_CHANNELS_AFTER_SECONDS) {
			this.lastValues = ImmutableTable.of();
		}
		if (this.lastValues.isEmpty()) {
			this.lastSendValuesOfAllChannels = now;
		}

		final List<OpenemsComponent> enabledComponents = this.parent.componentManager.getEnabledComponents();

		// Update the data from ChannelValues
		final ImmutableTable<String, String, JsonElement> allValues = this.collectData(enabledComponents);

		// Get timestamp and round to Global Cycle-Time
		final int cycleTime = this.parent.cycle.getCycleTime();
		final long timestamp = now.toEpochMilli() / cycleTime * cycleTime;

		// Prepare message values
		Map<ChannelAddress, JsonElement> sendValues = new HashMap<>();

		// Collect Changed values
		allValues.rowMap().entrySet().parallelStream() //
				.forEach(row -> {
					row.getValue().entrySet().parallelStream() //
							.forEach(column -> {
								if (!Objects.equals(column.getValue(),
										this.lastValues.get(row.getKey(), column.getKey()))) {
									sendValues.put(new ChannelAddress(row.getKey(), column.getKey()),
											column.getValue());
								}
							});
				});

		// Update disappeared components
		final Set<String> enabledComponentIds = enabledComponents.stream() //
				.map(c -> c.id()) //
				.collect(Collectors.toSet());
		this.lastValues.rowMap().entrySet().stream() //
				.filter(row -> !enabledComponentIds.contains(row.getKey())) //
				.forEach(row -> {
					row.getValue().entrySet().parallelStream() //
							.forEach(column -> {
								sendValues.put(new ChannelAddress(row.getKey(), column.getKey()), JsonNull.INSTANCE);
							});
				});

		// Keep values for next run
		this.lastValues = allValues;

		// Nothing to send
		if (sendValues.isEmpty()) {
			return;
		}

		// create JSON-RPC notification
		TimestampedDataNotification message = new TimestampedDataNotification();
		message.add(timestamp, sendValues);

		// Add to Task Queue
		this.executor.execute(new SendTask(this, message));
	}

	/**
	 * Triggers sending all Channel values once. After executing once, this is reset
	 * automatically to default 'send changed values only' mode.
	 */
	public synchronized void sendValuesOfAllChannelsOnce() {
		this.lastValues = ImmutableTable.of();
	}

	public void deactivate() {
		// Shutdown executor
		if (this.executor != null) {
			try {
				this.executor.shutdown();
				this.executor.awaitTermination(5, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				this.log.warn("tasks interrupted");
			} finally {
				if (!this.executor.isTerminated()) {
					this.log.warn("cancel non-finished tasks");
				}
				this.executor.shutdownNow();
			}
		}
	}

	/**
	 * Cycles through all Channels and collects the value.
	 * 
	 * @param enabledComponents the enabled components
	 * @return collected data
	 */
	private ImmutableTable<String, String, JsonElement> collectData(List<OpenemsComponent> enabledComponents) {
		return enabledComponents.parallelStream() //
				.flatMap(component -> component.channels().parallelStream()) //
				.filter(channel -> // Ignore WRITE_ONLY Channels
				channel.channelDoc().getAccessMode() == AccessMode.READ_ONLY
						|| channel.channelDoc().getAccessMode() == AccessMode.READ_WRITE)
				.collect(ImmutableTable.toImmutableTable(c -> c.address().getComponentId(),
						c -> c.address().getChannelId(), c -> c.value().asJson()));
	}

	private static class SendTask implements Runnable {

		private final BackendWorker parent;
		private final TimestampedDataNotification message;

		public SendTask(BackendWorker parent, TimestampedDataNotification message) {
			this.parent = parent;
			this.message = message;
		}

		@Override
		public void run() {
			// Try to send; drop message if not possible to send (i.e. task is not
			// rescheduled)
			boolean wasSent = this.parent.parent.websocket.sendMessage(this.message);

			// Set the UNABLE_TO_SEND channel
			this.parent.parent.getUnableToSendChannel().setNextValue(!wasSent);
		}

	};
}