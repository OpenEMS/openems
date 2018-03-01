package io.openems.backend.uiwebsocket.impl.provider;

import java.util.Map.Entry;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.java_websocket.WebSocket;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.backend.metadata.api.Edge;
import io.openems.backend.metadata.api.User;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.StringUtils;
import io.openems.common.websocket.AbstractWebsocketServer;
import io.openems.common.websocket.DefaultMessages;
import io.openems.common.websocket.LogBehaviour;
import io.openems.common.websocket.Notification;
import io.openems.common.websocket.WebSocketUtils;

public class UiWebsocketServer extends AbstractWebsocketServer {

	protected final UiWebsocket parent;
	private final Logger log = LoggerFactory.getLogger(UiWebsocketServer.class);
	private final Map<UUID, WebSocket> websocketsMap = new HashMap<>();

	public UiWebsocketServer(UiWebsocket parent, int port) {
		super(port);
		this.parent = parent;
	}

	@Override
	protected void _onOpen(WebSocket websocket, ClientHandshake handshake) {
		String error = "";
		User user;

		// login using session_id from the cookie
		Optional<String> sessionIdOpt = getFieldFromHandshakeCookie(handshake, "session_id");
		try {
			if (!sessionIdOpt.isPresent()) {
				throw new OpenemsException("Session-ID is missing in handshake");
			}
			user = this.parent.metadataService.getUserWithSession(sessionIdOpt.get());
			// TODO fix bug in Odoo that is not reliably returning all configured devices
		} catch (OpenemsException e) {
			// send connection failed to browser
			this.send(websocket, DefaultMessages.uiConnectionFailedReply());
			log.warn("User connection failed. Session [" + sessionIdOpt.orElse("") + "] Error [" + error + "].");
			websocket.closeConnection(CloseFrame.REFUSE, error);
			return;
		}

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
			Edge edge;
			try {
				edge = this.parent.metadataService.getEdge(edgeId);
				JsonObject jEdge = edge.toJsonObject();
				jEdge.addProperty("role", role.toString());
				jEdges.add(jEdge);
			} catch (OpenemsException e) {
				// TODO handle error
				log.warn(e.getMessage());
			}
		}
		log.info("User [" + user.getName() + "] connected with Session [" + sessionIdOpt.orElse("") + "].");
		JsonObject jReply = DefaultMessages.uiConnectionSuccessfulReply("" /* empty token? */,
				jEdges);
		WebSocketUtils.sendOrLogError(websocket, jReply);
	}

	@Override
	protected void _onError(WebSocket websocket, Exception ex) {
		WebsocketData data = websocket.getAttachment();
		log.info("User [" + getUserName(data) + "] websocket error: " + ex.getMessage());
	}

	@Override
	protected void _onClose(WebSocket websocket) {
		// get current User
		WebsocketData data = websocket.getAttachment();
		log.info("User [" + getUserName(data) + "] disconnected.");

		// stop CurrentDataWorker
		Optional<BackendCurrentDataWorker> currentDataWorkerOpt = data.getCurrentDataWorker();
		if (currentDataWorkerOpt.isPresent()) {
			currentDataWorkerOpt.get().dispose();
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
		Optional<User> userOpt = this.parent.metadataService.getUser(userId);
		if (!userOpt.isPresent()) {
			// TODO Error user not found
			return;
		}
		User user = userOpt.get();

		// get MessageId from message
		Optional<JsonObject> jMessageIdOpt = JsonUtils.getAsOptionalJsonObject(jMessage, "messageId");

		// get EdgeId from message
		Optional<Integer> edgeIdOpt = JsonUtils.getAsOptionalInt(jMessage, "edgeId");

		if (jMessageIdOpt.isPresent() && edgeIdOpt.isPresent()) {
			JsonObject jMessageId = jMessageIdOpt.get();
			int edgeId = edgeIdOpt.get();

			/*
			 * verify that User is allowed to access Edge
			 */
			if (!user.getEdgeRole(edgeId).isPresent()) {
				// TODO Error Access denied
				return;
			}

			// get Edge
			Edge edge;
			try {
				edge = this.parent.metadataService.getEdge(edgeId);
			} catch (OpenemsException e) {
				// TODO handle error
				log.error(e.getMessage());
				return;
			}

			/*
			 * Query historic data
			 */
			Optional<JsonObject> jHistoricDataOpt = JsonUtils.getAsOptionalJsonObject(jMessage, "historicData");
			if (jHistoricDataOpt.isPresent()) {
				JsonObject jHistoricData = jHistoricDataOpt.get();
				JsonObject jReply = this.historicData(jMessageId, edgeId, jHistoricData);
				WebSocketUtils.sendOrLogError(websocket, jReply);
				return;
			}

			/*
			 * Subscribe to currentData
			 */
			Optional<JsonObject> jCurrentDataOpt = JsonUtils.getAsOptionalJsonObject(jMessage, "currentData");
			if (jCurrentDataOpt.isPresent()) {
				JsonObject jCurrentData = jCurrentDataOpt.get();
				log.info("User [" + user.getName() + "] subscribed to current data for device [" + edge.getName()
						+ "]: " + StringUtils.toShortString(jCurrentData, 50));
				this.currentData(websocket, data, jMessageId, edgeId, jCurrentData);
				return;
			}

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
					JsonObject jReply = DefaultMessages.configQueryReply(jMessageId, edge.getConfig());
					WebSocketUtils.sendOrLogError(websocket, jReply);
					return;
				}
			}

			/*
			 * TODO Forward to OpenEMS Edge
			 */
			if (jMessage.has("config") || jMessage.has("log") || jMessage.has("system")) {
				try {
					log.info("User [" + user.getName() + "] Forward message to Edge [" + edge.getName() +"]: " + StringUtils.toShortString(jMessage, 100));
					Optional<Role> roleOpt = user.getEdgeRole(edgeId);
					JsonObject j = DefaultMessages.prepareMessageForForwardToEdge(jMessage, data.getUuid(), roleOpt);
					this.parent.edgeWebsocketService.forwardMessageFromUi(edgeId, j);
				} catch (OpenemsException e) {
					WebSocketUtils.sendNotificationOrLogError(websocket, jMessageId, LogBehaviour.WRITE_TO_LOG,
							Notification.EDGE_UNABLE_TO_FORWARD, edge.getName(), e.getMessage());
				}
			}
		}
	}

	/**
	 * Handle current data subscriptions
	 *
	 * @param j
	 */
	private synchronized void currentData(WebSocket websocket, WebsocketData data, JsonObject jMessageId, int edgeId,
			JsonObject jCurrentData) {
		try {
			String mode = JsonUtils.getAsString(jCurrentData, "mode");

			if (mode.equals("subscribe")) {
				/*
				 * Subscribe to channels
				 */

				// remove old worker if it existed
				Optional<BackendCurrentDataWorker> workerOpt = data.getCurrentDataWorker();
				if (workerOpt.isPresent()) {
					data.setCurrentDataWorker(null);
					workerOpt.get().dispose();
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
					BackendCurrentDataWorker worker = new BackendCurrentDataWorker(this, websocket, jMessageId, edgeId,
							channels);
					data.setCurrentDataWorker(worker);
				}
			}
		} catch (OpenemsException e) {
			// TODO handle exception
			log.warn(e.getMessage());
		}
	}

	/**
	 * Query history command
	 *
	 * @param j
	 */
	private JsonObject historicData(JsonObject jMessageId, int edgeId, JsonObject jHistoricData) {
		try {
			String mode = JsonUtils.getAsString(jHistoricData, "mode");

			if (mode.equals("query")) {
				/*
				 * Query historic data
				 */
				int timezoneDiff = JsonUtils.getAsInt(jHistoricData, "timezone");
				ZoneId timezone = ZoneId.ofOffset("", ZoneOffset.ofTotalSeconds(timezoneDiff * -1));
				ZonedDateTime fromDate = JsonUtils.getAsZonedDateTime(jHistoricData, "fromDate", timezone);
				ZonedDateTime toDate = JsonUtils.getAsZonedDateTime(jHistoricData, "toDate", timezone).plusDays(1);
				JsonObject channels = JsonUtils.getAsJsonObject(jHistoricData, "channels");
				// TODO check if role is allowed to read these channels
				// JsonObject kWh = JsonUtils.getAsJsonObject(jQuery, "kWh");
				int days = Period.between(fromDate.toLocalDate(), toDate.toLocalDate()).getDays();
				// TODO: better calculation of sensible resolution
				int resolution = 10 * 60; // 10 Minutes
				if (days > 25) {
					resolution = 24 * 60 * 60; // 1 Day
				} else if (days > 6) {
					resolution = 3 * 60 * 60; // 3 Hours
				} else if (days > 2) {
					resolution = 60 * 60; // 60 Minutes
				}
				JsonArray jData = this.parent.timeDataService.queryHistoricData(edgeId, fromDate, toDate, channels,
						resolution);
				// send reply
				return DefaultMessages.historicDataQueryReply(jMessageId, jData);
			}
		} catch (Exception e) {
			// TODO handle exception
			log.warn(e.getMessage());
		}
		return new JsonObject();
	}

	private String getUserName(WebsocketData data) {
		Optional<User> userOpt = this.parent.metadataService.getUser(data.getUserId());
		if (userOpt.isPresent()) {
			return userOpt.get().getName();
		} else {
			return "ID:" + data.getUserId();
		}
	}

	public void handleEdgeReply(int edgeId, JsonObject jMessage) {
		try {
			JsonObject jMessageId = JsonUtils.getAsJsonObject(jMessage, "messageId");
			String backendId = JsonUtils.getAsString(jMessageId, "backend");
			WebSocket websocket = this.websocketsMap.get(UUID.fromString(backendId));
			if (websocket != null) {
				JsonObject j = DefaultMessages.prepareMessageForForwardToUi(jMessage);
				WebSocketUtils.send(websocket, j);
				return;
			}
			throw new OpenemsException("No websocket found for UUID [" + backendId + "]");
		} catch (OpenemsException e) {
			log.error("Unable to handle reply from Edge [ID:" + edgeId + "]: " + e.getMessage());
		}
	}
}
