package io.openems.edge.controller.api.backend;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.ImmutableTable;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import io.openems.common.channel.AccessMode;
import io.openems.common.jsonrpc.base.JsonrpcMessage;
import io.openems.common.jsonrpc.notification.TimestampedDataNotification;
import io.openems.common.types.ChannelAddress;
import io.openems.common.worker.AbstractCycleWorker;
import io.openems.edge.common.component.OpenemsComponent;

class BackendWorker extends AbstractCycleWorker {

	private static final int SEND_VALUES_OF_ALL_CHANNELS_AFTER_SECONDS = 300; /* 5 minutes */
	private static final int MAX_CACHED_MESSAGES = 100;

	private final BackendApiImpl parent;

	// Component-ID to Channel-ID to value
	private ImmutableTable<String, String, JsonElement> lastValues = ImmutableTable.of();

	// Unsent queue (FIFO)
	private EvictingQueue<JsonrpcMessage> unsent = EvictingQueue.create(MAX_CACHED_MESSAGES);

	private Instant lastSendValuesOfAllChannels = Instant.MIN;

	protected BackendWorker(BackendApiImpl parent) {
		this.parent = parent;
	}

	@Override
	public void activate(String name) {
		super.activate(name);
	}

	@Override
	public void deactivate() {
		super.deactivate();
	}

	/**
	 * Triggers sending all Channel values once. After executing once, this is reset
	 * automatically to default 'send changed values only' mode.
	 */
	public synchronized void sendValuesOfAllChannelsOnce() {
		this.lastValues = ImmutableTable.of();
		this.triggerNextRun();
	}

	@Override
	protected synchronized void forever() {
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

		// Get timestamp and round to Cycle-Time
		final int cycleTime = this.getCycleTime();
		final long timestamp = now.toEpochMilli() / cycleTime * cycleTime;

		// Prepare message values
		Map<ChannelAddress, JsonElement> sendValues = new HashMap<>();

		// Send Changed values
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

		/*
		 * send, if list is not empty
		 */
		final boolean canSendFromCache;
		if (sendValues.isEmpty()) {
			canSendFromCache = true;

		} else {
			// create JSON-RPC notification
			TimestampedDataNotification message = new TimestampedDataNotification();
			message.add(timestamp, sendValues);

			boolean wasSent = this.parent.websocket.sendMessage(message);
			if (!wasSent) {
				// cache data for later
				this.unsent.add(message);
			}

			canSendFromCache = wasSent;
		}

		// send from cache
		if (canSendFromCache && !this.unsent.isEmpty()) {
			for (Iterator<JsonrpcMessage> iterator = this.unsent.iterator(); iterator.hasNext();) {
				JsonrpcMessage cached = iterator.next();
				boolean cacheWasSent = this.parent.websocket.sendMessage(cached);
				if (cacheWasSent) {
					// sent successfully -> remove from cache & try next
					iterator.remove();
				}
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

}