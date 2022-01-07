package io.openems.backend.edgewebsocket;

import java.util.concurrent.CompletableFuture;

import org.java_websocket.WebSocket;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.backend.common.edgewebsocket.EdgeWebsocket;
import io.openems.backend.common.metadata.Metadata;
import io.openems.backend.common.metadata.User;
import io.openems.backend.common.timedata.Timedata;
import io.openems.backend.common.uiwebsocket.UiWebsocket;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.notification.SystemLogNotification;
import io.openems.common.jsonrpc.request.AuthenticatedRpcRequest;
import io.openems.common.jsonrpc.request.SubscribeSystemLogRequest;
import io.openems.common.jsonrpc.response.AuthenticatedRpcResponse;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = "Edge.Websocket", //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		immediate = true //
)
public class EdgeWebsocketImpl extends AbstractOpenemsBackendComponent implements EdgeWebsocket {

	private final Logger log = LoggerFactory.getLogger(EdgeWebsocketImpl.class);

	private WebsocketServer server = null;

	private final SystemLogHandler systemLogHandler;

	@Reference
	protected volatile Metadata metadata;

	@Reference
	protected volatile Timedata timedata;

	@Reference(cardinality = ReferenceCardinality.OPTIONAL)
	protected volatile UiWebsocket uiWebsocket;

	public EdgeWebsocketImpl() {
		super("Edge.Websocket");
		this.systemLogHandler = new SystemLogHandler(this);
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
		this.server = new WebsocketServer(this, this.getName(), port, poolSize, debugMode);
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

	/**
	 * Gets whether the Websocket for this Edge is connected.
	 *
	 * @param edgeId the Edge-ID
	 * @return true if it is online
	 */
	protected boolean isOnline(String edgeId) {
		return this.server.isOnline(edgeId);
	}

	@Override
	public CompletableFuture<JsonrpcResponseSuccess> send(String edgeId, User user, JsonrpcRequest request)
			throws OpenemsNamedException {
		var ws = this.getWebSocketForEdgeId(edgeId);
		if (ws == null) {
			throw OpenemsError.BACKEND_EDGE_NOT_CONNECTED.exception(edgeId);
		}
		WsData wsData = ws.getAttachment();
		// Wrap Request in AuthenticatedRpc
		var authenticatedRpc = new AuthenticatedRpcRequest<>(edgeId, user, request);
		var responseFuture = wsData.send(authenticatedRpc);

		// Unwrap Response
		var result = new CompletableFuture<JsonrpcResponseSuccess>();
		responseFuture.whenComplete((r, ex) -> {
			if (ex != null) {
				result.completeExceptionally(ex);
			} else if (r != null) {
				try {
					var response = AuthenticatedRpcResponse.from(r);
					result.complete(response.getPayload());
				} catch (OpenemsNamedException e) {
					this.logError(this.log, e.getMessage());
					result.completeExceptionally(e);
				}
			} else {
				result.completeExceptionally(
						new OpenemsNamedException(OpenemsError.JSONRPC_UNHANDLED_METHOD, request.getMethod()));
			}
		});
		return result;
	}

	@Override
	public void send(String edgeId, JsonrpcNotification notification) throws OpenemsException {
		var ws = this.getWebSocketForEdgeId(edgeId);
		if (ws != null) {
			WsData wsData = ws.getAttachment();
			wsData.send(notification);
		}
	}

	/**
	 * Gets the WebSocket connection for an Edge-ID. If more than one connection
	 * exists, the first one is returned. Returns null if none is found.
	 *
	 * @param edgeId the Edge-ID
	 * @return the WebSocket connection
	 */
	private final WebSocket getWebSocketForEdgeId(String edgeId) {
		for (WebSocket ws : this.server.getConnections()) {
			WsData wsData = ws.getAttachment();
			var wsEdgeIdOpt = wsData.getEdgeId();
			if (wsEdgeIdOpt.isPresent() && wsEdgeIdOpt.get().equals(edgeId)) {
				return ws;
			}
		}
		return null;
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
	public CompletableFuture<JsonrpcResponseSuccess> handleSubscribeSystemLogRequest(String edgeId, User user,
			String token, SubscribeSystemLogRequest request) throws OpenemsNamedException {
		return this.systemLogHandler.handleSubscribeSystemLogRequest(edgeId, user, token, request);
	}

	/**
	 * Handles a {@link SystemLogNotification}, i.e. the replies to
	 * {@link SubscribeSystemLogRequest}.
	 *
	 * @param edgeId       the Edge-ID
	 * @param notification the SystemLogNotification
	 */
	public void handleSystemLogNotification(String edgeId, SystemLogNotification notification) {
		this.systemLogHandler.handleSystemLogNotification(edgeId, null, notification);
	}
}
