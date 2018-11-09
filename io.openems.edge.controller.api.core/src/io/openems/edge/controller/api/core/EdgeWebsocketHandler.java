package io.openems.edge.controller.api.core;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.java_websocket.WebSocket;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.AccessDeniedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.Role;
import io.openems.common.timedata.TimedataUtils;
import io.openems.common.utils.JsonUtils;
import io.openems.common.websocket.DefaultMessages;
import io.openems.common.websocket.LogBehaviour;
import io.openems.common.websocket.Notification;
import io.openems.common.websocket.WebSocketUtils;
import io.openems.edge.timedata.api.Timedata;

/**
 * Handles a Websocket connection to a browser, OpenEMS backend,...
 */
public class EdgeWebsocketHandler {

	private Logger log = LoggerFactory.getLogger(EdgeWebsocketHandler.class);

	protected final ApiController parent;

	/**
	 * Holds the websocket connection
	 */
	protected final WebSocket websocket;

	/**
	 * Holds subscriber to current data
	 */
	private Optional<EdgeCurrentDataWorker> currentDataWorkerOpt = Optional.empty();

	/**
	 * Holds subscribers to system log (identified by messageId.backend, holds
	 * complete jMessageId)
	 */
	private final Map<String, JsonObject> logSubscribers = new HashMap<>();

	/**
	 * Role for this connection.
	 */
	private Optional<Role> roleOpt = Optional.empty();

	public EdgeWebsocketHandler(ApiController parent, WebSocket websocket) {
		this.parent = parent;
		this.websocket = websocket;
	}

	public synchronized void dispose() {
		if (this.currentDataWorkerOpt.isPresent()) {
			this.currentDataWorkerOpt.get().dispose();
			this.currentDataWorkerOpt = Optional.empty();
		}
		this.logSubscribers.clear();
		this.websocket.close();
	}

	public synchronized void setRole(Role role) {
		this.setRole(Optional.ofNullable(role));
	}

	public synchronized void unsetRole() {
		this.setRole(Optional.empty());
	}

	public synchronized void setRole(Optional<Role> roleOpt) {
		this.roleOpt = roleOpt;
		if (this.currentDataWorkerOpt.isPresent()) {
			this.currentDataWorkerOpt.get().dispose();
			this.currentDataWorkerOpt = Optional.empty();
		}
		if (roleOpt.isPresent()) {
			this.currentDataWorkerOpt = Optional.of(new EdgeCurrentDataWorker(this, roleOpt.get(), websocket));
		}
	}

	public Optional<Role> getRoleOpt() {
		return roleOpt;
	}

	/**
	 * Handles a message from Websocket.
	 *
	 * @param jMessage
	 */
	public final void onMessage(JsonObject jMessage) {
		if (!this.roleOpt.isPresent()) {
			log.error("No User! Aborting...");
			return;
		}
		// Role role = this.userOpt.get().getRole();

		Role role = Role.ADMIN;
		// get MessageId from message -> used for reply
		Optional<JsonObject> jMessageIdOpt = JsonUtils.getAsOptionalJsonObject(jMessage, "messageId");

		if (jMessageIdOpt.isPresent()) {
			JsonObject jMessageId = jMessageIdOpt.get();
			/*
			 * Config
			 */
			Optional<JsonObject> jConfigOpt = JsonUtils.getAsOptionalJsonObject(jMessage, "config");
			if (jConfigOpt.isPresent()) {
				this.config(role, jMessageId, jConfigOpt.get());
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
		Timedata timedataService = this.parent.getTimedataService();
		if (timedataService == null) {
			WebSocketUtils.sendNotificationOrLogError(this.websocket, new JsonObject(), LogBehaviour.WRITE_TO_LOG,
					Notification.NO_TIMEDATA_SOURCE_AVAILABLE);
			return;
		}
		try {
			JsonObject j = TimedataUtils.handle(timedataService, jMessageId, jHistoricData);
			WebSocketUtils.send(this.websocket, j);
		} catch (OpenemsException e) {
			WebSocketUtils.sendNotificationOrLogError(this.websocket, jMessageId, LogBehaviour.WRITE_TO_LOG,
					Notification.UNABLE_TO_QUERY_HISTORIC_DATA, e.getMessage());
		}
	}

	/**
	 * Handle "config" messages
	 *
	 * @param jConfig
	 * @return
	 */
	private synchronized void config(Role role, JsonObject jMessageId, JsonObject jConfig) {
		Optional<String> modeOpt = JsonUtils.getAsOptionalString(jConfig, "mode");
		switch (modeOpt.orElse("")) {
		case "query":
			/*
			 * Query current config
			 */
			try {
				JsonObject jReplyConfig = new JsonObject();
				jReplyConfig.add("meta", Utils.getComponentsMeta(this.parent.getComponents()));
				jReplyConfig.add("components",
						Utils.getComponents(this.parent.getConfigurationAdmin().listConfigurations("(enabled=true)")));
				WebSocketUtils.send(this.websocket, DefaultMessages.configQueryReply(jMessageId, jReplyConfig));
			} catch (IOException | InvalidSyntaxException | OpenemsException e) {
				WebSocketUtils.sendNotificationOrLogError(this.websocket, jMessageId, LogBehaviour.WRITE_TO_LOG,
						Notification.UNABLE_TO_READ_CURRENT_CONFIG, e.getMessage());
			}
			break;

		case "update":
			/*
			 * Update thing/channel config
			 */
			throw new IllegalArgumentException("Config Update is not implemented");

			// TODO Update config
			// Optional<String> thingIdOpt = JsonUtils.getAsOptionalString(jConfig,
			// "thing");
			// Optional<String> channelIdOpt = JsonUtils.getAsOptionalString(jConfig,
			// "channel");
			// try {
			// String thingId = thingIdOpt.get();
			// String channelId = channelIdOpt.get();
			// JsonElement jValue = JsonUtils.getSubElement(jConfig, "value");
			// Optional<Channel> channelOpt =
			// ThingRepository.getInstance().getChannel(thingId, channelId);
			// if (channelOpt.isPresent()) {
			// Channel channel = channelOpt.get();
			// // check write permissions
			// channel.assertWriteAllowed(role);
			// if (channel instanceof ConfigChannel<?>) {
			// /*
			// * ConfigChannel
			// */
			// ConfigChannel<?> configChannel = (ConfigChannel<?>) channel;
			// Object value = ConfigUtils.getConfigObject(configChannel, jValue);
			// configChannel.updateValue(value, true);
			// WebSocketUtils.sendNotificationOrLogError(this.websocket, jMessageId,
			// LogBehaviour.WRITE_TO_LOG,
			// Notification.EDGE_CHANNEL_UPDATE_SUCCESS, channel.address() + " => " +
			// jValue);
			//
			// } else if (channel instanceof WriteChannel<?>) {
			// /*
			// * WriteChannel
			// */
			// WriteChannel<?> writeChannel = (WriteChannel<?>) channel;
			// if (!apiWorkerOpt.isPresent()) {
			// WebSocketUtils.sendNotificationOrLogError(this.websocket, jMessageId,
			// LogBehaviour.WRITE_TO_LOG, Notification.BACKEND_NOT_ALLOWED,
			// "set " + channel.address() + " => " + jValue);
			// } else {
			// ApiWorker apiWorker = apiWorkerOpt.get();
			// WriteObject writeObject = new WriteJsonObject(jValue).onFirstSuccess(() -> {
			// WebSocketUtils.sendNotificationOrLogError(this.websocket, jMessageId,
			// LogBehaviour.WRITE_TO_LOG, Notification.EDGE_CHANNEL_UPDATE_SUCCESS,
			// "set " + channel.address() + " => " + jValue);
			// }).onFirstError((e) -> {
			// WebSocketUtils.sendNotificationOrLogError(this.websocket, jMessageId,
			// LogBehaviour.WRITE_TO_LOG, Notification.EDGE_CHANNEL_UPDATE_FAILED,
			// "set " + channel.address() + " => " + jValue, e.getMessage());
			// }).onTimeout(() -> {
			// WebSocketUtils.sendNotificationOrLogError(this.websocket, jMessageId,
			// LogBehaviour.WRITE_TO_LOG, Notification.EDGE_CHANNEL_UPDATE_TIMEOUT,
			// "set " + channel.address() + " => " + jValue);
			// });
			// apiWorker.addValue(writeChannel, writeObject);
			// }
			// }
			// } else {
			// WebSocketUtils.sendNotificationOrLogError(this.websocket, jMessageId,
			// LogBehaviour.WRITE_TO_LOG,
			// Notification.CHANNEL_NOT_FOUND, thingId + "/" + channelId);
			// }
			// } catch (NoSuchElementException | OpenemsException e) {
			// WebSocketUtils.sendNotificationOrLogError(this.websocket, jMessageId,
			// LogBehaviour.WRITE_TO_LOG,
			// Notification.EDGE_CHANNEL_UPDATE_FAILED,
			// thingIdOpt.orElse("UNDEFINED") + "/" + channelIdOpt.orElse("UNDEFINED"),
			// e.getMessage());
			// }
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
	// throw new ConfigException("IDs starting with underscore are reserved for
	// internal use.");
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
	// Notification.send(NotificationType.SUCCESS, "Device [" + device.id() + "]
	// wurde erstellt.");
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
	// Notification.send(NotificationType.SUCCESS, "Controller [" + thingId + "]
	// wurde " + " gel�scht.");
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
	 * Handle current data subscriptions (try to keep synced with
	 * Backend.BrowserWebsocket)
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
		throw new IllegalArgumentException("System is no implemented");

		// TODO implement system
		// if (!(role == Role.ADMIN)) {
		// throw new AccessDeniedException("User role [" + role + "] is not allowed to
		// execute system commands.");
		// }
		// String output = "";
		// String mode = JsonUtils.getAsString(jSystem, "mode");
		// String password = JsonUtils.getAsString(jSystem, "password");
		// String command = JsonUtils.getAsString(jSystem, "command");
		// boolean background = JsonUtils.getAsBoolean(jSystem, "background");
		// int timeout = JsonUtils.getAsInt(jSystem, "timeout");
		//
		// if (mode.equals("execute")) {
		// log.info("UI [" + jMessageId + "] executes system command [" + command + "]
		// background [" + background
		// + "] timeout [" + timeout + "]");
		// output = LinuxCommand.execute(password, command, background, timeout);
		// }
		// WebSocketUtils.send(this.websocket,
		// DefaultMessages.systemExecuteReply(jMessageId, output));
	}

	/**
	 * Send a message to the websocket.
	 *
	 * @param j
	 * @throws OpenemsException
	 */
	public void send(JsonObject j) throws OpenemsException {
		WebSocketUtils.send(this.websocket, j);
	}

	/**
	 * Send a message to the websocket.
	 *
	 * @param j
	 */
	public void sendOrLogError(JsonObject j) {
		WebSocketUtils.sendOrLogError(this.websocket, j);
	}

	/**
	 * Send a log message to the websocket. This method is called by logback
	 *
	 * @param event
	 */
	public void sendLog(PaxLoggingEvent event) {
		if (this.logSubscribers.isEmpty()) {
			// nobody subscribed
			return;
		}
		for (Entry<String, JsonObject> entry : this.logSubscribers.entrySet()) {
			JsonObject j = DefaultMessages.log(entry.getValue(), event.getTimeStamp(), event.getLevel().toString(),
					event.getLoggerName(), event.getMessage());
			try {
				this.send(j);
			} catch (OpenemsException e) {
				// Error while sending: remove subscriber
				this.logSubscribers.remove(entry.getKey());
				log.error("Error while sending log. Removing subscriber [" + entry.getKey() + "]");
			}
		}
	}

	@Deprecated
	public WebSocket getWebsocket() {
		return this.websocket;
	}

}
