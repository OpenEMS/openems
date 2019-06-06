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
package io.openems.core.utilities.websocket;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.persistence.QueryablePersistence;
import io.openems.api.security.User;
import io.openems.common.exceptions.AccessDeniedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;
import io.openems.common.websocket.DefaultMessages;
import io.openems.common.websocket.LogBehaviour;
import io.openems.common.websocket.Notification;
import io.openems.common.websocket.WebSocketUtils;
import io.openems.core.Config;
import io.openems.core.ConfigFormat;
import io.openems.core.ThingRepository;
import io.openems.core.utilities.ConfigUtils;
import io.openems.core.utilities.LinuxCommand;
import io.openems.core.utilities.api.ApiWorker;
import io.openems.core.utilities.api.WriteJsonObject;
import io.openems.core.utilities.api.WriteObject;
import io.openems.impl.persistence.influxdb.TimedataService;

/**
 * Handles a Websocket connection to a browser, OpenEMS backend,...
 *
 * @author stefan.feilmeier
 */
public class EdgeWebsocketHandler {

	private Logger log = LoggerFactory.getLogger(EdgeWebsocketHandler.class);

	/**
	 * Holds the websocket connection
	 */
	protected final WebSocket websocket;

	/**
	 * Holds subscriber to current data
	 */
	private Optional<EdgeCurrentDataWorker> currentDataWorkerOpt = Optional.empty();

	/**
	 * Holds subscribers to system log (identified by messageId.backend, holds complete jMessageId)
	 */
	private final Map<String, JsonObject> logSubscribers = new HashMap<>();

	/**
	 * User for this connection.
	 */
	private Optional<User> userOpt = Optional.empty();

	/**
	 * ApiWorker handles updates to WriteChannels
	 */
	private Optional<ApiWorker> apiWorkerOpt = Optional.empty();

	public EdgeWebsocketHandler(WebSocket websocket) {
		this.websocket = websocket;
	}

	public EdgeWebsocketHandler(WebSocket websocket, ApiWorker apiWorker) {
		this(websocket);
		this.apiWorkerOpt = Optional.ofNullable(apiWorker);
	}

	public synchronized void dispose() {
		if (this.currentDataWorkerOpt.isPresent()) {
			this.currentDataWorkerOpt.get().dispose();
			this.currentDataWorkerOpt = Optional.empty();
		}
		this.logSubscribers.clear();
		this.websocket.close();
	}

	public synchronized void setUser(User user) {
		this.setUser(Optional.ofNullable(user));
	}

	public synchronized void unsetUser() {
		this.setUser(Optional.empty());
	}

	public synchronized void setUser(Optional<User> userOpt) {
		this.userOpt = userOpt;
		if (this.currentDataWorkerOpt.isPresent()) {
			this.currentDataWorkerOpt.get().dispose();
			this.currentDataWorkerOpt = Optional.empty();
		}
		if (userOpt.isPresent()) {
			Role role = userOpt.get().getRole();
			this.currentDataWorkerOpt = Optional.of(new EdgeCurrentDataWorker(this, websocket, role));
		}
	}

	public Optional<User> getUserOpt() {
		return userOpt;
	}

	/**
	 * Handles a message from Websocket.
	 *
	 * @param jMessage
	 */
	public final void onMessage(JsonObject jMessage) {
		if (!this.userOpt.isPresent()) {
			log.error("No User! Aborting...");
			return;
		}
		Role role = this.userOpt.get().getRole();

		// get MessageId from message -> used for reply
		Optional<JsonObject> jMessageIdOpt = JsonUtils.getAsOptionalJsonObject(jMessage, "messageId");

		if (jMessageIdOpt.isPresent()) {
			JsonObject jMessageId = jMessageIdOpt.get();
			/*
			 * Config
			 */
			Optional<JsonObject> jConfigOpt = JsonUtils.getAsOptionalJsonObject(jMessage, "config");
			if (jConfigOpt.isPresent()) {
				this.config(role, jMessageId, apiWorkerOpt, jConfigOpt.get());
				return;
			}

			/*
			 * Subscribe to currentData
			 */
			Optional<JsonObject> jCurrentDataOpt = JsonUtils.getAsOptionalJsonObject(jMessage, "currentData");
			if (jCurrentDataOpt.isPresent()) {
				this.currentData(jMessageId, jCurrentDataOpt.get());
				return;
			}

			/*
			 * Query historic data
			 */
			Optional<JsonObject> jhistoricDataOpt = JsonUtils.getAsOptionalJsonObject(jMessage, "historicData");
			if (jhistoricDataOpt.isPresent()) {
				this.historicData(jMessageId, jhistoricDataOpt.get());
				return;
			}

			/*
			 * Subscribe to log
			 */
			Optional<JsonObject> jLogOpt = JsonUtils.getAsOptionalJsonObject(jMessage, "log");
			if (jLogOpt.isPresent()) {
				try {
					this.log(role, jMessageId, jLogOpt.get());
				} catch (OpenemsException e) {
					WebSocketUtils.sendNotificationOrLogError(this.websocket, jMessageId, LogBehaviour.WRITE_TO_LOG,
							Notification.UNABLE_TO_SUBSCRIBE_TO_LOG, e.getMessage());
				}
			}

			/*
			 * Remote system control
			 */
			Optional<JsonObject> jSystemOpt = JsonUtils.getAsOptionalJsonObject(jMessage, "system");
			if (jSystemOpt.isPresent()) {
				try {
					this.system(jMessageId, jSystemOpt.get(), role);
				} catch (OpenemsException e) {
					WebSocketUtils.sendNotificationOrLogError(this.websocket, jMessageId, LogBehaviour.WRITE_TO_LOG,
							Notification.UNABLE_TO_EXECUTE_SYSTEM_COMMAND, e.getMessage());
				}
			}
		}
	}

	private void historicData(JsonObject jMessageId, JsonObject jHistoricData) {
		// select first QueryablePersistence (by default the running InfluxdbPersistence)
		TimedataService timedataSource = null;
		for (QueryablePersistence queryablePersistence : ThingRepository.getInstance().getQueryablePersistences()) {
			timedataSource = queryablePersistence;
			break;
		}
		if (timedataSource == null) {
			WebSocketUtils.sendNotificationOrLogError(this.websocket, new JsonObject(), LogBehaviour.WRITE_TO_LOG,
					Notification.NO_TIMEDATA_SOURCE_AVAILABLE);
			return;
		}
		JsonArray jData;
		try {
			jData = timedataSource.queryHistoricData(jHistoricData);
			WebSocketUtils.send(this.websocket, DefaultMessages.historicDataQueryReply(jMessageId, jData));
		} catch (OpenemsException e) {
			WebSocketUtils.sendNotificationOrLogError(this.websocket, jMessageId, LogBehaviour.WRITE_TO_LOG,
					Notification.UNABLE_TO_QUERY_HISTORIC_DATA, e.getMessage());
		}
		return;
	}

	/**
	 * Handle "config" messages
	 *
	 * @param jConfig
	 * @return
	 */
	private synchronized void config(Role role, JsonObject jMessageId, Optional<ApiWorker> apiWorkerOpt,
			JsonObject jConfig) {
		Optional<String> modeOpt = JsonUtils.getAsOptionalString(jConfig, "mode");
		switch (modeOpt.orElse("")) {
		case "query":
			/*
			 * Query current config
			 */
			try {
				String language = JsonUtils.getAsString(jConfig, "language");
				JsonObject jReplyConfig = Config.getInstance().getJson(ConfigFormat.OPENEMS_UI, role, language);
				WebSocketUtils.send(this.websocket, DefaultMessages.configQueryReply(jMessageId, jReplyConfig));
				return;
			} catch (OpenemsException e) {
				WebSocketUtils.sendNotificationOrLogError(this.websocket, jMessageId, LogBehaviour.WRITE_TO_LOG,
						Notification.UNABLE_TO_READ_CURRENT_CONFIG, e.getMessage());
			}

		case "update":
			/*
			 * Update thing/channel config
			 */
			Optional<String> thingIdOpt = JsonUtils.getAsOptionalString(jConfig, "thing");
			Optional<String> channelIdOpt = JsonUtils.getAsOptionalString(jConfig, "channel");
			try {
				String thingId = thingIdOpt.get();
				String channelId = channelIdOpt.get();
				JsonElement jValue = JsonUtils.getSubElement(jConfig, "value");
				Optional<Channel> channelOpt = ThingRepository.getInstance().getChannel(thingId, channelId);
				if (channelOpt.isPresent()) {
					Channel channel = channelOpt.get();
					// check write permissions
					channel.assertWriteAllowed(role);
					if (channel instanceof ConfigChannel<?>) {
						/*
						 * ConfigChannel
						 */
						ConfigChannel<?> configChannel = (ConfigChannel<?>) channel;
						Object value = ConfigUtils.getConfigObject(configChannel, jValue);
						configChannel.updateValue(value, true);
						WebSocketUtils.sendNotificationOrLogError(this.websocket, jMessageId, LogBehaviour.WRITE_TO_LOG,
								Notification.EDGE_CHANNEL_UPDATE_SUCCESS, channel.address() + " => " + jValue);

					} else if (channel instanceof WriteChannel<?>) {
						/*
						 * WriteChannel
						 */
						WriteChannel<?> writeChannel = (WriteChannel<?>) channel;
						if (!apiWorkerOpt.isPresent()) {
							WebSocketUtils.sendNotificationOrLogError(this.websocket, jMessageId,
									LogBehaviour.WRITE_TO_LOG, Notification.BACKEND_NOT_ALLOWED,
									"set " + channel.address() + " => " + jValue);
						} else {
							ApiWorker apiWorker = apiWorkerOpt.get();
							WriteObject writeObject = new WriteJsonObject(jValue).onFirstSuccess(() -> {
								WebSocketUtils.sendNotificationOrLogError(this.websocket, jMessageId,
										LogBehaviour.WRITE_TO_LOG, Notification.EDGE_CHANNEL_UPDATE_SUCCESS,
										"set " + channel.address() + " => " + jValue);
							}).onFirstError((e) -> {
								WebSocketUtils.sendNotificationOrLogError(this.websocket, jMessageId,
										LogBehaviour.WRITE_TO_LOG, Notification.EDGE_CHANNEL_UPDATE_FAILED,
										"set " + channel.address() + " => " + jValue, e.getMessage());
							}).onTimeout(() -> {
								WebSocketUtils.sendNotificationOrLogError(this.websocket, jMessageId,
										LogBehaviour.WRITE_TO_LOG, Notification.EDGE_CHANNEL_UPDATE_TIMEOUT,
										"set " + channel.address() + " => " + jValue);
							});
							apiWorker.addValue(writeChannel, writeObject);
						}
					}
				} else {
					WebSocketUtils.sendNotificationOrLogError(this.websocket, jMessageId, LogBehaviour.WRITE_TO_LOG,
							Notification.CHANNEL_NOT_FOUND, thingId + "/" + channelId);
				}
			} catch (NoSuchElementException | OpenemsException e) {
				WebSocketUtils.sendNotificationOrLogError(this.websocket, jMessageId, LogBehaviour.WRITE_TO_LOG,
						Notification.EDGE_CHANNEL_UPDATE_FAILED,
						thingIdOpt.orElse("UNDEFINED") + "/" + channelIdOpt.orElse("UNDEFINED"), e.getMessage());
			}
		}
	}

	// TODO implement creation of new things
	// /*
	// * Create new Thing
	// */
	// JsonObject jObject = JsonUtils.getAsJsonObject(jConfig, "object");
	// String parentId = JsonUtils.getAsString(jConfig, "parent");
	// String thingId = JsonUtils.getAsString(jObject, "id");
	// if (thingId.startsWith("_")) {
	// throw new ConfigException("IDs starting with underscore are reserved for internal use.");
	// }
	// if (thingRepository.getThingById(thingId).isPresent()) {
	// throw new ConfigException("Thing Id is already existing.");
	// }
	// String clazzName = JsonUtils.getAsString(jObject, "class");
	// Class<?> clazz = Class.forName(clazzName);
	// if (Device.class.isAssignableFrom(clazz)) {
	// // Device
	// Thing parentThing = thingRepository.getThing(parentId);
	// if (parentThing instanceof Bridge) {
	// Bridge parentBridge = (Bridge) parentThing;
	// Device device = thingRepository.createDevice(jObject);
	// parentBridge.addDevice(device);
	// Config.getInstance().writeConfigFile();
	// Notification.send(NotificationType.SUCCESS, "Device [" + device.id() + "] wurde erstellt.");
	// break;
	// }
	// }
	// } else if (mode.equals("delete")) {
	// TODO implement deletion of things
	// /*
	// * Delete a Thing
	// */
	// String thingId = JsonUtils.getAsString(jConfig, "thing");
	// thingRepository.removeThing(thingId);
	// Config.getInstance().writeConfigFile();
	// Notification.send(NotificationType.SUCCESS, "Controller [" + thingId + "] wurde " + " gel�scht.");
	// } else {
	// throw new OpenemsException("Modus [" + mode + "] ist nicht implementiert.");
	// }
	// }
	// TODO send new config after config update
	// // Send new config
	// JsonObject jMetadata = new JsonObject();
	// jMetadata.add("config", Config.getInstance().getMetaConfigJson());
	// JsonObject j = new JsonObject();
	// j.add("metadata", jMetadata);
	// WebSocketUtils.send(this.websocket, j);
	// } catch (OpenemsException | ClassNotFoundException e) {
	// Notification.send(NotificationType.ERROR, e.getMessage());
	// }
	// }

	/**
	 * Handle current data subscriptions
	 * (try to keep synced with Backend.BrowserWebsocket)
	 *
	 * @param j
	 */
	private synchronized JsonObject currentData(JsonObject jMessageId, JsonObject jCurrentData) {
		try {
			String mode = JsonUtils.getAsString(jCurrentData, "mode");

			if (mode.equals("subscribe")) {
				/*
				 * Subscribe to channels
				 */
				if (!this.currentDataWorkerOpt.isPresent()) {
					throw new OpenemsException("No CurrentDataWorker available");
				}
				JsonObject jSubscribeChannels = JsonUtils.getAsJsonObject(jCurrentData, "channels");
				this.currentDataWorkerOpt.get().setChannels(jSubscribeChannels, jMessageId);
			}
		} catch (OpenemsException e) {
			log.warn(e.getMessage());
		}
		return new JsonObject();
	}

	/**
	 * Handle system log subscriptions
	 *
	 * @param j
	 * @throws AccessDeniedException
	 */
	private synchronized void log(Role role, JsonObject jMessageId, JsonObject jLog) throws OpenemsException {
		// check permissions
		switch (role) {
		case ADMIN:
		case INSTALLER:
		case OWNER:
			/* allowed */
			break;
		case GUEST:
		default:
			throw new AccessDeniedException("User role [" + role + "] is not allowed to read system logs.");
		}

		String mode = JsonUtils.getAsString(jLog, "mode");
		String messageId = JsonUtils.getAsOptionalString(jMessageId, "backend")
				.orElse(JsonUtils.getAsString(jMessageId, "ui"));

		if (mode.equals("subscribe")) {
			/*
			 * Subscribe to system log
			 */
			log.info("UI [" + messageId + "] subscribed to log.");
			this.logSubscribers.put(messageId, jMessageId);
		} else if (mode.equals("unsubscribe")) {
			/*
			 * Unsubscribe from system log
			 */
			log.info("UI [" + messageId + "] unsubscribed from log.");
			this.logSubscribers.remove(messageId);
		}
	}

	/**
	 * Handle remote system control
	 *
	 * @param j
	 * @throws OpenemsException
	 */
	private synchronized void system(JsonObject jMessageId, JsonObject jSystem, Role role) throws OpenemsException {
		if (!(role == Role.ADMIN)) {
			throw new AccessDeniedException("User role [" + role + "] is not allowed to execute system commands.");
		}
		String output = "";
		String mode = JsonUtils.getAsString(jSystem, "mode");
		String password = JsonUtils.getAsString(jSystem, "password");
		String command = JsonUtils.getAsString(jSystem, "command");
		boolean background = JsonUtils.getAsBoolean(jSystem, "background");
		int timeout = JsonUtils.getAsInt(jSystem, "timeout");

		if (mode.equals("execute")) {
			log.info("UI [" + jMessageId + "] executes system command [" + command + "] background [" + background
					+ "] timeout [" + timeout + "]");
			output = LinuxCommand.execute(password, command, background, timeout);
		}
		WebSocketUtils.send(this.websocket, DefaultMessages.systemExecuteReply(jMessageId, output));
	}

	/**
	 * Send a message to the websocket.
	 *
	 * @param message
	 * @throws OpenemsException
	 */
	public void send(JsonObject j) throws OpenemsException {
		WebSocketUtils.send(this.websocket, j);
	}

	/**
	 * Send a message to the websocket.
	 *
	 * @param message
	 */
	public void sendOrLogError(JsonObject j) {
		WebSocketUtils.sendOrLogError(this.websocket, j);
	}

	/**
	 * Send a log message to the websocket. This method is called by logback
	 *
	 * @param message2
	 * @param timestamp
	 */
	public void sendLog(long timestamp, String level, String source, String message) {
		if (this.logSubscribers.isEmpty()) {
			// nobody subscribed
			return;
		}
		for (Entry<String, JsonObject> entry : this.logSubscribers.entrySet()) {
			JsonObject j = DefaultMessages.log(entry.getValue(), timestamp, level, source, message);
			try {
				this.send(j);
			} catch (OpenemsException e) {
				// Error while sending: remove subscriber
				log.error("Error while sending log. Removing subscriber [" + entry.getKey() + "]");
				this.logSubscribers.remove(entry.getKey());
			}
		}
	}

	@Deprecated
	public WebSocket getWebsocket() {
		return this.websocket;
	}

}
