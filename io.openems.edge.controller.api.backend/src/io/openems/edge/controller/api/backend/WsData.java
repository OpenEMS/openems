package io.openems.edge.controller.api.backend;

import org.java_websocket.WebSocket;

public class WsData extends io.openems.common.websocket.WsData {

	public WsData(WebSocket ws) {
		super(ws);
	}

	@Override
	public String toString() {
		return "BackendApi.WsData []";
	}

}
