package io.openems.edge.controller.api.backend;

import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.collect.EvictingQueue;
import com.google.gson.JsonElement;

import io.openems.common.channel.AccessMode;
import io.openems.common.jsonrpc.base.JsonrpcMessage;
import io.openems.common.jsonrpc.notification.TimestampedDataNotification;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.common.worker.AbstractCycleWorker;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.type.slidingvalue.DoubleSlidingValue;
import io.openems.edge.common.type.slidingvalue.FloatSlidingValue;
import io.openems.edge.common.type.slidingvalue.IntegerSlidingValue;
import io.openems.edge.common.type.slidingvalue.LatestSlidingValue;
import io.openems.edge.common.type.slidingvalue.LongSlidingValue;
import io.openems.edge.common.type.slidingvalue.ShortSlidingValue;
import io.openems.edge.common.type.slidingvalue.SlidingValue;

class BackendWorker extends AbstractCycleWorker {

	private static final int MAX_CACHED_MESSAGES = 1000;

	// private final Logger log = LoggerFactory.getLogger(BackendWorker.class);

	private final BackendApiImpl parent;

	// Counts the number of Cycles till data is sent to Backend.
	private int cycleCount = 0;

	// Holds an current NoOfCycles
	private Optional<Integer> increasedNoOfCycles = Optional.empty();

	// Current values
	private final ConcurrentHashMap<ChannelAddress, SlidingValue<?>> data = new ConcurrentHashMap<>();

	// Unsent queue (FIFO)
	private EvictingQueue<JsonrpcMessage> unsent = EvictingQueue.create(MAX_CACHED_MESSAGES);

	// By default the worker reads and sends only changed values. If this variable
	// is set to 'false', it sends all values once.
	private final AtomicBoolean sendChangedValuesOnly = new AtomicBoolean(false);

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
	public void sendValuesOfAllChannelsOnce() {
		this.sendChangedValuesOnly.set(false);
		this.triggerNextRun();
	}

	@Override
	protected void forever() {
		// Update the data from ChannelValues
		this.updateData();

		// Increase CycleCount
		if (++this.cycleCount < this.parent.noOfCycles) {
			// Stop here if not reached CycleCount
			return;
		}

		/*
		 * Reached CycleCount -> Send data
		 */
		// Reset CycleCount
		this.cycleCount = 0;

		// resets the mode to 'send changed values only'
		boolean sendChangedValuesOnly = this.sendChangedValuesOnly.getAndSet(false);

		// Prepare message values
		Map<ChannelAddress, JsonElement> sendValues = new HashMap<>();

		if (sendChangedValuesOnly) {
			// Only Changed Values
			for (Entry<ChannelAddress, SlidingValue<?>> entry : this.data.entrySet()) {
				JsonElement changedValueOrNull = entry.getValue().getChangedValueOrNull();
				if (changedValueOrNull != null) {
					sendValues.put(entry.getKey(), changedValueOrNull);
				}
			}
		} else {
			// All Values
			for (Entry<ChannelAddress, SlidingValue<?>> entry : this.data.entrySet()) {
				sendValues.put(entry.getKey(), entry.getValue().getValue());
			}
		}

		boolean canSendFromCache;

		/*
		 * send, if list is not empty
		 */
		if (!sendValues.isEmpty()) {
			// Get timestamp and round to Cycle-Time
			int cycleTime = this.getCycleTime();
			long timestamp = Instant.now(this.parent.componentManager.getClock()).toEpochMilli() / cycleTime
					* cycleTime;

			// create JSON-RPC notification
			TimestampedDataNotification message = new TimestampedDataNotification();
			message.add(timestamp, sendValues);

			// reset cycleTime to default
			this.resetNoOfCycles();

			boolean wasSent = this.parent.websocket.sendMessage(message);
			if (!wasSent) {
				// increase cycleTime
				this.increaseNoOfCycles();

				// cache data for later
				this.unsent.add(message);
			}

			canSendFromCache = wasSent;
		} else {
			canSendFromCache = true;
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
	 * Cycles through all Channels and updates the value.
	 */
	private void updateData() {
		this.parent.componentManager.getEnabledComponents().parallelStream() //
				.filter(c -> c.isEnabled()) //
				.flatMap(component -> component.channels().parallelStream()) //
				.filter(channel -> // Ignore WRITE_ONLY Channels
				channel.channelDoc().getAccessMode() == AccessMode.READ_ONLY
						|| channel.channelDoc().getAccessMode() == AccessMode.READ_WRITE)
				.forEach(channel -> {
					ChannelAddress address = channel.address();
					Object value = channel.value().get();

					// Get existing SlidingValue object or add new one
					SlidingValue<?> slidingValue = this.data.get(address);

					if (slidingValue == null) {
						// Create new SlidingValue object
						if (channel instanceof EnumReadChannel) {
							slidingValue = new LatestSlidingValue(OpenemsType.INTEGER);
						} else {
							switch (channel.getType()) {
							case INTEGER:
								slidingValue = new IntegerSlidingValue();
								break;
							case DOUBLE:
								slidingValue = new DoubleSlidingValue();
								break;
							case FLOAT:
								slidingValue = new FloatSlidingValue();
								break;
							case LONG:
								slidingValue = new LongSlidingValue();
								break;
							case SHORT:
								slidingValue = new ShortSlidingValue();
								break;
							case BOOLEAN:
							case STRING:
								slidingValue = new LatestSlidingValue(channel.getType());
								break;
							}
						}
						this.data.put(address, slidingValue);
					}

					// Add Value to SlidingValue object
					if (slidingValue instanceof LatestSlidingValue) {
						((LatestSlidingValue) slidingValue).addValue(value);
					} else {
						switch (channel.getType()) {
						case INTEGER:
							((IntegerSlidingValue) slidingValue).addValue((Integer) value);
							break;
						case DOUBLE:
							((DoubleSlidingValue) slidingValue).addValue((Double) value);
							break;
						case FLOAT:
							((FloatSlidingValue) slidingValue).addValue((Float) value);
							break;
						case LONG:
							((LongSlidingValue) slidingValue).addValue((Long) value);
							break;
						case SHORT:
							((ShortSlidingValue) slidingValue).addValue((Short) value);
							break;
						case BOOLEAN:
						case STRING:
							// already covered as they are of type LatestSlidingValue
							break;
						}
					}
				});
	}

	/**
	 * NoOfCycles is adjusted if connection to Backend fails. This method increases
	 * the NoOfCycles.
	 */
	private void increaseNoOfCycles() {
		int increasedNoOfCycles;
		if (this.increasedNoOfCycles.isPresent()) {
			increasedNoOfCycles = this.increasedNoOfCycles.get();
		} else {
			increasedNoOfCycles = this.parent.noOfCycles;
		}
		if (increasedNoOfCycles < 60) {
			increasedNoOfCycles++;
		}
		this.increasedNoOfCycles = Optional.of(increasedNoOfCycles);
	}

	/**
	 * NoOfCycles is adjusted if connection to Backend fails. This method resets it
	 * to configured or default value.
	 */
	private void resetNoOfCycles() {
		this.increasedNoOfCycles = Optional.empty();
	}

}