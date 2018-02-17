package io.openems.backend.uiwebsocket.impl.provider;

import java.util.Map.Entry;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.java_websocket.WebSocket;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.backend.metadata.api.Edge;
import io.openems.backend.metadata.api.Role;
import io.openems.backend.metadata.api.User;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;
import io.openems.common.websocket.AbstractWebsocketServer;
import io.openems.common.websocket.DefaultMessages;
import io.openems.common.websocket.WebSocketUtils;

public class UiWebsocketServer extends AbstractWebsocketServer {

	private final Logger log = LoggerFactory.getLogger(UiWebsocketServer.class);
	private final UiWebsocket parent;
	private final Map<UUID, WebSocket> websocketsMap = new HashMap<>();

	public UiWebsocketServer(UiWebsocket parent, int port) {
		super(port);
		this.parent = parent;
	}

	@Override
	protected void _onOpen(WebSocket websocket, ClientHandshake handshake) {
		String error = "";
		Optional<User> userOpt = Optional.empty();

		// login using session_id from the cookie
		Optional<String> sessionIdOpt = getSessionIdFromHandshake(handshake);
		if (!sessionIdOpt.isPresent()) {
			error = "Session-ID is missing in handshake";
		} else {
			try {
				userOpt = this.parent.metadataService.getUserWithSession(sessionIdOpt.get());
				// TODO fix bug in Odoo that is not reliably returning all configured devices
			} catch (OpenemsException e) {
				error = e.getMessage();
			}
		}

		if (!error.isEmpty()) {
			// send connection failed to browser
			this.send(websocket, DefaultMessages.browserConnectionFailedReply());
			log.warn("User connection failed. Session [" + sessionIdOpt.orElse("") + "] Error [" + error + "].");
			websocket.closeConnection(CloseFrame.REFUSE, error);

		} else if (userOpt.isPresent()) {
			User user = userOpt.get();

			UUID uuid = UUID.randomUUID();
			synchronized (this.websocketsMap) {
				// add websocket to local cache
				this.websocketsMap.put(uuid, websocket);
			}
			// store userId together with the websocket
			websocket.setAttachment(new WebsocketData(user.getId(), uuid));

			// send connection successful to browser
			JsonArray jEdges = new JsonArray();
			for (Entry<Integer, Role> edgeRole : user.getEdgeRoles().entrySet()) {
				int edgeId = edgeRole.getKey();
				Role role = edgeRole.getValue();
				Optional<Edge> edgeOpt = this.parent.metadataService.getEdge(edgeId);
				if (!edgeOpt.isPresent()) {
					log.warn("Unable to find Edge [ID:" + edgeId + "]");
				} else {
					JsonObject jEdge = edgeOpt.get().toJsonObject();
					jEdge.addProperty("role", role.toString());
					jEdges.add(jEdge);
				}
			}
			JsonObject jReply = DefaultMessages.browserConnectionSuccessfulReply("" /* TODO empty token? */,
					Optional.empty(), jEdges);
			WebSocketUtils.send(websocket, jReply);
			log.info("User [" + user.getName() + "] connected with Session [" + sessionIdOpt.orElse("") + "].");
		}
	}

	@Override
	protected void _onError(WebSocket websocket, Exception ex) {
		log.info("UiWebsocketServer: On Error");
	}

	@Override
	protected void _onClose(WebSocket websocket) {
		// get current User
		WebsocketData data = websocket.getAttachment();
		Optional<User> userOpt = this.parent.metadataService.getUser(data.getUserId());
		if (userOpt.isPresent()) {
			log.info("User [" + userOpt.get().getName() + "] disconnected.");
		} else {
			log.info("User [ID:" + data.getUserId() + "] disconnected.");
		}
		// remove websocket from local cache
		synchronized (this.websocketsMap) {
			this.websocketsMap.remove(data.getUuid());
		}
	}

	@Override
	protected void _onMessage(WebSocket websocket, JsonObject jMessage) {
		// get current User
		WebsocketData data = websocket.getAttachment();
		int userId = data.getUserId();
		
		// get MessageId from message
		Optional<String> messageIdOpt = JsonUtils.getAsOptionalString(jMessage, "messageId");

		// get EdgeId from message
		Optional<Integer> edgeIdOpt = JsonUtils.getAsOptionalInt(jMessage, "edgeId");

		if (messageIdOpt.isPresent() && edgeIdOpt.isPresent()) {
			String messageId = messageIdOpt.get();
			int edgeId = edgeIdOpt.get();

			/*
			 * verify that User is allowed to access Edge
			 */
			Optional<User> userOpt = this.parent.metadataService.getUser(userId);
			if (!userOpt.isPresent() || !(userOpt.get().getEdgeRole(edgeId).isPresent())) {
				// TODO Error Access denied
				return;
			}

			/*
			 * TODO Query historic data
			 */
			// if (jMessage.has("historicData")) {
			// // parse deviceId
			// JsonArray jMessageId = jMessageIdOpt.get();
			// try {
			// JsonObject jHistoricData = JsonUtils.getAsJsonObject(jMessage,
			// "historicData");
			// JsonObject jReply = WebSocketUtils.historicData(jMessageId, jHistoricData,
			// deviceIdOpt,
			// Timedata.instance(), Role.ADMIN);
			// // TODO read role from device
			// WebSocketUtils.send(websocket, jReply);
			// } catch (OpenemsException e) {
			// log.error(e.getMessage());
			// }
			// }

			/*
			 * TODO Subscribe to currentData
			 */
			// if (jMessage.has("currentData")) {
			// JsonObject jCurrentData;
			// try {
			// jCurrentData = JsonUtils.getAsJsonObject(jMessage, "currentData");
			// log.info("User [" + session.getData().getUserName() + "] subscribed to
			// current data for device ["
			// + deviceName + "]: " + StringUtils.toShortString(jCurrentData, 50));
			// JsonArray jMessageId = jMessageIdOpt.get();
			// int deviceId = deviceIdOpt.get();
			// this.currentData(session, websocket, jCurrentData, jMessageId, deviceName,
			// deviceId);
			// } catch (OpenemsException e) {
			// log.error(e.getMessage());
			// }
			// }

			/*
			 * Serve "Config -> Query" from cache
			 */
			Optional<JsonObject> jConfigOpt = JsonUtils.getAsOptionalJsonObject(jMessage, "config");
			if (jConfigOpt.isPresent()) {
				JsonObject jConfig = jConfigOpt.get();
				switch (JsonUtils.getAsOptionalString(jConfig, "mode").orElse("")) {
				case "query":
					/*
					 * Query current config
					 */
					Edge edge = this.parent.metadataService.getEdge(edgeId).get();
					JsonObject jReply = DefaultMessages.configQueryReply(messageId, edge.getConfig());
					WebSocketUtils.send(websocket, jReply);
					break;
				}

				/*
				 * TODO Forward to OpenEMS Edge
				 */
				// if ((jMessage.has("config") && !configModeOpt.orElse("").equals("query")) ||
				// jMessage.has("log")
				// || jMessage.has("system")) {
				// try {
				// forwardMessageToOpenems(session, websocket, jMessage, deviceName);
				// } catch (OpenemsException e) {
				// WebSocketUtils.sendNotification(websocket, new JsonArray(),
				// LogBehaviour.WRITE_TO_LOG,
				// Notification.EDGE_UNABLE_TO_FORWARD, deviceName, e.getMessage());
				// }
			}
		}
	}
}
