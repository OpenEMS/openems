package io.openems.impl.controller.api.websocket;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.core.ClassRepository;
import io.openems.core.ThingRepository;
import io.openems.core.utilities.websocket.AuthenticatedWebsocketHandler;

public class WebsocketServer extends WebSocketServer {

	private static Logger log = LoggerFactory.getLogger(WebsocketServer.class);

	private final ConcurrentHashMap<WebSocket, AuthenticatedWebsocketHandler> websockets = new ConcurrentHashMap<>();
	private final ThingRepository thingRepository;
	private final ClassRepository classRepository;
	private final WebsocketApiController controller;

	public WebsocketServer(WebsocketApiController controller, int port) {
		super(new InetSocketAddress(port));
		this.thingRepository = ThingRepository.getInstance();
		this.classRepository = ClassRepository.getInstance();
		this.controller = controller;
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		log.info("User[" + getUserName(conn) + "]: close connection." //
				+ " Code [" + code + "] Reason [" + reason + "]");
		websockets.remove(conn);
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
		log.info("User[" + getUserName(conn) + "]: error on connection. " + ex.getMessage());
	}

	@Override
	public void onMessage(WebSocket websocket, String message) {
		AuthenticatedWebsocketHandler handler = websockets.get(websocket);
		JsonObject jMessage = (new JsonParser()).parse(message).getAsJsonObject();
		handler.onMessage(jMessage);

		if (!handler.authenticationIsValid()) {
			websockets.remove(websocket);
		}
	}

	// private void config(JsonElement jConfigsElement, AuthenticatedWebsocketHandler handler) {
	// try {
	// JsonArray jConfigs = JsonUtils.getAsJsonArray(jConfigsElement);
	// for (JsonElement jConfigElement : jConfigs) {
	// JsonObject jConfig = JsonUtils.getAsJsonObject(jConfigElement);
	// String operation = JsonUtils.getAsString(jConfig, "operation");
	// if (operation.equals("update")) {
	// /*
	// * Channel Update operation
	// */
	// log.info("Channel: " + jConfig);
	// ThingRepository thingRepository = ThingRepository.getInstance();
	// String thingId = JsonUtils.getAsString(jConfig, "thing");
	// String channelId = JsonUtils.getAsString(jConfig, "channel");
	// Optional<Channel> channelOptional = thingRepository.getChannel(thingId, channelId);
	// if (channelOptional.isPresent()) {
	// Channel channel = channelOptional.get();
	// if (operation.equals("update")) {
	// ConfigChannel<?> configChannel = (ConfigChannel<?>) channel;
	// JsonElement jValue = JsonUtils.getSubElement(jConfig, "value");
	// configChannel.updateValue(jValue, true);
	// log.info("Updated channel " + channel.address() + " with " + jValue);
	// handler.sendNotification(NotificationType.SUCCESS,
	// "Successfully updated [" + channel.address() + "] to [" + jValue + "]");
	// }
	// }
	// } else if (operation.equals("create")) {
	// /*
	// * Create new Thing
	// */
	// JsonObject jObject = JsonUtils.getAsJsonObject(jConfig, "object");
	// String parentId = JsonUtils.getAsString(jConfig, "parentId");
	// String thingId = JsonUtils.getAsString(jObject, "id");
	// if (thingId.startsWith("_")) {
	// throw new ConfigException("IDs starting with underscore are reserved for internal use.");
	// }
	// String clazzName = JsonUtils.getAsString(jObject, "class");
	// Class<?> clazz = Class.forName(clazzName);
	// log.info(jObject.toString());
	// log.info(parentId);
	// log.info(clazzName);
	// if (Device.class.isAssignableFrom(clazz)) {
	// // Device
	// Thing parentThing = thingRepository.getThing(parentId);
	// if (parentThing instanceof Bridge) {
	// Bridge parentBridge = (Bridge) parentThing;
	// Device device = thingRepository.createDevice(jObject);
	// parentBridge.addDevice(device);
	// Config.getInstance().writeConfigFile();
	// handler.sendNotification(NotificationType.SUCCESS,
	// "Device [" + device.id() + "] wurde erstellt.");
	// break;
	// }
	// }
	// } else if (operation.equals("delete")) {
	// /*
	// * Delete a Thing
	// */
	// String thingId = JsonUtils.getAsString(jConfig, "thing");
	// thingRepository.removeThing(thingId);
	// Config.getInstance().writeConfigFile();
	// handler.sendNotification(NotificationType.SUCCESS, "Controller [" + thingId + "] wurde gel�scht.");
	//
	// } else if (jConfig.has("get")) {
	// /*
	// * Get configuration
	// */
	// ThingRepository thingRepository = ThingRepository.getInstance();
	// String get = JsonUtils.getAsString(jConfig, "get");
	// if (get.equals("scheduler")) {
	// for (Scheduler scheduler : thingRepository.getSchedulers()) {
	// handler.sendNotification(NotificationType.INFO,
	// "Scheduler: " + ConfigUtils.getAsJsonElement(scheduler));
	// }
	// } else if (get.equals("controllers")) {
	// for (Scheduler scheduler : thingRepository.getSchedulers()) {
	// for (Controller controller : scheduler.getControllers()) {
	// handler.sendNotification(NotificationType.INFO,
	// "Controller: " + ConfigUtils.getAsJsonElement(controller));
	// }
	// }
	// }
	// } else {
	// throw new OpenemsException("Methode [" + operation + "] ist nicht implementiert.");
	// }
	// }
	// // Send new config
	// JsonObject j = new JsonObject();
	// j.add("config", Config.getInstance().getMetaConfigJson());
	// handler.send(true, j);
	// } catch (OpenemsException | ClassNotFoundException e) {
	// handler.sendNotification(NotificationType.ERROR, e.getMessage());
	// // TODO: send notification to websocket
	// }
	// }
	//
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

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		log.info("Incoming connection...");
		websockets.put(conn, new AuthenticatedWebsocketHandler(conn));
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
		AuthenticatedWebsocketHandler handler = websockets.get(conn);
		if (handler == null) {
			return "NOT_CONNECTED";
		} else {
			return handler.getUserName();
		}
	}

	// public void broadcastNotification(NotificationType type, String message) {
	// websockets.forEach((websocket, handler) -> {
	// handler.sendNotification(type, message);
	// });
	// }
}
