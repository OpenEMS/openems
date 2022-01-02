package io.openems.backend.uiwebsocket.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.java_websocket.WebSocket;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;

import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.backend.common.edgewebsocket.EdgeWebsocket;
import io.openems.backend.common.jsonrpc.JsonRpcRequestHandler;
import io.openems.backend.common.metadata.Metadata;
import io.openems.backend.common.timedata.Timedata;
import io.openems.backend.common.uiwebsocket.UiWebsocket;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = "Ui.Websocket", //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		immediate = true //
)
public class UiWebsocketImpl extends AbstractOpenemsBackendComponent implements UiWebsocket {

	// private final Logger log = LoggerFactory.getLogger(UiWebsocket.class);

	protected WebsocketServer server = null;

	@Reference
	protected volatile JsonRpcRequestHandler jsonRpcRequestHandler;

	@Reference
	protected volatile Metadata metadata;

	@Reference
	protected volatile EdgeWebsocket edgeWebsocket;

	@Reference
	protected volatile Timedata timeData;

	public UiWebsocketImpl() {
		super("Ui.Websocket");
	}

	private Config config;

	private final Runnable startServerWhenMetadataIsInitialized = () -> {
		this.startServer(this.config.port(), this.config.poolSize(), this.config.debugMode());
	};

	@Activate
	private void activate(Config config) {
		this.config = config;
		this.metadata.addOnIsInitializedListener(this.startServerWhenMetadataIsInitialized);
	}

	@Deactivate
	private void deactivate() {
		this.metadata.removeOnIsInitializedListener(this.startServerWhenMetadataIsInitialized);
		this.stopServer();
	}

	/**
	 * Create and start new server.
	 *
	 * @param port      the port
	 * @param poolSize  number of threads dedicated to handle the tasks
	 * @param debugMode activate a regular debug log about the state of the tasks
	 */
	private synchronized void startServer(int port, int poolSize, boolean debugMode) {
		this.server = new WebsocketServer(this, "Ui.Websocket", port, poolSize, debugMode);
		this.server.start();
	}

	/**
	 * Stop existing websocket server.
	 */
	private synchronized void stopServer() {
		if (this.server != null) {
			this.server.stop();
		}
	}

	@Override
	protected void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}

	@Override
	protected void logWarn(Logger log, String message) {
		super.logWarn(log, message);
	}

	@Override
	public void send(String token, JsonrpcNotification notification) throws OpenemsNamedException {
		var wsData = this.getWsDataForTokenOrError(token);
		wsData.send(notification);
	}

	@Override
	public CompletableFuture<JsonrpcResponseSuccess> send(String token, JsonrpcRequest request)
			throws OpenemsNamedException {
		var wsData = this.getWsDataForTokenOrError(token);
		return wsData.send(request);
	}

	@Override
	public void sendBroadcast(String edgeId, JsonrpcNotification notification) throws OpenemsNamedException {
		var wsDatas = this.getWsDatasForEdgeId(edgeId);
		OpenemsNamedException exception = null;
		for (WsData wsData : wsDatas) {
			try {
				wsData.send(notification);
			} catch (OpenemsNamedException e) {
				exception = e;
			}
		}
		if (exception != null) {
			throw exception;
		}
	}

	/**
	 * Gets the WebSocket connection attachment for a UI token.
	 *
	 * @param token the UI token
	 * @return the WsData
	 * @throws OpenemsNamedException if there is no connection with this token
	 */
	private WsData getWsDataForTokenOrError(String token) throws OpenemsNamedException {
		var connections = this.server.getConnections();
		for (Iterator<WebSocket> iter = connections.iterator(); iter.hasNext();) {
			var websocket = iter.next();
			WsData wsData = websocket.getAttachment();
			var thisToken = wsData.getToken();
			if (thisToken.isPresent() && thisToken.get().equals(token)) {
				return wsData;
			}
		}
		throw OpenemsError.BACKEND_NO_UI_WITH_TOKEN.exception(token);
	}

	/**
	 * Gets the WebSocket connection attachments of all connections accessing an
	 * Edge-ID.
	 *
	 * @param edgeId the Edge-ID
	 * @return the WsDatas; empty list if there are none
	 */
	private List<WsData> getWsDatasForEdgeId(String edgeId) {
		var result = new ArrayList<WsData>();
		var connections = this.server.getConnections();
		for (Iterator<WebSocket> iter = connections.iterator(); iter.hasNext();) {
			var websocket = iter.next();
			WsData wsData = websocket.getAttachment();
			// get attachment User-ID
			var userIdOpt = wsData.getUserId();
			if (userIdOpt.isPresent()) {
				var userId = userIdOpt.get();
				// get User for User-ID
				var userOpt = this.metadata.getUser(userId);
				if (userOpt.isPresent()) {
					var user = userOpt.get();
					var edgeRoleOpt = user.getRole(edgeId);
					if (edgeRoleOpt.isPresent()) {
						// User has access to this Edge-ID
						result.add(wsData);
					}
				}
			}
		}
		return result;
	}

}
