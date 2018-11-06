package io.openems.edge.controller.api.backend;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.EvictingQueue;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.StringUtils;
import io.openems.common.websocket.DefaultMessages;
import io.openems.edge.common.worker.AbstractWorker;

class BackendWorker extends AbstractWorker {

	private final Logger log = LoggerFactory.getLogger(BackendWorker.class);

	private final BackendApi parent;

	private Optional<Integer> increasedCycleTime = Optional.empty();

	// Last cached values
	private final HashMap<ChannelAddress, JsonElement> last = new HashMap<>();
	// Unsent queue (FIFO)
	private EvictingQueue<JsonObject> unsent = EvictingQueue.create(1000);

	/**
	 * @param backendApi
	 */
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
		JsonObject jValues = getChangedValues();
		boolean canSendFromCache;

		/*
		 * send, if list is not empty
		 */
		if (!jValues.entrySet().isEmpty()) {
			// Get timestamp and round to Cycle-Time
			int cycleTime = this.getCycleTime();
			long timestamp = System.currentTimeMillis() / cycleTime * cycleTime;

			// create websocket message
			JsonObject j = DefaultMessages.timestampedData(timestamp, jValues);

			// reset cycleTime to default
			resetCycleTime();

			boolean wasSent = this.sendOrLogError(j);
			if (!wasSent) {
				// increase cycleTime
				increaseCycleTime();

				// cache data for later
				this.unsent.add(j);
			}

			canSendFromCache = wasSent;
		} else {
			canSendFromCache = true;
		}

		// send from cache
		if (canSendFromCache && !this.unsent.isEmpty()) {
			for (Iterator<JsonObject> iterator = this.unsent.iterator(); iterator.hasNext();) {
				JsonObject jCached = iterator.next();
				boolean cacheWasSent = this.sendOrLogError(jCached);
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
	 * Goes through all Channels and gets the value. If the value changed since last
	 * check, it is added to the queue.
	 */
	private JsonObject getChangedValues() {
		final JsonObject j = new JsonObject();
		this.parent.getComponents().stream().filter(c -> c.isEnabled()).forEach(component -> {
			component.channels().forEach(channel -> {
				// Ignore WRITE_ONLY Channels
				switch (channel.channelDoc().getAccessMode()) {
				case READ_ONLY:
				case READ_WRITE:
					break;
				case WRITE_ONLY:
					return;
				}

				ChannelAddress address = channel.address();
				JsonElement jValue = channel.value().asJson();
				JsonElement jLastValue = this.last.get(address);
				if (jLastValue == null || !jLastValue.equals(jValue)) {
					// this value differs from the last sent value -> add to queue
					// TODO use JsonNull in Backend
					if (jValue.equals(JsonNull.INSTANCE)) {
						return;
					}
					j.add(address.toString(), jValue);
					this.last.put(address, jValue);
				}
			});
		});
		return j;
	}

	/**
	 * Send message to websocket
	 *
	 * @param j
	 * @return
	 * @throws OpenemsException
	 */
	private boolean sendOrLogError(JsonObject j) {
		try {
			this.parent.websocket.send(j);
			if (this.parent.debug) {
				log.info("Sent successfully: " + StringUtils.toShortString(j, 100));
			}
			return true;
		} catch (OpenemsException e) {
			log.warn("Unable to send! " + e.getMessage() + ". Content: " + StringUtils.toShortString(j, 100));
			return false;
		}
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