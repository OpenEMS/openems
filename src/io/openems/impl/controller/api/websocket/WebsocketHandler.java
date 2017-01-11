package io.openems.impl.controller.api.websocket;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.api.device.nature.ess.AsymmetricEssNature;
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.device.nature.ess.SymmetricEssNature;
import io.openems.api.device.nature.meter.AsymmetricMeterNature;
import io.openems.api.device.nature.meter.SymmetricMeterNature;
import io.openems.api.exception.NotImplementedException;
import io.openems.api.security.Session;
import io.openems.api.thing.Thing;
import io.openems.core.Databus;
import io.openems.core.ThingRepository;
import io.openems.core.utilities.JsonUtils;

public class WebsocketHandler {
	private static Logger log = LoggerFactory.getLogger(WebsocketHandler.class);

	/**
	 * Holds the websocket
	 */
	private final WebSocket conn;

	/**
	 * Holds the authenticated session
	 */
	private Session session = null;

	/**
	 * Holds thingId and channelId, subscribed by this websocket
	 */
	private final HashMultimap<String, String> subscribedChannels;

	private final ThingRepository thingRepository;
	private final Databus databus;

	private static ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
	private final Runnable task;
	private ScheduledFuture<?> future = null;

	public WebsocketHandler(WebSocket conn) {
		this.conn = conn;
		this.subscribedChannels = HashMultimap.create();
		this.thingRepository = ThingRepository.getInstance();
		this.databus = Databus.getInstance();
		this.task = () -> {
			/*
			 * This task is executed regularly. Sends data to websocket.
			 */
			JsonObject j = new JsonObject();
			JsonObject jData = getData();
			j.add("data", jData);
			send(true, j);
		};
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public Session getSession() {
		return session;
	}

	public WebSocket getWebSocket() {
		return conn;
	}

	public boolean isValid() {
		if (this.session != null && this.session.isValid()) {
			return true;
		}
		return false;
	}

	/**
	 * Gets a json object with all subscribed channels
	 *
	 * @return
	 */
	private JsonObject getData() {
		JsonObject jData = new JsonObject();
		subscribedChannels.keys().forEach(thingId -> {
			JsonObject jThingData = new JsonObject();
			subscribedChannels.get(thingId).forEach(channelId -> {
				Optional<?> value = databus.getValue(thingId, channelId);
				JsonElement jValue;
				try {
					jValue = JsonUtils.getAsJsonElement(value.orElse(null));
					jThingData.add(channelId, jValue);
				} catch (NotImplementedException e) {
					log.error(e.getMessage());
				}
			});
			jData.add(thingId, jThingData);
		});
		return jData;
	}

	public void addSubscribedChannels(String tag) {
		if (tag.equals("")) {
			unsubscribe();
		} else if (tag.equals("fenecon_monitor_v1")) {
			subscribeFeneconMonitorV1();
		} else {
			log.warn("User[" + getUserName() + "]: subscribe-tag [" + tag + "] is not implemented.");
			unsubscribe();
		}
	}

	private void subscribe(Thing thing, String... channelIds) {
		subscribedChannels.putAll(thing.id(), Arrays.asList(channelIds));
	}

	private void unsubscribe() {
		// unsubscribe regular task
		if (future != null) {
			future.cancel(true);
		}
		// clear subscriptions
		subscribedChannels.clear();
	}

	private void subscribeFeneconMonitorV1() {
		log.info("User[" + getUserName() + "]: subscribed to FENECON Monitor v1");
		thingRepository.getDeviceNatures().forEach(nature -> {
			/*
			 * Subscribe to channels
			 */
			if (nature instanceof EssNature) {
				subscribe(nature, "Soc", "SystemState", "GridMode", "Warning");
			}
			if (nature instanceof SymmetricEssNature) {
				subscribe(nature, "ActivePower", "ReactivePower");
			}
			if (nature instanceof AsymmetricEssNature) {
				subscribe(nature, "ActivePowerL1", "ActivePowerL2", "ActivePowerL3");
			}
			if (nature instanceof SymmetricMeterNature) {
				subscribe(nature, "ActivePower", "ReactivePower");
			}
			if (nature instanceof AsymmetricMeterNature) {
				subscribe(nature, "ActivePowerL1", "ActivePowerL2", "ActivePowerL3");
			}
		});

		/*
		 * Execute regular task
		 */
		if (future != null) {
			future.cancel(true);
		}
		future = executor.scheduleWithFixedDelay(this.task, 0, 3, TimeUnit.SECONDS);
	}

	/**
	 * Send a message to the websocket
	 *
	 * @param key
	 * @param j
	 * @return true if successful, otherwise false
	 */
	public synchronized boolean send(boolean asDevice, String key, JsonElement j) {
		JsonObject jSend = new JsonObject();
		jSend.add(key, j);
		return send(asDevice, jSend);
	}

	/**
	 * Send a message to the websocket
	 *
	 * @param j
	 * @return true if successful, otherwise false
	 */
	public synchronized boolean send(boolean asDevice, JsonObject j) {
		JsonObject jSend;
		if (asDevice) {
			jSend = new JsonObject();
			JsonObject jDevices = new JsonObject();
			jDevices.add(WebsocketServer.DEFAULT_DEVICE_NAME, j);
			jSend.add("devices", jDevices);
		} else {
			jSend = j;
		}
		try {
			this.conn.send(jSend.toString());
			return true;
		} catch (WebsocketNotConnectedException e) {
			return false;
		}
	}

	/**
	 * Send a notification message/error to the websocket
	 *
	 * @param mesage
	 * @return true if successful, otherwise false
	 */
	public synchronized boolean sendNotification(NotificationType type, String message) {
		JsonObject jMessage = new JsonObject();
		jMessage.addProperty("type", type.name().toLowerCase());
		jMessage.addProperty("message", message);
		return send(true, "notification", jMessage);
	}

	/**
	 * Gets the user name of this user, avoiding null
	 *
	 * @param conn
	 * @return
	 */
	public String getUserName() {
		if (session != null && session.getUser() != null) {
			return session.getUser().getName();
		}
		return "NOT_CONNECTED";
	}
}
