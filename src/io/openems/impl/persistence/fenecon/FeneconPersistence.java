package io.openems.impl.persistence.fenecon;

import java.net.Inet4Address;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.HashMultimap;
import com.google.gson.JsonObject;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelChangeListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.persistence.Persistence;
import io.openems.core.Config;
import io.openems.core.Databus;

public class FeneconPersistence extends Persistence implements ChannelChangeListener {

	/*
	 * Config
	 */
	@ConfigInfo(title = "Sets the 'apikey' for FENECON Cloud", type = String.class)
	public final ConfigChannel<String> apikey = new ConfigChannel<String>("apikey", this);

	@ConfigInfo(title = "Sets the connection URI", type = String.class)
	public final ConfigChannel<String> uri = new ConfigChannel<String>("uri", this)
			.defaultValue("wss://fenecon.de:443/femsserver");

	private HashMultimap<Long, FieldValue<?>> queue = HashMultimap.create();

	private List<JsonObject> unsentCache = new ArrayList<>();

	private WebsocketClient _websocket;

	private static final int DEFAULT_CYCLETIME = 2000;

	@ConfigInfo(title = "Sets the duration of each cycle in milliseconds", type = Integer.class)
	private ConfigChannel<Integer> cycleTime = new ConfigChannel<Integer>("cycleTime", this)
			.defaultValue(DEFAULT_CYCLETIME);

	@Override
	@ConfigInfo(title = "Sets the duration of each cycle in milliseconds", type = Integer.class)
	public ConfigChannel<Integer> cycleTime() {
		return cycleTime;
	}

	private volatile int currentCycleTime = DEFAULT_CYCLETIME;

	/**
	 * Receives update events for all {@link ReadChannel}s, excluding {@link ConfigChannel}s via the {@link Databus}.
	 */
	@Override
	public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
		if (channel == cycleTime) {
			// Cycle Time
			this.currentCycleTime = cycleTime.valueOptional().orElse(DEFAULT_CYCLETIME);
		}

		if (!(channel instanceof ReadChannel<?>)) {
			return;
		}
		ReadChannel<?> readChannel = (ReadChannel<?>) channel;

		String field = readChannel.address();
		FieldValue<?> fieldValue;
		if (!newValue.isPresent()) {
			fieldValue = new NullFieldValue(field);
		} else {
			Object value = newValue.get();
			if (value instanceof Number) {
				fieldValue = new NumberFieldValue(field, (Number) value);
			} else if (value instanceof String) {
				fieldValue = new StringFieldValue(field, (String) value);
			} else if (value instanceof Inet4Address) {
				fieldValue = new StringFieldValue(field, ((Inet4Address) value).getHostAddress());
			} else {
				log.warn("FENECON Persistence for value type [" + value.getClass().getName() + "] is not implemented.");
				return;
			}
		}
		// Round time to Seconds
		Long timestamp = System.currentTimeMillis() / 1000 * 1000;
		synchronized (queue) {
			queue.put(timestamp, fieldValue);
		}
	}

	@Override
	protected void dispose() {
		if (this._websocket != null) {
			this._websocket.close();
		}
	}

	@Override
	protected void forever() {
		JsonObject jTimedata = new JsonObject();
		/*
		 * Convert FieldVales in queue to JsonObject
		 */
		synchronized (queue) {
			queue.asMap().forEach((timestamp, fieldValues) -> {
				JsonObject jTimestamp = new JsonObject();
				fieldValues.forEach(fieldValue -> {
					if (fieldValue instanceof NumberFieldValue) {
						jTimestamp.addProperty(fieldValue.field, ((NumberFieldValue) fieldValue).value);
					} else if (fieldValue instanceof StringFieldValue) {
						jTimestamp.addProperty(fieldValue.field, ((StringFieldValue) fieldValue).value);
					}
				});
				jTimedata.add(String.valueOf(timestamp), jTimestamp);
			});
			queue.clear();
		}
		// build Json
		JsonObject j = new JsonObject();
		j.add("timedata", jTimedata);
		/*
		 * Send to Server
		 */
		Optional<WebsocketClient> ws = getWebsocketClient();
		if (ws.isPresent() && ws.get().send(j)) {
			/*
			 * Sent successfully
			 */
			// reset cycleTime
			this.currentCycleTime = cycleTime.valueOptional().orElse(DEFAULT_CYCLETIME);

			// resend from cache
			for (Iterator<JsonObject> iterator = unsentCache.iterator(); iterator.hasNext();) {
				JsonObject jCachedTimedata = iterator.next();
				JsonObject jCached = new JsonObject();
				jCached.add("timedata", jCachedTimedata);
				boolean cacheWasSent = this._websocket.send(jCached);
				if (cacheWasSent) {
					iterator.remove();
				}
			}
		} else {
			/*
			 * Unable to send
			 */
			// increase cycleTime
			increaseCycleTime();

			// cache data for later
			unsentCache.add(jTimedata);
		}
	}

	Optional<WebsocketClient> getWebsocketClient() {
		WebsocketClient ws = this._websocket;

		// return existing and opened websocket
		if (ws != null && ws.getConnection().isOpen()) {
			return Optional.of(ws);
		}
		// check config
		if (!this.apikey.valueOptional().isPresent() || !this.uri.valueOptional().isPresent()) {
			return Optional.empty();
		}
		String uri = this.uri.valueOptional().get();
		String apikey = this.apikey.valueOptional().get();
		try {
			// create new websocket
			// TODO: check server certificate
			ws = new WebsocketClient(new URI(uri), apikey);
			boolean connected = ws.connectBlocking();
			if (connected) {
				// return connected websocket
				log.info("FENECON persistence connected to uri [" + uri + "]");
				this._websocket = ws;
				// send current OpenEMS config
				JsonObject jConfig = Config.getInstance().getMetaConfigJson();
				JsonObject jMetadata = new JsonObject();
				jMetadata.add("config", jConfig);
				JsonObject j = new JsonObject();
				j.add("metadata", jMetadata);
				ws.send(j);
				return Optional.of(ws);
			} else {
				// not connected -> return empty
				log.warn("FENECON persistence failed connection to uri [" + uri + "]");
				this._websocket = null;
				return Optional.empty();
			}
		} catch (URISyntaxException e) {
			log.error("Invalid uri: " + e.getMessage());
			return Optional.empty();
		} catch (InterruptedException e) {
			log.warn("Websocket connection interrupted: " + e.getMessage());
			return Optional.empty();
		} catch (Exception e) {
			log.warn("Websocket exception: " + e.getMessage());
			return Optional.empty();
		}
	}

	private void increaseCycleTime() {
		// TODO increase max cycle time for production
		if (currentCycleTime < 30000 /* 30 seconds */) {
			currentCycleTime *= 2;
		}
		log.info("New cycle time: " + cycleTime);
	}

	@Override
	protected boolean initialize() {
		boolean successful = getWebsocketClient().isPresent();
		if (!successful) {
			increaseCycleTime();
		}
		return getWebsocketClient().isPresent();
	}
}
