package io.openems.backend.edgewebsocket.impl;

import java.util.Optional;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.metadata.api.Edge;
import io.openems.common.websocket_old.AbstractOnClose;

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

		for (int edgeId : edgeIds) {
			/*
			 * if there is no other websocket connection for this edgeId -> announce Edge as
			 * offline (Another connection could have been opened in the meantime when the
			 * Edge reconnected)
			 */
			synchronized (this.parent.websocketsMap) {
				if (this.parent.websocketsMap.containsKey(edgeId)) {
					continue;
				}
			}
			Optional<Edge> edgeOpt = this.parent.parent.metadataService.getEdgeOpt(edgeId);
			if (edgeOpt.isPresent()) {
				edgeOpt.get().setOnline(false);
			}
		}

		// log
		for (String edgeName : this.parent.getEdgeNames(edgeIds)) {
			log.info("Edge [" + edgeName + "] disconnected.");
		}
	}

}
