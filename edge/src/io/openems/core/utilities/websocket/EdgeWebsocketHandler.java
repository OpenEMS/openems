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

import com.google.common.collect.HashMultimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.persistence.QueryablePersistence;
import io.openems.backend.timedata.api.TimedataService;
import io.openems.common.exceptions.AccessDeniedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;
import io.openems.common.websocket.CurrentDataWorker;
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
	protected Optional<WebSocket> websocketOpt = Optional.empty();

	/**
	 * Holds subscribers to current data
	 */
	private final HashMap<String, CurrentDataWorker> currentDataSubscribers = new HashMap<>();

	/**
	 * Holds subscribers to system log (identified by messageId.backend, holds complete jMessageId)
	 */
	private final Map<String, JsonObject> logSubscribers = new HashMap<>();

	/**
	 * Predefined role for this connection. If empty, role is taken from message (in onMessage method).
	 */
	private final Optional<Role> roleOpt;

	/**
	 * ApiWorker handles updates to WriteChannels
	 */
	private final Optional<ApiWorker> apiWorkerOpt;

	public EdgeWebsocketHandler() {
		this.apiWorkerOpt = Optional.empty();
		this.roleOpt = Optional.empty();
	}

	public EdgeWebsocketHandler(ApiWorker apiWorker, WebSocket websocket, Role role) {
		this.apiWorkerOpt = Optional.ofNullable(apiWorker);
		this.websocketOpt = Optional.ofNullable(websocket);
		this.roleOpt = Optional.ofNullable(role);
	}

	public void setWebsocket(WebSocket websocket) {
		this.websocketOpt = Optional.ofNullable(websocket);
	}

	public Optional<WebSocket> getWebsocket() {
		return websocketOpt;
	}

	/**
	 * Handles a message from Websocket.
	 *
	 * @param jMessage
	 */
	public final void onMessage(JsonObject jMessage) {
		// get MessageId from message -> used for reply
		Optional<JsonObject> jMessageIdOpt = JsonUtils.getAsOptionalJsonObject(jMessage, "messageId");

		// get role
		// TODO
		Role role;
		if (this.roleOpt.isPresent()) {
			role = this.roleOpt.get();
		} else {
			Optional<String> roleStringOpt = JsonUtils.getAsOptionalString(jMessage, "role");
			if (roleStringOpt.isPresent()) {
				role = Role.getRole(roleStringOpt.get());
			} else {
				role = Role.getDefaultRole();
			}
		}

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
				currentData(role, jMessageId, jCurrentDataOpt.get());
				return;
			}

			/*
			 * Query historic data
			 */
			Optional<JsonObject> jhistoricDataOpt = JsonUtils.getAsOptionalJsonObject(jMessage, "historicData");
			if (jhistoricDataOpt.isPresent()) {
				// select first QueryablePersistence (by default the running InfluxdbPersistence)
				TimedataService timedataSource = null;
				for (QueryablePersistence queryablePersistence : ThingRepository.getInstance()
						.getQueryablePersistences()) {
					timedataSource = queryablePersistence;
					break;
				}
				if (timedataSource == null) {
					// TODO create notification that there is no datasource available
				} else {
					// TODO
					// jReply = JsonUtils.merge(jReply, //
					// WebSocketUtils.historicData(jIdOpt.get(), jhistoricDataOpt.get(), deviceIdOpt, timedataSource,
					// role) //
					// );
				}
				return;
			}

			/*
			 * Subscribe to log
			 */
			Optional<JsonObject> jLogOpt = JsonUtils.getAsOptionalJsonObject(jMessage, "log");
			if (jLogOpt.isPresent()) {
				try {
					log(jMessageId, jLogOpt.get(), role);
				} catch (OpenemsException e) {
					log.error(e.getMessage());
				}
			}

			/*
			 * Remote system control
			 */
			Optional<JsonObject> jSystemOpt = JsonUtils.getAsOptionalJsonObject(jMessage, "system");
			if (jSystemOpt.isPresent()) {
				try {
					system(jMessageId, jSystemOpt.get(), role);
				} catch (OpenemsException e) {
					// TODO create notification
					log.error(e.getMessage());
				}
			}
		}
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
				WebSocketUtils.send(this.websocketOpt, DefaultMessages.configQueryReply(jMessageId, jReplyConfig));
				return;
			} catch (OpenemsException e) {
				// TODO notification
				log.error(e.getMessage());
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
						WebSocketUtils.sendNotificationOrLogError(this.websocketOpt, jMessageId,
								LogBehaviour.WRITE_TO_LOG, Notification.EDGE_CHANNEL_UPDATE_SUCCESS,
								channel.address() + " => " + jValue);

					} else if (channel instanceof WriteChannel<?>) {
						/*
						 * WriteChannel
						 */
						WriteChannel<?> writeChannel = (WriteChannel<?>) channel;
						if (!apiWorkerOpt.isPresent()) {
							WebSocketUtils.sendNotificationOrLogError(this.websocketOpt, new JsonObject() /* TODO */,
									LogBehaviour.WRITE_TO_LOG, Notification.BACKEND_NOT_ALLOWED,
									"set " + channel.address() + " => " + jValue);
						} else {
							ApiWorker apiWorker = apiWorkerOpt.get();
							WriteObject writeObject = new WriteJsonObject(jValue).onFirstSuccess(() -> {
								WebSocketUtils.sendNotificationOrLogError(this.websocketOpt,
										new JsonObject() /* TODO */, LogBehaviour.WRITE_TO_LOG,
										Notification.EDGE_CHANNEL_UPDATE_SUCCESS,
										"set " + channel.address() + " => " + jValue);
							}).onFirstError((e) -> {
								WebSocketUtils.sendNotificationOrLogError(this.websocketOpt,
										new JsonObject() /* TODO */, LogBehaviour.WRITE_TO_LOG,
										Notification.EDGE_CHANNEL_UPDATE_FAILED,
										"set " + channel.address() + " => " + jValue, e.getMessage());
							}).onTimeout(() -> {
								WebSocketUtils.sendNotificationOrLogError(this.websocketOpt,
										new JsonObject() /* TODO */, LogBehaviour.WRITE_TO_LOG,
										Notification.EDGE_CHANNEL_UPDATE_TIMEOUT,
										"set " + channel.address() + " => " + jValue);
							});
							apiWorker.addValue(writeChannel, writeObject);
						}
					}
				} else {
					throw new OpenemsException("Unable to find Channel [" + thingId + "/" + channelId + "]");
				}
			} catch (NoSuchElementException | OpenemsException e) {
				WebSocketUtils.sendNotificationOrLogError(this.websocketOpt, jMessageId, LogBehaviour.WRITE_TO_LOG,
						Notification.EDGE_CHANNEL_UPDATE_FAILED,
						thingIdOpt.orElse("UNDEFINED") + "/" + channelIdOpt.orElse("UNDEFINED"), e.getMessage());
			}
		}
	}

	/**
	 * Handle current data subscriptions
	 * (try to keep synced with Backend.BrowserWebsocket)
	 *
	 * @param j
	 */
	private synchronized JsonObject currentData(Role role, JsonObject jMessageId, JsonObject jCurrentData) {
		try {
			String mode = JsonUtils.getAsString(jCurrentData, "mode");
			String messageIdUi = JsonUtils.getAsString(jMessageId, "ui");

			if (mode.equals("subscribe")) {
				/*
				 * Subscribe to channels
				 */

				// remove old worker if existed
				CurrentDataWorker worker = this.currentDataSubscribers.remove(messageIdUi);
				if (worker != null) {
					worker.dispose();
				}
				// parse subscribed channels
				HashMultimap<String, String> channels = HashMultimap.create();
				JsonObject jSubscribeChannels = JsonUtils.getAsJsonObject(jCurrentData, "channels");
				for (Entry<String, JsonElement> entry : jSubscribeChannels.entrySet()) {
					String thing = entry.getKey();
					JsonArray jChannels = JsonUtils.getAsJsonArray(entry.getValue());
					for (JsonElement jChannel : jChannels) {
						String channel = JsonUtils.getAsString(jChannel);
						channels.put(thing, channel);
					}
				}
				if (!channels.isEmpty()) {
					// create new worker
					worker = new EdgeCurrentDataWorker(this, jMessageId, channels, role);
					this.currentDataSubscribers.put(messageIdUi, worker);
				}
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
	private synchronized void log(JsonObject jMessageId, JsonObject jLog, Role role) throws OpenemsException {
		if (!(role == Role.ADMIN || role == Role.INSTALLER || role == Role.OWNER)) {
			throw new AccessDeniedException("User role [" + role + "] is not allowed to read system logs.");
		}
		String mode = JsonUtils.getAsString(jLog, "mode");
		String messageIdBackend = JsonUtils.getAsString(jMessageId, "backend");

		if (mode.equals("subscribe")) {
			/*
			 * Subscribe to system log
			 */
			log.info("UI [" + messageIdBackend + "] subscribed to log...");
			this.logSubscribers.put(messageIdBackend, jMessageId);
		} else if (mode.equals("unsubscribe")) {
			/*
			 * Unsubscribe from system log
			 */
			log.info("UI [" + messageIdBackend + "] unsubscribed from log...");
			this.logSubscribers.remove(messageIdBackend);
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
		WebSocketUtils.send(this.websocketOpt, DefaultMessages.systemExecuteReply(jMessageId, output));
	}

	// TODO handle config command
	// /**
	// * Set configuration
	// *
	// * @param j
	// */
	// private synchronized void configure(JsonElement jConfigsElement) {
	// try {
	// JsonArray jConfigs = JsonUtils.getAsJsonArray(jConfigsElement);
	// ThingRepository thingRepository = ThingRepository.getInstance();
	// for (JsonElement jConfigElement : jConfigs) {
	// JsonObject jConfig = JsonUtils.getAsJsonObject(jConfigElement);
	// String mode = JsonUtils.getAsString(jConfig, "mode");
	// if (mode.equals("update")) {
	// /*
	// * Channel Set mode
	// */
	// String thingId = JsonUtils.getAsString(jConfig, "thing");
	// String channelId = JsonUtils.getAsString(jConfig, "channel");
	// JsonElement jValue = JsonUtils.getSubElement(jConfig, "value");
	// Optional<Channel> channelOptional = thingRepository.getChannel(thingId, channelId);
	// if (channelOptional.isPresent()) {
	// Channel channel = channelOptional.get();
	// if (channel instanceof ConfigChannel<?>) {
	// /*
	// * ConfigChannel
	// */
	// ConfigChannel<?> configChannel = (ConfigChannel<?>) channel;
	// configChannel.updateValue(jValue, true);
	// Notification.send(NotificationType.SUCCESS,
	// "Successfully updated [" + channel.address() + "] to [" + jValue + "]");
	//
	// } else if (channel instanceof WriteChannel<?>) {
	// /*
	// * WriteChannel
	// */
	// WriteChannel<?> writeChannel = (WriteChannel<?>) channel;
	// writeChannel.pushWrite(jValue);
	// Notification.send(NotificationType.SUCCESS,
	// "Successfully set [" + channel.address() + "] to [" + jValue + "]");
	// }
	// } else {
	// throw new ConfigException("Unable to find " + jConfig.toString());
	// }
	// } else if (mode.equals("create")) {
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
	// // Send new config
	// JsonObject jMetadata = new JsonObject();
	// // TODO jMetadata.add("config", Config.getInstance().getMetaConfigJson());
	// JsonObject j = new JsonObject();
	// j.add("metadata", jMetadata);
	// WebSocketUtils.send(this.websocket, j);
	// } catch (OpenemsException | ClassNotFoundException e) {
	// Notification.send(NotificationType.ERROR, e.getMessage());
	// }
	// }

	// TODO handle system command
	// /**
	// * System command
	// *
	// * @param j
	// */
	// private synchronized void system(JsonElement jSystemElement) {
	// JsonObject jNotification = new JsonObject();
	// try {
	// JsonObject jSystem = JsonUtils.getAsJsonObject(jSystemElement);
	// String mode = JsonUtils.getAsString(jSystem, "mode");
	// if (mode.equals("systemd-restart")) {
	// /*
	// * Restart systemd service
	// */
	// String service = JsonUtils.getAsString(jSystem, "service");
	// if (service.equals("fems-pagekite")) {
	// ProcessBuilder builder = new ProcessBuilder("/bin/systemctl", "restart", "fems-pagekite");
	// Process p = builder.start();
	// if (p.waitFor() == 0) {
	// log.info("Successfully restarted fems-pagekite");
	// } else {
	// throw new OpenemsException("restart fems-pagekite failed");
	// }
	// } else {
	// throw new OpenemsException("Unknown systemd-restart service: " + jSystemElement.toString());
	// }
	//
	// } else if (mode.equals("manualpq")) {
	// /*
	// * Manual PQ settings
	// */
	// String ess = JsonUtils.getAsString(jSystem, "ess");
	// Boolean active = JsonUtils.getAsBoolean(jSystem, "active");
	// if (active) {
	// Long p = JsonUtils.getAsLong(jSystem, "p");
	// Long q = JsonUtils.getAsLong(jSystem, "q");
	// if (this.controller == null) {
	// throw new OpenemsException("Local access only. Controller is null.");
	// }
	// this.controller.setManualPQ(ess, p, q);
	// Notification.send(NotificationType.SUCCESS,
	// "Leistungsvorgabe gesetzt: ess[" + ess + "], p[" + p + "], q[" + q + "]");
	// } else {
	// this.controller.resetManualPQ(ess);
	// Notification.send(NotificationType.SUCCESS, "Leistungsvorgabe gestoppt: ess[" + ess + "]");
	// }
	// } else {
	// throw new OpenemsException("Unknown system message: " + jSystemElement.toString());
	// }
	// } catch (OpenemsException | IOException | InterruptedException e) {
	// Notification.send(NotificationType.ERROR, e.getMessage());
	// }
	// }

	// TODO handle manual PQ
	// private void manualPQ(JsonElement j, AuthenticatedWebsocketHandler handler) {
	// try {
	// JsonObject jPQ = JsonUtils.getAsJsonObject(j);
	// if (jPQ.has("p") && jPQ.has("q")) {
	// long p = JsonUtils.getAsLong(jPQ, "p");
	// long q = JsonUtils.getAsLong(jPQ, "q");
	// this.controller.setManualPQ(p, q);
	// handler.sendNotification(NotificationType.SUCCESS, "Leistungsvorgabe gesetzt: P=" + p + ",Q=" + q);
	// } else {
	// // stop manual PQ
	// this.controller.resetManualPQ();
	// handler.sendNotification(NotificationType.SUCCESS, "Leistungsvorgabe zurückgesetzt");
	// }
	// } catch (ReflectionException e) {
	// handler.sendNotification(NotificationType.SUCCESS, "Leistungsvorgabewerte falsch: " + e.getMessage());
	// }
	// }

	// TODO handle channel commands
	// private void channel(JsonElement jChannelElement, AuthenticatedWebsocketHandler handler) {
	// try {
	// JsonObject jChannel = JsonUtils.getAsJsonObject(jChannelElement);
	// String thingId = JsonUtils.getAsString(jChannel, "thing");
	// String channelId = JsonUtils.getAsString(jChannel, "channel");
	// JsonElement jValue = JsonUtils.getSubElement(jChannel, "value");
	//
	// // get channel
	// Channel channel;
	// Optional<Channel> channelOptional = thingRepository.getChannel(thingId, channelId);
	// if (channelOptional.isPresent()) {
	// // get channel value
	// channel = channelOptional.get();
	// } else {
	// // Channel not found
	// throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND);
	// }
	//
	// // check for writable channel
	// if (!(channel instanceof WriteChannel<?>)) {
	// throw new ResourceException(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
	// }
	//
	// // set channel value
	// if (channel instanceof ConfigChannel<?>) {
	// // is a ConfigChannel
	// ConfigChannel<?> configChannel = (ConfigChannel<?>) channel;
	// try {
	// configChannel.updateValue(jValue, true);
	// log.info("Updated Channel [" + channel.address() + "] to value [" + jValue.toString() + "].");
	// handler.sendNotification(NotificationType.SUCCESS,
	// "Channel [" + channel.address() + "] aktualisiert zu [" + jValue.toString() + "].");
	// } catch (NotImplementedException e) {
	// throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Conversion not implemented");
	// }
	// } else {
	// // is a WriteChannel
	// handler.sendNotification(NotificationType.WARNING, "WriteChannel nicht implementiert");
	// }
	// } catch (ReflectionException e) {
	// handler.sendNotification(NotificationType.SUCCESS, "Leistungsvorgabewerte falsch: " + e.getMessage());
	// }
	// }

	/**
	 * Send a notification message/error to the websocket
	 *
	 * @param mesage
	 * @return true if successful, otherwise false
	 */
	// TODO send notification
	// public synchronized void sendNotification(NotificationType type, String message) {
	// JsonObject jNotification = new JsonObject();
	// jNotification.addProperty("type", type.name().toLowerCase());
	// jNotification.addProperty("message", message);
	// JsonObject j = new JsonObject();
	// j.add("notification", jNotification);
	// new Thread(() -> {
	// WebSocketUtils.send(websocket, j);
	// }).start();
	// }

	/**
	 * Send a message to the websocket.
	 *
	 * @param message
	 * @throws OpenemsException
	 */
	public void send(JsonObject j) throws OpenemsException {
		WebSocketUtils.send(this.websocketOpt, j);
	}

	/**
	 * Send a message to the websocket.
	 *
	 * @param message
	 */
	public void sendOrLogError(JsonObject j) {
		WebSocketUtils.sendOrLogError(this.websocketOpt, j);
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

}
