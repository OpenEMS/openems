/*******************************************************************************
 * OpenEMS - Open Source Energy Management System
 * Copyright (c) 2016, 2017 FENECON GmbH and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *   FENECON GmbH - initial API and implementation and initial documentation
 *******************************************************************************/
package io.openems.impl.persistence.fenecon;

import java.net.Inet4Address;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.HashMultimap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelChangeListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.persistence.Persistence;
import io.openems.api.thing.Thing;
import io.openems.common.types.FieldValue;
import io.openems.common.types.NullFieldValue;
import io.openems.common.types.NumberFieldValue;
import io.openems.common.types.StringFieldValue;
import io.openems.common.websocket.DefaultMessages;
import io.openems.common.websocket.WebSocketUtils;
import io.openems.core.Databus;
import io.openems.core.ThingRepository;

@ThingInfo(title = "FENECON Persistence", description = "Establishes the connection to FENECON Cloud.")
public class FeneconPersistence extends Persistence implements ChannelChangeListener {

	/*
	 * Config
	 */
	@ConfigInfo(title = "Apikey", description = "Sets the apikey for FENECON Cloud.", type = String.class)
	public final ConfigChannel<String> apikey = new ConfigChannel<String>("apikey", this).doNotPersist();

	@ConfigInfo(title = "Uri", description = "Sets the connection Uri to FENECON Cloud.", type = String.class, defaultValue = "\"wss://fenecon.de:443/femsserver\"")
	public final ConfigChannel<String> uri = new ConfigChannel<String>("uri", this).doNotPersist();

	private ConfigChannel<Integer> cycleTime = new ConfigChannel<Integer>("cycleTime", this)
			.defaultValue(DEFAULT_CYCLETIME);

	@Override
	public ConfigChannel<Integer> cycleTime() {
		return cycleTime;
	}

	/*
	 * Fields
	 */
	private static final int DEFAULT_CYCLETIME = 2000;
	// Queue of data for the next cycle
	private HashMultimap<Long, FieldValue<?>> queue = HashMultimap.create();
	// Unsent queue (FIFO)
	private EvictingQueue<JsonObject> unsentCache = EvictingQueue.create(1000);
	private volatile WebsocketClient websocketClient;
	private volatile Integer configuredCycleTime = DEFAULT_CYCLETIME;

	/*
	 * Methods
	 */
	/**
	 * Receives update events for all {@link ReadChannel}s, excluding {@link ConfigChannel}s via the {@link Databus}.
	 */
	@Override
	public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
		// Update cycleTime of FENECON Persistence
		if (channel == cycleTime) {
			this.configuredCycleTime = cycleTime.valueOptional().orElse(DEFAULT_CYCLETIME);
		}
		this.addChannelValueToQueue(channel, newValue);
	}

	@Override
	protected void forever() {
		// Convert FieldVales in queue to JsonObject
		JsonObject j;
		synchronized (queue) {
			j = DefaultMessages.timestampedData(queue);
			queue.clear();
		}

		// Send data to Server
		if (this.send(j)) {
			// Successful

			// reset cycleTime
			resetCycleTime();

			// resend from cache
			for (Iterator<JsonObject> iterator = unsentCache.iterator(); iterator.hasNext();) {
				JsonObject jCached = iterator.next();
				boolean cacheWasSent = this.send(jCached);
				if (cacheWasSent) {
					iterator.remove();
				}
			}
		} else {
			// Failed to send

			// increase cycleTime
			increaseCycleTime();

			// cache data for later
			unsentCache.add(j);
		}
	}

	@Override
	protected void dispose() {
		if (this.websocketClient != null) {
			this.websocketClient.close();
		}
	}

	/**
	 * Send message to websocket
	 *
	 * @param j
	 * @return
	 */
	private boolean send(JsonObject j) {
		Optional<FeneconPersistenceWebsocketHandler> websocketHandler = getWebsocketHandler();
		return websocketHandler.isPresent() && WebSocketUtils.send(websocketHandler.get().websocket, j);
	}

	/**
	 * Gets the websocket handler
	 *
	 * @return
	 */
	public Optional<FeneconPersistenceWebsocketHandler> getWebsocketHandler() {
		Optional<WebsocketClient> websocketClient = getWebsocketClient();
		if (websocketClient.isPresent()) {
			return Optional.of(websocketClient.get().getWebsocketHandler());
		}
		return Optional.empty();
	}

	/**
	 * Gets the websocket client
	 *
	 * @return
	 */
	private AtomicBoolean isAlreadyConnecting = new AtomicBoolean(false);

	public Optional<WebsocketClient> getWebsocketClient() {
		// return existing and opened websocket
		if (this.websocketClient != null && this.websocketClient.getConnection().isOpen()) {
			return Optional.of(this.websocketClient);
		}
		this.websocketClient = null;
		// check config
		if (!this.apikey.valueOptional().isPresent() || !this.uri.valueOptional().isPresent()) {
			return Optional.empty();
		}
		// a connection is already requested -> return null
		// from here only one thread is allowed to enter
		if (isAlreadyConnecting.getAndSet(true)) {
			return Optional.empty();
		}
		String uri = this.uri.valueOptional().get();
		String apikey = this.apikey.valueOptional().get();
		// Try to connect in asynchronous thread
		Runnable task = () -> {
			WebsocketClient newWebsocketClient = null;
			try {
				// create new websocket
				// TODO: check server certificate
				newWebsocketClient = new WebsocketClient(new URI(uri), apikey);
				log.info("FENECON persistence is connecting... [" + uri + "]");
				if (newWebsocketClient.connectBlocking(10)) {
					// successful -> return connected websocket
					log.info("FENECON persistence connected [" + uri + "]");
					// Add current status of all channels to queue
					this.addCurrentValueOfAllChannelsToQueue();
				} else {
					// not connected -> return empty
					log.warn("FENECON persistence failed connection to uri [" + uri + "]");
					newWebsocketClient = null;
				}
			} catch (URISyntaxException e) {
				log.error("Invalid uri: " + e.getMessage());
				// newWebsocketClient = null;
			} catch (InterruptedException e) {
				log.warn("Websocket connection interrupted: " + e.getMessage());
				newWebsocketClient = null;
			} catch (Exception e) {
				log.warn("Websocket exception: " + e.getMessage());
				newWebsocketClient = null;
			}
			this.websocketClient = newWebsocketClient;
			isAlreadyConnecting.set(false);
		};
		Thread thread = new Thread(task);
		thread.start();
		// while connecting -> still returning null
		return Optional.empty();
	}

	private void increaseCycleTime() {
		int currentCycleTime = this.cycleTime().valueOptional().orElse(DEFAULT_CYCLETIME);
		int newCycleTime;
		if (currentCycleTime < 30000 /* 30 seconds */) {
			newCycleTime = currentCycleTime * 2;
		} else {
			newCycleTime = currentCycleTime;
		}
		if (currentCycleTime != newCycleTime) {
			this.cycleTime().updateValue(newCycleTime, false);
		}
	}

	/**
	 * Cycletime is adjusted if connection to Backend fails. This method resets it to configured or default value.
	 */
	private void resetCycleTime() {
		int currentCycleTime = this.cycleTime().valueOptional().orElse(DEFAULT_CYCLETIME);
		int newCycleTime = this.configuredCycleTime;
		this.cycleTime().updateValue(newCycleTime, false);
		if (currentCycleTime != newCycleTime) {
			this.cycleTime().updateValue(newCycleTime, false);
		}
	}

	@Override
	protected boolean initialize() {
		boolean successful = getWebsocketClient().isPresent();
		if (!successful) {
			increaseCycleTime();
		}
		return getWebsocketClient().isPresent();
	}

	/**
	 * Add a channel value to the send queue
	 *
	 * @param channel
	 * @param valueOpt
	 */
	private void addChannelValueToQueue(Channel channel) {
		if (!(channel instanceof ReadChannel<?>)) {
			// TODO check for more types - see other addChannelValueToQueue method
			return;
		}
		ReadChannel<?> readChannel = (ReadChannel<?>) channel;
		this.addChannelValueToQueue(channel, readChannel.valueOptional());
	}

	/**
	 * Add a channel value to the send queue
	 *
	 * @param channel
	 * @param valueOpt
	 */
	private void addChannelValueToQueue(Channel channel, Optional<?> valueOpt) {
		// Ignore anything that is not a ReadChannel
		if (!(channel instanceof ReadChannel<?>)) {
			return;
		}
		ReadChannel<?> readChannel = (ReadChannel<?>) channel;
		// Ignore channels that shall not be persisted
		if (readChannel.isDoNotPersist()) {
			return;
		}

		// Get timestamp and round to seconds
		Long timestamp = System.currentTimeMillis() / 1000 * 1000;

		// Read and format value from channel
		String field = readChannel.address();
		FieldValue<?> fieldValue;
		if (!valueOpt.isPresent()) {
			fieldValue = new NullFieldValue(field);
		} else {
			Object value = valueOpt.get();
			if (value instanceof Number) {
				fieldValue = new NumberFieldValue(field, (Number) value);
			} else if (value instanceof String) {
				fieldValue = new StringFieldValue(field, (String) value);
			} else if (value instanceof Inet4Address) {
				fieldValue = new StringFieldValue(field, ((Inet4Address) value).getHostAddress());
			} else if (value instanceof Boolean) {
				fieldValue = new NumberFieldValue(field, ((Boolean) value) ? 1 : 0);
			} else if (value instanceof DeviceNature || value instanceof JsonElement || value instanceof Map) {
				// ignore
				return;
			} else {
				log.warn("FENECON Persistence for value type [" + value.getClass().getName() + "] of channel ["
						+ channel.address() + "] is not implemented.");
				return;
			}
		}

		// Add timestamp + value to queue
		synchronized (queue) {
			queue.put(timestamp, fieldValue);
		}
	}

	/**
	 * On websocket open, add current values of all channels to queue. This is to prepare upcoming "channelChanged"
	 * events, where only changes are sent
	 */
	private void addCurrentValueOfAllChannelsToQueue() {
		ThingRepository thingRepository = ThingRepository.getInstance();
		for (Thing thing : thingRepository.getThings()) {
			for (Channel channel : thingRepository.getChannels(thing)) {
				this.addChannelValueToQueue(channel);
			}
		}
	}
}
