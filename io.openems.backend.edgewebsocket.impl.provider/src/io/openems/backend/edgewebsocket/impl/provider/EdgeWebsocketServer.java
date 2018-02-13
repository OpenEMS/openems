package io.openems.backend.edgewebsocket.impl.provider;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.java_websocket.WebSocket;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.osgi.service.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.backend.common.events.BackendEventConstants;
import io.openems.backend.metadata.api.Device;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.websocket.DefaultMessages;
import io.openems.common.websocket.WebSocketUtils;

public class EdgeWebsocketServer extends AbstractWebsocketServer {

	private final EdgeWebsocket parent;
	private final Map<Integer, WebSocket> websocketsMap = new HashMap<>();
	private final Logger log = LoggerFactory.getLogger(EdgeWebsocketServer.class);

	public EdgeWebsocketServer(EdgeWebsocket parent, int port) {
		super(port);
		this.parent = parent;
	}

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
								"Another device with this apikey [" + apikey + "] connected.");
					}
					// add websocket to local cache
					this.websocketsMap.put(edgeId, websocket);
				}
			}

			// send successful reply to openems
			JsonObject jReply = DefaultMessages.openemsConnectionSuccessfulReply();
			WebSocketUtils.send(websocket, jReply);

			// announce device as online
			for (int edgeId : edgeIds) {
				Map<String, Object> properties = new HashMap<>();
				properties.put(BackendEventConstants.PROPERTY_KEY_EDGE_ID, edgeId);
				Event event = new Event(BackendEventConstants.TOPIC_EDGE_ONLINE, properties);
				this.parent.eventAdmin.postEvent(event);
			}

			// log
			for (int edgeId : edgeIds) {
				Optional<Device> deviceOpt = this.parent.metadataService.getDevice(edgeId);
				if (deviceOpt.isPresent()) {
					log.info("Device [" + deviceOpt.get() + "] connected.");
				} else {
					log.info("Device [ID:" + edgeId + "] connected.");
				}
			}

			// TODO do this in Metadata
			// try {
			// // set device active (in Odoo)
			// for (MetadataDevice device : devices) {
			// if (device.getState().equals("inactive")) {
			// device.setState("active");
			// }
			// device.setLastMessage();
			// device.writeObject();
			// }
			// } catch (OpenemsException e) {
			// // this error does not stop the connection
			// log.error("Device [" + String.join(",", deviceNames) + "] error: " +
			// e.getMessage());
			// }

		} catch (OpenemsException e) {
			// send connection failed to OpenEMS
			JsonObject jReply = DefaultMessages.openemsConnectionFailedReply(e.getMessage());
			WebSocketUtils.send(websocket, jReply);
			// close websocket
			websocket.closeConnection(CloseFrame.REFUSE, "OpenEMS connection failed. Apikey [" + apikey + "]");
		}
	}

	@Override
	protected void _onMessage(WebSocket websocket, JsonObject jMessage, Optional<JsonArray> jMessageIdOpt,
			Optional<String> deviceNameOpt) {
		System.out.println("_onMessage");
	}

	@Override
	protected void _onError(WebSocket websocket, Exception ex) {
		System.out.println("_onError");
	}

	@Override
	protected void _onClose(WebSocket websocket) {
		System.out.println("_onClose");
	}
}
