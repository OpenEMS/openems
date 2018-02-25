package io.openems.common.websocket;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.java_websocket.WebSocket;
import com.google.common.collect.HashMultimap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.types.ChannelAddress;

public abstract class CurrentDataWorker {

	protected final static int UPDATE_INTERVAL_IN_SECONDS = 2;

	/**
	 * Executor for subscriptions task
	 */
	private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

	/**
	 * Holds thingId and channelId, subscribed by this websocket
	 */
	private final HashMultimap<String, String> channels;

	/**
	 * Holds the scheduled task for currentData
	 */
	private final ScheduledFuture<?> future;

	private final WebSocket websocket;

	public CurrentDataWorker(WebSocket websocket, JsonObject jMessageId, HashMultimap<String, String> channels) {
		this.websocket = websocket;
		this.channels = channels;
		this.future = this.executor.scheduleWithFixedDelay(() -> {
			/*
			 * This task is executed regularly. Sends data to websocket.
			 */
			if (!this.websocket.isOpen()) {
				// disconnected; stop worker
				this.dispose();
				return;
			}
			WebSocketUtils.sendOrLogError(this.websocket, DefaultMessages.currentData(jMessageId, getSubscribedData()));
		}, 0, UPDATE_INTERVAL_IN_SECONDS, TimeUnit.SECONDS);
	}

	public void dispose() {
		// unsubscribe regular task
		future.cancel(true);
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
