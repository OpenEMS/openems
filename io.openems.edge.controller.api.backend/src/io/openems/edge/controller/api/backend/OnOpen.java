package io.openems.edge.controller.api.backend;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.notification.EdgeConfigNotification;

public class OnOpen implements io.openems.common.websocket.OnOpen {

	private final Logger log = LoggerFactory.getLogger(OnOpen.class);
	private final ControllerApiBackendImpl parent;

	public OnOpen(ControllerApiBackendImpl parent) {
		this.parent = parent;
	}

	@Override
	public void run(WebSocket ws, JsonObject handshake) {
		this.parent.logInfo(this.log, "Connected to OpenEMS Backend");

		// Immediately send Config
		var config = this.parent.componentManager.getEdgeConfig();
		var message = new EdgeConfigNotification(config);
		this.parent.websocket.sendMessage(message);

		// Send all Channel values
		this.parent.sendChannelValuesWorker.sendValuesOfAllChannelsOnce();
	}

}
