package io.openems.core.utilities.websocket;

import java.util.Optional;
import java.util.concurrent.Executors;
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

import io.openems.api.channel.Channel;
import io.openems.api.exception.NotImplementedException;
import io.openems.common.exceptions.AccessDeniedException;
import io.openems.common.session.Role;
import io.openems.common.types.ChannelAddress;
import io.openems.common.websocket.DefaultMessages;
import io.openems.common.websocket.WebSocketUtils;
import io.openems.core.Databus;
import io.openems.core.ThingRepository;
import io.openems.core.utilities.JsonUtils;

public class CurrentDataWorker {

	private final static int UPDATE_INTERVAL_IN_SECONDS = 2;

	private Logger log = LoggerFactory.getLogger(CurrentDataWorker.class);

	/**
	 * Executor for subscriptions task
	 */
	private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

	/**
	 * Holds thingId and channelId, subscribed by this websocket
	 */
	private final HashMultimap<String, String> channels;

	/**
	 * The access level Role of this worker
	 */
	private final Role role;

	/**
	 * Holds the scheduled task for currentData
	 */
	private final ScheduledFuture<?> future;

	public CurrentDataWorker(JsonArray jId, HashMultimap<String, String> channels, Role role,
			EdgeWebsocketHandler edgeWebsocketHandler) {
		this.channels = channels;
		this.role = role;
		this.future = this.executor.scheduleWithFixedDelay(() -> {
			/*
			 * This task is executed regularly. Sends data to websocket.
			 */
			Optional<WebSocket> wsOpt = edgeWebsocketHandler.getWebsocket();
			if (!(wsOpt.isPresent() && wsOpt.get().isOpen())) {
				// disconnected; stop worker
				this.dispose();
				return;
			}
			WebSocketUtils.send(edgeWebsocketHandler.getWebsocket(),
					DefaultMessages.currentData(jId, getSubscribedData()));
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
		ThingRepository thingRepository = ThingRepository.getInstance();
		Databus databus = Databus.getInstance();
		JsonObject jData = new JsonObject();
		for (String thingId : this.channels.keys()) {
			JsonObject jThingData = new JsonObject();
			for (String channelId : this.channels.get(thingId)) {
				ChannelAddress channelAddress = new ChannelAddress(thingId, channelId);
				// TODO rename getChannel() to getChannelOpt
				// TODO create new getChannel() that throws an error if not existing
				Optional<Channel> channelOpt = thingRepository.getChannel(channelAddress);
				if(channelOpt.isPresent()) {
					Channel channel = channelOpt.get();
					try {
						channel.assertReadAllowed(role);
						JsonElement jValue = JsonUtils.getAsJsonElement(databus.getValue(channel).orElse(null));
						jThingData.add(channelId, jValue);
					} catch (AccessDeniedException | NotImplementedException e) {
						log.error(e.getMessage());
					}
				} else {
					log.error("Channel ["+channelAddress+"] is not existing.");
				}
			}
			jData.add(thingId, jThingData);
		}
		return jData;
	}
}
