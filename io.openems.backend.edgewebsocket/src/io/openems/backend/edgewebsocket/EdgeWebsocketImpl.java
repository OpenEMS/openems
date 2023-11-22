package io.openems.backend.edgewebsocket;

import java.time.Instant;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.java_websocket.WebSocket;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.TreeBasedTable;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.backend.common.debugcycle.DebugCycle;
import io.openems.backend.common.edgewebsocket.EdgeWebsocket;
import io.openems.backend.common.metadata.AppCenterMetadata;
import io.openems.backend.common.metadata.Metadata;
import io.openems.backend.common.metadata.User;
import io.openems.backend.common.timedata.TimedataManager;
import io.openems.backend.common.uiwebsocket.UiWebsocket;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.notification.SystemLogNotification;
import io.openems.common.jsonrpc.notification.TimestampedDataNotification;
import io.openems.common.jsonrpc.request.AuthenticatedRpcRequest;
import io.openems.common.jsonrpc.request.SubscribeSystemLogRequest;
import io.openems.common.jsonrpc.response.AuthenticatedRpcResponse;
import io.openems.common.types.ChannelAddress;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = "Edge.Websocket", //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		immediate = true //
)
@EventTopics({ //
		Metadata.Events.AFTER_IS_INITIALIZED //
})
public class EdgeWebsocketImpl extends AbstractOpenemsBackendComponent
		implements EdgeWebsocket, EventHandler, DebugCycle {

	private static final String EDGE_ID = "backend0";
	private static final String COMPONENT_ID = "edgewebsocket0";

	private final Logger log = LoggerFactory.getLogger(EdgeWebsocketImpl.class);
	private final SystemLogHandler systemLogHandler;

	@Reference
	protected volatile Metadata metadata;

	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
	protected volatile AppCenterMetadata.EdgeData appCenterMetadata;

	@Reference
	protected volatile TimedataManager timedataManager;

	@Reference
	protected volatile EventAdmin eventAdmin;

	@Reference(cardinality = ReferenceCardinality.OPTIONAL)
	protected volatile UiWebsocket uiWebsocket;

	private WebsocketServer server = null;
	private Config config;

	public EdgeWebsocketImpl() {
		super("Edge.Websocket");
		this.systemLogHandler = new SystemLogHandler(this);
	}

	@Activate
	private void activate(Config config) {
		this.config = config;

		if (this.metadata.isInitialized()) {
			this.startServer();
		}
	}

	@Deactivate
	private void deactivate() {
		this.stopServer();
	}

	/**
	 * Create and start new server.
	 */
	private synchronized void startServer() {
		if (this.server == null) {
			this.server = new WebsocketServer(this, this.getName(), this.config.port(), this.config.poolSize(),
					this.config.debugMode());
			this.server.start();
		}
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
		if (this.server == null) {
			return false;
		}
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
		if (this.server == null) {
			return null;
		}
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

	/**
	 * Logs a info message with Edge-ID.
	 * 
	 * @param log     the {@link Logger}
	 * @param edgeId  the Edge-ID
	 * @param message the message
	 */
	protected void logInfo(Logger log, String edgeId, String message) {
		if (edgeId == null) {
			edgeId = "UNKNOWN";
		}
		super.logInfo(log, "[" + edgeId + "] " + message);
	}

	@Override
	protected void logWarn(Logger log, String message) {
		super.logWarn(log, message);
	}

	/**
	 * Logs a warning message with Edge-ID.
	 * 
	 * @param log     the {@link Logger}
	 * @param edgeId  the Edge-ID
	 * @param message the message
	 */
	protected void logWarn(Logger log, String edgeId, String message) {
		if (edgeId == null) {
			edgeId = "UNKNOWN";
		}
		super.logWarn(log, "[" + edgeId + "] " + message);
	}

	@Override
	protected void logError(Logger log, String message) {
		super.logError(log, message);
	}

	@Override
	public CompletableFuture<JsonrpcResponseSuccess> handleSubscribeSystemLogRequest(String edgeId, User user,
			UUID websocketId, SubscribeSystemLogRequest request) throws OpenemsNamedException {
		return this.systemLogHandler.handleSubscribeSystemLogRequest(edgeId, user, websocketId, request);
	}

	/**
	 * Handles a {@link SystemLogNotification}, i.e. the replies to
	 * {@link SubscribeSystemLogRequest}.
	 *
	 * @param edgeId       the Edge-ID
	 * @param notification the SystemLogNotification
	 */
	public void handleSystemLogNotification(String edgeId, SystemLogNotification notification) {
		this.systemLogHandler.handleSystemLogNotification(edgeId, notification);
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case Metadata.Events.AFTER_IS_INITIALIZED:
			this.startServer();
			break;
		}
	}

	@Override
	public SortedMap<ChannelAddress, JsonElement> getChannelValues(String edgeId,
			Set<ChannelAddress> channelAddresses) {
		SortedMap<ChannelAddress, JsonElement> result = channelAddresses.stream() //
				.collect(Collectors.toMap(Function.identity(), c -> JsonNull.INSTANCE, (t, u) -> u, TreeMap::new));
		var ws = this.getWebSocketForEdgeId(edgeId);
		if (ws == null) {
			return result;
		}
		var wsData = (WsData) ws.getAttachment();
		if (wsData == null) {
			return result;
		}
		for (var channelAddress : channelAddresses) {
			result.put(channelAddress, wsData.edgeCache.getChannelValue(channelAddress.toString()));
		}
		return result;
	}

	public String getId() {
		return COMPONENT_ID;
	}

	@Override
	public void debugCycle() {
		var data = TreeBasedTable.<Long, String, JsonElement>create();
		var now = Instant.now().toEpochMilli();
		data.put(now, this.getId() + "/Connections",
				new JsonPrimitive(this.server != null ? this.server.getConnections().size() : 0));
		this.timedataManager.write(EDGE_ID, new TimestampedDataNotification(data));
	}

}
