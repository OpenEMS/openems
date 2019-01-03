package io.openems.backend.edgewebsocket.impl;

import java.util.Optional;
import java.util.function.Consumer;

import org.java_websocket.WebSocket;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.backend.edgewebsocket.api.EdgeWebsocket;
import io.openems.backend.metadata.api.Metadata;
import io.openems.backend.timedata.api.Timedata;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.Error;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;

@Designate(ocd = Config.class, factory = false)
@Component(name = "Edge.Websocket", configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true)
public class EdgeWebsocketImpl implements EdgeWebsocket {

//	private final Logger log = LoggerFactory.getLogger(EdgeWebsocketImpl.class);

	private WebsocketServer server = null;

	@Reference
	protected volatile Metadata metadata;

	@Reference
	protected volatile Timedata timedata;

	@Activate
	void activate(Config config) {
		this.startServer(config.port());
	}

	@Deactivate
	void deactivate() {
		this.stopServer();
	}

	/**
	 * Create and start new server
	 * 
	 * @param port
	 */
	private synchronized void startServer(int port) {
		this.server = new WebsocketServer(this, "Edge.Websocket", port);
		this.server.start();
	}

	/**
	 * Stop existing websocket server
	 */
	private synchronized void stopServer() {
		if (this.server != null) {
			this.server.stop();
		}
	}

	/**
	 * Gets whether the Websocket for this Edge is connected
	 * 
	 * @param edgeId
	 * @return
	 */
	protected boolean isOnline(String edgeId) {
		return this.server.isOnline(edgeId);
	}

	/**
	 * Sends a JsonrpcNotification to an OpenEMS Edge.
	 * 
	 * @param edgeId
	 * @param request
	 * @throws OpenemsException
	 */
	public void sendNotification(String edgeId, JsonrpcNotification notification) throws OpenemsException {
		// TODO
	}

	@Override
	public void send(String edgeId, JsonrpcRequest request, Consumer<JsonrpcResponse> responseCallback)
			throws OpenemsException {
		WebSocket ws = this.getWebSocketForEdgeId(edgeId);
		if (ws != null) {
			WsData wsData = ws.getAttachment();
			wsData.send(request, responseCallback);
		} else {
			responseCallback.accept(Error.EDGE_NOT_CONNECTED.asJsonrpc(request.getId(), edgeId));
		}
	}

	@Override
	public void send(String edgeId, JsonrpcNotification notification) throws OpenemsException {
		WebSocket ws = this.getWebSocketForEdgeId(edgeId);
		if (ws != null) {
			WsData wsData = ws.getAttachment();
			wsData.send(notification);
		}
	}

	/**
	 * Gets the WebSocket connection for an EdgeId. If more than one connection
	 * exists, the first one is returned. Returns null if none is found.
	 * 
	 * @param edgeId
	 * @return
	 */
	private final WebSocket getWebSocketForEdgeId(String edgeId) {
		for (WebSocket ws : this.server.getConnections()) {
			WsData wsData = ws.getAttachment();
			Optional<String> wsEdgeId = wsData.getEdgeId();
			if (wsEdgeId.isPresent() && wsEdgeId.get().equals(edgeId)) {
				return ws;
			}
		}
		return null;
	}

}
