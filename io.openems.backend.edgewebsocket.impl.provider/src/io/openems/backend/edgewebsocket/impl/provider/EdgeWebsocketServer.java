package io.openems.backend.edgewebsocket.impl.provider;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.java_websocket.WebSocket;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.osgi.service.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.backend.common.events.BackendEventConstants;
import io.openems.backend.metadata.api.Edge;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.StringUtils;
import io.openems.common.websocket.AbstractWebsocketServer;
import io.openems.common.websocket.DefaultMessages;
import io.openems.common.websocket.LogBehaviour;
import io.openems.common.websocket.Notification;
import io.openems.common.websocket.WebSocketUtils;

public class EdgeWebsocketServer extends AbstractWebsocketServer {

	private final EdgeWebsocket parent;
	private final Map<Integer, WebSocket> websocketsMap = new HashMap<>();
	private final Logger log = LoggerFactory.getLogger(EdgeWebsocketServer.class);

	public EdgeWebsocketServer(EdgeWebsocket parent, int port) {
		super(port);
		this.parent = parent;
	}

	/**
	 * Open event of websocket. Parses the "apikey" and to authenticate Edge.
	 */
	@Override
	protected void _onOpen(WebSocket websocket, ClientHandshake handshake) {
		String apikey = "";
		try {
			// get apikey from handshake
			Optional<String> apikeyOpt = Utils.parseApikeyFromHandshake(handshake);
			if (!apikeyOpt.isPresent()) {
				throw new OpenemsException("Apikey is missing in handshake");
			}
			apikey = apikeyOpt.get();

			// get edgeId for apikey
			int[] edgeIds = this.parent.metadataService.getEdgeIdsForApikey(apikey);

			// if existing: close existing websocket for this apikey
			synchronized (this.websocketsMap) {
				for (int edgeId : edgeIds) {
					if (this.websocketsMap.containsKey(edgeId)) {
						WebSocket oldWebsocket = this.websocketsMap.get(edgeId);
						oldWebsocket.closeConnection(CloseFrame.REFUSE,
								"Another Edge with this apikey [" + apikey + "] connected.");
					}
					// add websocket to local cache
					this.websocketsMap.put(edgeId, websocket);
				}
			}

			// store edgeIds together with WebSocket
			websocket.setAttachment(edgeIds);

			// send successful reply to openems
			JsonObject jReply = DefaultMessages.openemsConnectionSuccessfulReply();
			WebSocketUtils.send(websocket, jReply);

			// announce Edge as online
			for (int edgeId : edgeIds) {
				Map<String, Object> properties = new HashMap<>();
				properties.put(BackendEventConstants.PROPERTY_KEY_EDGE_ID, edgeId);
				Event event = new Event(BackendEventConstants.TOPIC_EDGE_ONLINE, properties);
				this.parent.eventAdmin.postEvent(event);
			}

			// log
			for (int edgeId : edgeIds) {
				Optional<Edge> edgeOpt = this.parent.metadataService.getEdgeOpt(edgeId);
				if (edgeOpt.isPresent()) {
					Edge edge = edgeOpt.get();
					log.info("Edge [" + edge.getName() + "] " //
							+ (edgeIds.length > 1 ? ", ID [" + edgeId + "]" : "") //
							+ "connected. Apikey [" + apikey + "]. Websocket [" + websocket + "]");
					// set last update timestamps in MetadataService
					edge.setLastMessage();
				} else {
					log.info("Edge [ID:" + edgeId + "] connected. Apikey [" + apikey + "]. Websocket [" + websocket
							+ "]");
				}
			}
		} catch (OpenemsException e) {
			// send connection failed to OpenEMS
			JsonObject jReply = DefaultMessages.openemsConnectionFailedReply(e.getMessage());
			WebSocketUtils.sendOrLogError(websocket, jReply);
			// close websocket
			websocket.closeConnection(CloseFrame.REFUSE,
					"Connection to backend failed. Apikey [" + apikey + "]. Error: " + e.getMessage());
		}
	}

	/**
	 * Message event of websocket. Handles a new message. At this point the Edge is
	 * already authenticated.
	 */
	@Override
	protected void _onMessage(WebSocket websocket, JsonObject jMessage) {
		// get edgeIds from websocket
		int[] edgeIds = websocket.getAttachment();

		// set last update timestamps in MetadataService
		for (int edgeId : edgeIds) {
			Optional<Edge> edgeOpt = this.parent.metadataService.getEdgeOpt(edgeId);
			if (edgeOpt.isPresent()) {
				Edge edge = edgeOpt.get();
				edge.setLastMessage();
			}
		}

		// get MessageId from message
		JsonObject jMessageId = JsonUtils.getAsOptionalJsonObject(jMessage, "messageId").orElse(new JsonObject());

		/*
		 * Config? -> store in Metadata
		 */
		Optional<JsonObject> jConfigOpt = JsonUtils.getAsOptionalJsonObject(jMessage, "config");
		if (jConfigOpt.isPresent()) {
			JsonObject jConfig = jConfigOpt.get();
			for (int edgeId : edgeIds) {
				Edge edge;
				try {
					edge = this.parent.metadataService.getEdge(edgeId);
					edge.setConfig(jConfig);
				} catch (OpenemsException e) {
					WebSocketUtils.sendNotificationOrLogError(websocket, jMessageId, LogBehaviour.WRITE_TO_LOG,
							Notification.METADATA_ERROR, e.getMessage());
				}
			}
			return;
		}

		/*
		 * Is this a reply? -> forward to UI
		 */
		if (jMessage.has("messageId")) {
			for (int edgeId : edgeIds) {
				try {
					this.parent.uiWebsocketService.handleEdgeReply(edgeId, jMessage);
				} catch (OpenemsException e) {
					WebSocketUtils.sendNotificationOrLogError(websocket, jMessageId, LogBehaviour.WRITE_TO_LOG,
							Notification.EDGE_UNABLE_TO_FORWARD, "ID:" + edgeId, e.getMessage());
				}
			}
			return;
		}

		/*
		 * New timestamped data
		 */
		Optional<JsonObject> jTimedataOpt = JsonUtils.getAsOptionalJsonObject(jMessage, "timedata");
		if (jTimedataOpt.isPresent()) {
			timedata(edgeIds, jTimedataOpt.get());
			return;
		}

		/*
		 * Unknown message
		 */
		for (String edgeName : getEdgeNames(edgeIds)) {
			WebSocketUtils.sendNotificationOrLogError(websocket, jMessageId, LogBehaviour.WRITE_TO_LOG,
					Notification.UNKNOWN_MESSAGE, edgeName, StringUtils.toShortString(jMessage, 100));
		}
	}

	@Override
	protected void _onError(WebSocket websocket, Exception ex) {
		if (websocket == null) {
			log.warn("Edge [UNKNOWN] websocket error: " + ex.getMessage());
		} else {
			int[] edgeIds = websocket.getAttachment();
			for (String edgeName : getEdgeNames(edgeIds)) {
				log.warn("Edge [" + edgeName + "] websocket error: " + ex.getMessage());
			}
		}
	}

	@Override
	protected void _onClose(WebSocket websocket) {
		// get edgeIds from websocket
		int[] edgeIds = websocket.getAttachment();

		// remove websocket from local map
		for (int edgeId : edgeIds) {
			synchronized (this.websocketsMap) {
				this.websocketsMap.remove(edgeId, websocket);
			}
		}

		// announce Edge as offline
		for (int edgeId : edgeIds) {
			Map<String, Object> properties = new HashMap<>();
			properties.put(BackendEventConstants.PROPERTY_KEY_EDGE_ID, edgeId);
			Event event = new Event(BackendEventConstants.TOPIC_EDGE_OFFLINE, properties);
			this.parent.eventAdmin.postEvent(event);
		}

		// log
		for (String edgeName : getEdgeNames(edgeIds)) {
			log.info("Edge [" + edgeName + "] disconnected.");
		}
	}

	public boolean isOnline(int edgeId) {
		return this.websocketsMap.containsKey(edgeId);
	}

	private void timedata(int[] edgeIds, JsonObject jTimedata) {
		for (int edgeId : edgeIds) {
			Edge edge;
			try {
				edge = this.parent.metadataService.getEdge(edgeId);
			} catch (OpenemsException e) {
				log.warn(e.getMessage());
				continue;
			}
			/*
			 * write data to timedataService
			 */
			try {
				this.parent.timedataService.write(edgeId, jTimedata);
				log.debug("Edge [" + edge.getName() + "] wrote " + jTimedata.entrySet().size() + " timestamps "
						+ StringUtils.toShortString(jTimedata, 120));
			} catch (Exception e) {
				log.error("Unable to write Timedata: " + e.getClass().getSimpleName() + ": " + e.getMessage());
			}

			for (Entry<String, JsonElement> jTimedataEntry : jTimedata.entrySet()) {
				try {
					JsonObject jChannels = JsonUtils.getAsJsonObject(jTimedataEntry.getValue());
					// set Odoo last update timestamp only for those channels
					for (String channel : jChannels.keySet()) {
						if (channel.endsWith("ActivePower")
								|| channel.endsWith("ActivePowerL1") | channel.endsWith("ActivePowerL2")
										| channel.endsWith("ActivePowerL3") | channel.endsWith("Soc")) {
							edge.setLastUpdate();
						}
					}

					// set specific Odoo values
					if (jChannels.has("ess0/Soc")) {
						int soc = JsonUtils.getAsPrimitive(jChannels, "ess0/Soc").getAsInt();
						edge.setSoc(soc);
					}
					if (jChannels.has("system0/PrimaryIpAddress")) {
						String ipv4 = JsonUtils.getAsPrimitive(jChannels, "system0/PrimaryIpAddress").getAsString();
						edge.setIpv4(ipv4);
					}
				} catch (OpenemsException e) {
					log.error("Edgde [" + edge.getName() + "] error: " + e.getMessage());
				}
			}
		}
	}

	private String[] getEdgeNames(int[] edgeIds) {
		String[] edgeNames = new String[edgeIds.length];
		for (int i = 0; i < edgeIds.length; i++) {
			Optional<Edge> edgeOpt = this.parent.metadataService.getEdgeOpt(edgeIds[i]);
			if (edgeOpt.isPresent()) {
				edgeNames[i] = edgeOpt.get().getName();
			} else {
				edgeNames[i] = "ID:" + edgeIds[i];
			}
		}
		return edgeNames;
	}

	public void forwardMessageFromUi(int edgeId, JsonObject jMessage) throws OpenemsException {
		WebSocket websocket = this.websocketsMap.get(edgeId);
		if (websocket != null) {
			WebSocketUtils.send(websocket, jMessage);
		}
	}
}
