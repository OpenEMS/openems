package io.openems.impl.controller.api.websocket;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.NotImplementedException;
import io.openems.api.exception.OpenemsException;
import io.openems.api.exception.ReflectionException;
import io.openems.api.scheduler.Scheduler;
import io.openems.api.security.Authentication;
import io.openems.api.thing.ThingDescription;
import io.openems.core.Config;
import io.openems.core.ThingRepository;
import io.openems.core.utilities.ConfigUtils;
import io.openems.core.utilities.InjectionUtils;
import io.openems.core.utilities.JsonUtils;

public class WebsocketServer extends WebSocketServer {

	private static Logger log = LoggerFactory.getLogger(WebsocketServer.class);
	private final ConcurrentHashMap<WebSocket, WebsocketHandler> sockets = new ConcurrentHashMap<>();
	private final ThingRepository thingRepository;

	public WebsocketServer(int port) {
		super(new InetSocketAddress(port));
		this.thingRepository = ThingRepository.getInstance();
	}

	@Override public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		log.info("User[" + getUserName(conn) + "]: close connection." //
				+ " Code [" + code + "] Reason [" + reason + "]");
		sockets.remove(conn);
	}

	@Override public void onError(WebSocket conn, Exception ex) {
		log.info("User[" + getUserName(conn) + "]: error on connection. " + ex.getMessage());
	}

	@Override public void onMessage(WebSocket conn, String message) {
		JsonObject j = (new JsonParser()).parse(message).getAsJsonObject();
		WebsocketHandler handler = sockets.get(conn);

		/*
		 * Authenticate user and send immediate reply
		 */
		if (j.has("authenticate")) {
			authenticate(j.get("authenticate"), handler);
		}

		/*
		 * Check authentication
		 */
		if (!handler.isValid()) {
			// no user authenticated till now -> exit
			conn.close();
			return;
		}

		/*
		 * On initial call -> send config
		 */
		if (j.has("authenticate")) {
			sendConfig(handler);
		}

		/*
		 * Subscribe to data
		 */
		if (j.has("subscribe")) {
			subscribe(j.get("subscribe"), handler);
		}

		/*
		 * Set a channel
		 */
		if (j.has("config")) {
			config(j.get("config"), handler);
		}
	}

	/**
	 * Authenticates a user according to the "authenticate" message. Adds the session to this {@link WebsocketHandler}
	 * if valid.
	 *
	 * @param jAuthenticateElement
	 * @param handler
	 */
	private void authenticate(JsonElement jAuthenticateElement, WebsocketHandler handler) {
		try {
			JsonObject jAuthenticate = JsonUtils.getAsJsonObject(jAuthenticateElement);
			Authentication auth = Authentication.getInstance();
			if (jAuthenticate.has("password")) {
				/*
				 * Authenticate using username and password
				 */
				String password = JsonUtils.getAsString(jAuthenticate, "password");
				if (jAuthenticate.has("username")) {
					String username = JsonUtils.getAsString(jAuthenticate, "username");
					handler.setSession(auth.byUserPassword(username, password));
				} else {
					handler.setSession(auth.byPassword(password));
				}
			} else if (jAuthenticate.has("token")) {
				/*
				 * Authenticate using session token
				 */
				String token = JsonUtils.getAsString(jAuthenticate, "token");
				handler.setSession(auth.bySession(token));
			}
		} catch (ReflectionException e) { /* ignore */ }
		/*
		 * Send reply
		 */
		JsonObject jReply = new JsonObject();
		JsonObject jAuthenticate = new JsonObject();
		if (handler.isValid()) {
			// on success: send immediate reply with token to client
			jAuthenticate.addProperty("token", handler.getSession().getToken());
			jAuthenticate.addProperty("username", handler.getSession().getUser().getName());
		} else {
			log.error("Authentication failed");
			// on failure: send immediate reply with error
			jAuthenticate.addProperty("failed", true);
		}
		jReply.add("authenticate", jAuthenticate);
		WebsocketServer.send(handler.getWebSocket(), jReply);
	}

	private void sendConfig(WebsocketHandler handler) {
		try {
			Config config = Config.getInstance();
			/*
			 * Json Config
			 */
			JsonObject j = config.getJson(true);
			/*
			 * Natures
			 */
			JsonObject jDevices = new JsonObject();
			thingRepository.getDeviceNatures().forEach(nature -> {
				JsonArray jNatureClasses = new JsonArray();
				/*
				 * get important classes/interfaces that are implemented by this nature
				 */
				for (Class<?> iface : InjectionUtils.getImportantNatureInterfaces(nature.getClass())) {
					jNatureClasses.add(iface.getSimpleName());
				}
				jDevices.add(nature.id(), jNatureClasses);
			});
			j.add("_devices", jDevices);
			/*
			 * Available Controllers
			 */
			JsonArray jControllers = new JsonArray();
			for (ThingDescription controllerDescription : thingRepository.getAvailableControllers()) {
				jControllers.add(controllerDescription.getAsJsonObject());
			}
			j.add("_controllers", jControllers);

			handler.send("config", j);
		} catch (NotImplementedException | ConfigException e) {
			handler.sendNotification(NotificationType.WARNING, "Unable to send config: " + e.getMessage());
		}
	}

	private void subscribe(JsonElement j, WebsocketHandler handler) {
		if (j.isJsonPrimitive() && j.getAsJsonPrimitive().isString()) {
			String tag = j.getAsString();
			handler.addSubscribedChannels(tag);
		}
	}

	private void config(JsonElement jConfigsElement, WebsocketHandler handler) {
		try {
			JsonArray jConfigs = JsonUtils.getAsJsonArray(jConfigsElement);
			for (JsonElement jConfigElement : jConfigs) {
				JsonObject jConfig = JsonUtils.getAsJsonObject(jConfigElement);
				String operation = JsonUtils.getAsString(jConfig, "operation");
				if (operation.equals("update")) {
					/*
					 * Channel Update operation
					 */
					log.info("Channel: " + jConfig);
					ThingRepository thingRepository = ThingRepository.getInstance();
					String thingId = JsonUtils.getAsString(jConfig, "thing");
					String channelId = JsonUtils.getAsString(jConfig, "channel");
					Optional<Channel> channelOptional = thingRepository.getChannel(thingId, channelId);
					if (channelOptional.isPresent()) {
						Channel channel = channelOptional.get();
						if (operation.equals("update")) {
							ConfigChannel<?> configChannel = (ConfigChannel<?>) channel;
							JsonElement jValue = JsonUtils.getSubElement(jConfig, "value");
							configChannel.updateValue(jValue, true);
							log.info("Updated channel " + channel.address() + " with " + jValue);
							handler.sendNotification(NotificationType.SUCCESS,
									"Successfully updated [" + channel.address() + "] to [" + jValue + "]");
						}
					}
				} else if (operation.equals("create")) {
					/*
					 * Create new Thing
					 */
					JsonObject jObject = JsonUtils.getAsJsonObject(jConfig, "object");
					JsonArray jPaths = JsonUtils.getAsJsonArray(jConfig, "path");
					String thingId = JsonUtils.getAsString(jObject, "id");
					if (thingId.startsWith("_")) {
						throw new ConfigException("IDs starting with underscore are reserved for internal use.");
					}
					for (JsonElement jPath : jPaths) {
						String path = JsonUtils.getAsString(jPath);
						if (path.equals("controllers")) {
							Controller controller = thingRepository.createController(jObject);
							for (Scheduler scheduler : thingRepository.getSchedulers()) {
								// TODO needs modification for multiple schedulers
								scheduler.addController(controller);
							}
							Config.getInstance().writeConfigFile();
							handler.sendNotification(NotificationType.SUCCESS,
									"Controller [" + controller.id() + "] wurde erstellt.");
							break;
						}
					}
				} else if (operation.equals("delete")) {
					/*
					 * Delete a Thing
					 */
					String thingId = JsonUtils.getAsString(jConfig, "thing");
					thingRepository.removeThing(thingId);
					Config.getInstance().writeConfigFile();
					handler.sendNotification(NotificationType.SUCCESS, "Controller [" + thingId + "] wurde gelöscht.");

				} else if (jConfig.has("get")) {
					/*
					 * Get configuration
					 */
					ThingRepository thingRepository = ThingRepository.getInstance();
					String get = JsonUtils.getAsString(jConfig, "get");
					if (get.equals("scheduler")) {
						for (Scheduler scheduler : thingRepository.getSchedulers()) {
							handler.sendNotification(NotificationType.INFO,
									"Scheduler: " + ConfigUtils.getAsJsonElement(scheduler));
						}
					} else if (get.equals("controllers")) {
						for (Scheduler scheduler : thingRepository.getSchedulers()) {
							for (Controller controller : scheduler.getControllers()) {
								handler.sendNotification(NotificationType.INFO,
										"Controller: " + ConfigUtils.getAsJsonElement(controller));
							}
						}
					}
				} else {
					throw new OpenemsException("Methode [" + operation + "] ist nicht implementiert.");
				}
			}
		} catch (OpenemsException e) {
			log.error(e.getMessage());
			handler.sendNotification(NotificationType.ERROR, e.getMessage());
			// TODO: send notification to websocket
		}
	}

	@Override public void onOpen(WebSocket conn, ClientHandshake handshake) {
		log.info("Incoming connection...");
		sockets.put(conn, new WebsocketHandler(conn));
	}

	/**
	 * Gets the user name of this user, avoiding null
	 *
	 * @param conn
	 * @return
	 */
	private String getUserName(WebSocket conn) {
		if (conn == null) {
			return "NOT_CONNECTED";
		}
		WebsocketHandler handler = sockets.get(conn);
		if (handler == null) {
			return "NOT_CONNECTED";
		} else {
			return handler.getUserName();
		}
	}

	/**
	 * Send a message to a websocket
	 *
	 * @param j
	 * @return true if successful, otherwise false
	 */
	private static boolean send(WebSocket conn, JsonObject j) {
		try {
			conn.send(j.toString());
			return true;
		} catch (WebsocketNotConnectedException e) {
			return false;
		}
	}
}
