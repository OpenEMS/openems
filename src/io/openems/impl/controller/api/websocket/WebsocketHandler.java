package io.openems.impl.controller.api.websocket;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.api.device.nature.DeviceNature;
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
			send(j);
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
		JsonObject jNatures = new JsonObject();
		thingRepository.getDeviceNatures().forEach(nature -> {
			JsonArray jNatureClasses = new JsonArray();
			/*
			 * get important classes/interfaces that are implemented by this nature
			 */
			for (Class<?> iface : getImportantNatureInterfaces(nature.getClass())) {
				jNatureClasses.add(iface.getSimpleName());
			}
			jNatures.add(nature.id(), jNatureClasses);

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
		 * Send data
		 */
		JsonObject jSend = new JsonObject();
		jSend.add("natures", jNatures);
		JsonObject jData = getData();
		jSend.add("data", jData);
		send(jSend);

		/*
		 * Execute regular task
		 */
		if (future != null) {
			future.cancel(true);
		}
		future = executor.scheduleWithFixedDelay(this.task, 0, 3, TimeUnit.SECONDS);
	}

	/**
	 * Gets all important nature super interfaces and classes. This data is used by web client to visualize the data
	 * appropriately
	 *
	 * @param clazz
	 * @return
	 */
	private Set<Class<?>> getImportantNatureInterfaces(Class<?> clazz) {
		Set<Class<?>> ifaces = new HashSet<>();
		if (clazz == null // at the top
				|| clazz.equals(DeviceNature.class) // we are at the DeviceNature interface
				|| !DeviceNature.class.isAssignableFrom(clazz) // clazz is not derived from DeviceNature
		) {
			return ifaces;
		}
		// myself
		ifaces.add(clazz);
		// super interfaces
		for (Class<?> iface : clazz.getInterfaces()) {
			if (DeviceNature.class.isAssignableFrom(iface)) {
				ifaces.addAll(getImportantNatureInterfaces(iface));
			}
		}
		// super classes
		ifaces.addAll(getImportantNatureInterfaces(clazz.getSuperclass()));
		return ifaces;
	}

	/**
	 * Send a message to the websocket
	 *
	 * @param j
	 * @return true if successful, otherwise false
	 */
	private boolean send(JsonObject j) {
		try {
			this.conn.send(j.toString());
			return true;
		} catch (WebsocketNotConnectedException e) {
			return false;
		}
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
