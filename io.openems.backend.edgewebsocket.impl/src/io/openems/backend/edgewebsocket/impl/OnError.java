package io.openems.backend.edgewebsocket.impl;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.websocket.AbstractOnError;

public class OnError extends AbstractOnError {

	private final Logger log = LoggerFactory.getLogger(OnError.class);
	private final EdgeWebsocketServer parent;

	public OnError(EdgeWebsocketServer parent, WebSocket websocket, Exception ex) {
		super(websocket, ex);
		this.parent = parent;
	}

	@Override
	protected void run(WebSocket websocket, Exception ex) {
		Attachment attachment = websocket.getAttachment();
		int[] edgeIds = attachment.getEdgeIds();
		if (websocket == null || edgeIds.length == 0) {
			log.warn("Edge [UNKNOWN] websocket error: " + ex.getMessage());
		} else {
			for (String edgeName : this.parent.getEdgeNames(edgeIds)) {
				log.warn("Edge [" + edgeName + "] websocket error: " + ex.getMessage());
			}
		}
	}

}
