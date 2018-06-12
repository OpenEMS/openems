package io.openems.backend.edgewebsocket.impl;

import java.util.HashMap;
import java.util.Map;

import org.java_websocket.WebSocket;
import org.osgi.service.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.common.events.BackendEventConstants;
import io.openems.common.websocket.AbstractOnClose;

public class OnClose extends AbstractOnClose {

	private final Logger log = LoggerFactory.getLogger(OnClose.class);

	private final EdgeWebsocketServer parent;

	public OnClose(EdgeWebsocketServer parent, WebSocket websocket, int code, String reason, boolean remote) {
		super(websocket, code, reason, remote);
		this.parent = parent;
	}

	@Override
	protected void run(WebSocket websocket, int code, String reason, boolean remote) {
		// get edgeIds from websocket
		Attachment attachment = websocket.getAttachment();
		int[] edgeIds = attachment.getEdgeIds();

		// remove websocket from local map
		for (int edgeId : edgeIds) {
			synchronized (this.parent.websocketsMap) {
				this.parent.websocketsMap.remove(edgeId, websocket);
			}
		}

		// announce Edge as offline
		for (int edgeId : edgeIds) {
			Map<String, Object> properties = new HashMap<>();
			properties.put(BackendEventConstants.PROPERTY_KEY_EDGE_ID, edgeId);
			Event event = new Event(BackendEventConstants.TOPIC_EDGE_OFFLINE, properties);
			this.parent.parent.eventAdmin.postEvent(event);
		}

		// log
		for (String edgeName : this.parent.getEdgeNames(edgeIds)) {
			log.info("Edge [" + edgeName + "] disconnected.");
		}
	}

}
