package io.openems.edge.controller.api.backend;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.EvictingQueue;
import com.google.gson.JsonElement;

import io.openems.common.jsonrpc.base.JsonrpcMessage;
import io.openems.common.jsonrpc.notification.TimestampedDataNotification;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.doc.AccessMode;
import io.openems.edge.common.worker.AbstractWorker;

class BackendWorker extends AbstractWorker {

	private static final int MAX_CACHED_MESSAGES = 1000;

	// private final Logger log = LoggerFactory.getLogger(BackendWorker.class);
	private final BackendApi parent;

	private Optional<Integer> increasedCycleTime = Optional.empty();

	// Last cached values
	private final HashMap<ChannelAddress, JsonElement> last = new HashMap<>();
	// Unsent queue (FIFO)
	private EvictingQueue<JsonrpcMessage> unsent = EvictingQueue.create(MAX_CACHED_MESSAGES);

	BackendWorker(BackendApi parent) {
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

	@Override
	protected void forever() {
		// TODO send all data once after reconnect. The Backend might have been
		// restartet.
		Map<ChannelAddress, JsonElement> values = this.getChangedValues();
		boolean canSendFromCache;

		/*
		 * send, if list is not empty
		 */
		if (!values.isEmpty()) {
			// Get timestamp and round to Cycle-Time
			int cycleTime = this.getCycleTime();
			long timestamp = System.currentTimeMillis() / cycleTime * cycleTime;

			// create JSON-RPC notification
			TimestampedDataNotification message = new TimestampedDataNotification();
			message.add(timestamp, values);

			// reset cycleTime to default
			resetCycleTime();

			boolean wasSent = this.parent.websocket.sendMessage(message);
			if (!wasSent) {
				// increase cycleTime
				increaseCycleTime();

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

	@Override
	protected int getCycleTime() {
		return this.increasedCycleTime.orElse(this.parent.cycleTime);
	}

	/**
	 * Cycles through all Channels and gets the value. If the value changed since
	 * last check, it is added to the queue.
	 * 
	 * @return a map of channels who's value changed since last check
	 */
	private Map<ChannelAddress, JsonElement> getChangedValues() {
		final ConcurrentHashMap<ChannelAddress, JsonElement> values = new ConcurrentHashMap<>();
		this.parent.componentManager.getComponents().parallelStream() //
				.filter(c -> c.isEnabled()) //
				.flatMap(component -> component.channels().parallelStream()) //
				.filter(channel -> // Ignore WRITE_ONLY Channels
				channel.channelDoc().getAccessMode() == AccessMode.READ_ONLY
						|| channel.channelDoc().getAccessMode() == AccessMode.READ_WRITE)
				.forEach(channel -> {
					ChannelAddress address = channel.address();
					JsonElement jValue = channel.value().asJson();
					JsonElement jLastValue = this.last.get(address);
					if (jLastValue == null || !jLastValue.equals(jValue)) {
						// this value differs from the last sent value
						this.last.put(address, jValue);
						values.put(address, jValue);
					}
				});
		return values;
	}

	private void increaseCycleTime() {
		int currentCycleTime = this.getCycleTime();
		int newCycleTime;
		if (currentCycleTime < 30000 /* 30 seconds */) {
			newCycleTime = currentCycleTime * 2;
		} else {
			newCycleTime = currentCycleTime;
		}
		if (currentCycleTime != newCycleTime) {
			this.increasedCycleTime = Optional.of(newCycleTime);
		}
	}

	/**
	 * Cycletime is adjusted if connection to Backend fails. This method resets it
	 * to configured or default value.
	 */
	private void resetCycleTime() {
		this.increasedCycleTime = Optional.empty();
	}

}