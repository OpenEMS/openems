package io.openems.common.websocket;

import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.java_websocket.WebSocket;
import com.google.common.collect.HashMultimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.Log;

public abstract class CurrentDataWorker {

	protected final static int UPDATE_INTERVAL_IN_SECONDS = 2;

	/**
	 * Executor for subscriptions task
	 */
	private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

	/**
	 * Holds thingId and channelId, subscribed by this websocket
	 */
	private final HashMultimap<String, String> channels = HashMultimap.create();

	/**
	 * Holds the scheduled task for currentData
	 */
	private Optional<ScheduledFuture<?>> futureOpt = Optional.empty();

	protected final WebSocket websocket;

	public CurrentDataWorker(WebSocket websocket) {
		this.websocket = websocket;
	}

	public synchronized void setChannels(JsonObject jSubscribeChannels, JsonObject jMessageId) {
		// stop current thread
		if (this.futureOpt.isPresent()) {
			this.futureOpt.get().cancel(true);
			this.futureOpt = Optional.empty();
		}

		// clear existing channels
		this.channels.clear();

		// parse and add subscribed channels
		for (Entry<String, JsonElement> entry : jSubscribeChannels.entrySet()) {
			String thing = entry.getKey();
			try {
				JsonArray jChannels = JsonUtils.getAsJsonArray(entry.getValue());
				for (JsonElement jChannel : jChannels) {
					String channel = JsonUtils.getAsString(jChannel);
					channels.put(thing, channel);
				}
			} catch (OpenemsException e) {
				Log.warn("Unable to add channel subscription: " + e.getMessage());
			}
		}
		if (!channels.isEmpty()) {
			// registered channels -> create new thread
			this.futureOpt = Optional.of(this.executor.scheduleWithFixedDelay(() -> {
				/*
				 * This task is executed regularly. Sends data to websocket.
				 */
				if (!this.websocket.isOpen()) {
					// disconnected; stop worker
					this.dispose();
					return;
				}
				WebSocketUtils.sendOrLogError(this.websocket,
						DefaultMessages.currentData(jMessageId, getSubscribedData()));
			}, 0, UPDATE_INTERVAL_IN_SECONDS, TimeUnit.SECONDS));
		}
	}

	public void dispose() {
		// unsubscribe regular task
		if (this.futureOpt.isPresent()) {
			futureOpt.get().cancel(true);
		}
	}

	/**
	 * Gets a json object with all subscribed channels
	 *
	 * @return
	 */
	private JsonObject getSubscribedData() {
		JsonObject jData = new JsonObject();
		for (String thingId : this.channels.keys()) {
			JsonObject jThingData = new JsonObject();
			for (String channelId : this.channels.get(thingId)) {
				ChannelAddress channelAddress = new ChannelAddress(thingId, channelId);
				Optional<JsonElement> jValueOpt = this.getChannelValue(channelAddress);
				if (jValueOpt.isPresent()) {
					jThingData.add(channelId, jValueOpt.get());
				}
			}
			jData.add(thingId, jThingData);
		}
		return jData;
	}

	protected abstract Optional<JsonElement> getChannelValue(ChannelAddress channelAddress);
}
