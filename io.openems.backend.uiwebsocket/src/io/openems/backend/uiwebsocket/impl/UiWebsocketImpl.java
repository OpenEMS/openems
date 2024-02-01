package io.openems.backend.uiwebsocket.impl;

import static java.util.stream.Collectors.toUnmodifiableMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.backend.common.debugcycle.DebugLoggable;
import io.openems.backend.common.edgewebsocket.EdgeCache;
import io.openems.backend.common.edgewebsocket.EdgeWebsocket;
import io.openems.backend.common.jsonrpc.JsonRpcRequestHandler;
import io.openems.backend.common.metadata.Metadata;
import io.openems.backend.common.metadata.User;
import io.openems.backend.common.timedata.TimedataManager;
import io.openems.backend.common.uiwebsocket.UiWebsocket;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.AbstractJsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = "Ui.Websocket", //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		immediate = true //
)
@EventTopics({ //
		Metadata.Events.AFTER_IS_INITIALIZED //
})
public class UiWebsocketImpl extends AbstractOpenemsBackendComponent
		implements UiWebsocket, EventHandler, DebugLoggable {

	private static final String COMPONENT_ID = "uiwebsocket0";

	protected WebsocketServer server = null;

	@Reference
	protected volatile JsonRpcRequestHandler jsonRpcRequestHandler;

	@Reference
	protected volatile Metadata metadata;

	@Reference
	protected volatile EdgeWebsocket edgeWebsocket;

	@Reference
	protected volatile TimedataManager timedataManager;

	public UiWebsocketImpl() {
		super("Ui.Websocket");
	}

	private Config config;

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
		if (this.server == null) {
			return;
		}
		this.server.stop();
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
	protected void logError(Logger log, String message) {
		super.logError(log, message);
	}

	@Override
	public void send(UUID websocketId, JsonrpcNotification notification) throws OpenemsNamedException {
		var wsData = this.getWsDataForIdOrError(websocketId);
		wsData.send(notification);
	}

	@Override
	public CompletableFuture<JsonrpcResponseSuccess> send(UUID websocketId, JsonrpcRequest request)
			throws OpenemsNamedException {
		var wsData = this.getWsDataForIdOrError(websocketId);
		return wsData.send(request);
	}

	@Override
	public void sendBroadcast(String edgeId, JsonrpcNotification notification) throws OpenemsNamedException {
		if (this.server == null) {
			return;
		}
		var wsDatas = this.getWsDatasForEdgeId(edgeId);
		OpenemsNamedException exception = null;
		for (WsData wsData : wsDatas) {
			if (!wsData.isEdgeSubscribed(edgeId)) {
				continue;
			}
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
	 * @param websocketId the id of the websocket connection
	 * @return the WsData
	 * @throws OpenemsNamedException if there is no connection with this token
	 */
	private WsData getWsDataForIdOrError(UUID websocketId) throws OpenemsNamedException {
		if (this.server == null) {
			throw new OpenemsException("Server is not yet fully initialized");
		}
		var connections = this.server.getConnections();
		for (var websocket : connections) {
			WsData wsData = websocket.getAttachment();
			if (wsData.getId().equals(websocketId)) {
				return wsData;
			}
		}
		throw OpenemsError.BACKEND_NO_UI_WITH_TOKEN.exception(websocketId);
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
		for (var websocket : connections) {
			WsData wsData = websocket.getAttachment();
			if (wsData == null) {
				continue;
			}
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

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case Metadata.Events.AFTER_IS_INITIALIZED:
			this.startServer();
			break;
		}
	}

	@Override
	public void sendSubscribedChannels(String edgeId, EdgeCache edgeCache) {
		if (this.server == null) {
			return;
		}
		var connections = this.server.getConnections();
		for (var websocket : connections) {
			WsData wsData = websocket.getAttachment();
			if (wsData != null) {
				wsData.sendSubscribedChannels(edgeId, edgeCache);
			}
		}
	}

	/**
	 * Gets the authenticated User or throws an Exception if User is not
	 * authenticated.
	 *
	 * @param wsData  the WebSocket attachment
	 * @param request the {@link AbstractJsonrpcRequest}
	 * @return the {@link User}
	 * @throws OpenemsNamedException if User is not authenticated
	 */
	protected User assertUser(WsData wsData, AbstractJsonrpcRequest request) throws OpenemsNamedException {
		var userIdOpt = wsData.getUserId();
		if (!userIdOpt.isPresent()) {
			throw OpenemsError.COMMON_USER_NOT_AUTHENTICATED
					.exception("User-ID is empty. Ignoring request [" + request.getMethod() + "]");
		}
		var userOpt = this.metadata.getUser(userIdOpt.get());
		if (!userOpt.isPresent()) {
			throw OpenemsError.COMMON_USER_NOT_AUTHENTICATED.exception("User with ID [" + userIdOpt.get()
					+ "] is unknown. Ignoring request [" + request.getMethod() + "]");
		}
		return userOpt.get();
	}

	public String getId() {
		return COMPONENT_ID;
	}

	@Override
	public String debugLog() {
		return new StringBuilder() //
				.append("[").append(this.getName()).append("] ") //
				.append(this.server != null //
						? this.server.debugLog() //
						: "NOT STARTED") //
				.toString();
	}

	@Override
	public Map<String, JsonElement> debugMetrics() {
		if (this.server == null) {
			return null;
		}

		return this.server.debugMetrics().entrySet().stream() //
				.collect(toUnmodifiableMap(//
						e -> this.getId() + "/" + e.getKey(), //
						e -> new JsonPrimitive(e.getValue())));
	}

}
