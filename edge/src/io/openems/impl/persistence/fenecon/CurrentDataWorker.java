package io.openems.impl.persistence.fenecon;

import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.api.exception.NotImplementedException;
import io.openems.common.websocket.DefaultMessages;
import io.openems.common.websocket.WebSocketUtils;
import io.openems.core.Databus;

public class CurrentDataWorker {

	private Logger log = LoggerFactory.getLogger(CurrentDataWorker.class);

	/**
	 * Holds thingId and channelId, subscribed by this websocket
	 */
	private final HashMultimap<String, String> channels;

	/**
	 * Holds the scheduled task for currentData
	 */
	private final ScheduledFuture<?> future;

	// TODO stop this task when the connection dies without unsubscribe
	public CurrentDataWorker(JsonArray jId, HashMultimap<String, String> channels, ScheduledExecutorService executor,
			WebSocket websocket) {
		this.channels = channels;
		this.future = executor.scheduleWithFixedDelay(() -> {
			/*
			 * This task is executed regularly. Sends data to websocket.
			 */
			WebSocketUtils.send(websocket, DefaultMessages.currentData(jId, getSubscribedData()));
		}, 0, 3, TimeUnit.SECONDS);
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
		Databus databus = Databus.getInstance();
		JsonObject jData = new JsonObject();
		for (String thingId : this.channels.keys()) {
			JsonObject jThingData = new JsonObject();
			for (String channelId : this.channels.get(thingId)) {
				Optional<?> value = databus.getValue(thingId, channelId);
				try {
					JsonElement jValue = io.openems.core.utilities.JsonUtils.getAsJsonElement(value.orElse(null));
					jThingData.add(channelId, jValue);
				} catch (NotImplementedException e) {
					log.error(e.getMessage());
				}
			}
			jData.add(thingId, jThingData);
		}
		return jData;
	}
}
