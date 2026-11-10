package io.openems.backend.edge.server;

import java.util.List;

import org.java_websocket.WebSocket;

import io.openems.backend.common.edge.EdgeClient;

public class WsData extends io.openems.common.websocket.WsData implements EdgeClient {

	private static final List<String> DEBUG_EDGE_IDS = List.of();

	/**
	 * Edge-ID is set only if the connection was authenticated (i.e. apikey was
	 * correct).
	 */
	private volatile String edgeId;

	public WsData(WebSocket ws) {
		super(ws);
	}

	/* package */ void setEdgeId(String edgeId) {
		this.edgeId = edgeId;
		super.setDebug(DEBUG_EDGE_IDS.contains(edgeId));
	}

	@Override
	public String getEdgeId() {
		return this.edgeId;
	}

	@Override
	protected String toLogString() {
		return "BackendEdgeServerWsData [" + this.getEdgeIdString() + "]";
	}

}
