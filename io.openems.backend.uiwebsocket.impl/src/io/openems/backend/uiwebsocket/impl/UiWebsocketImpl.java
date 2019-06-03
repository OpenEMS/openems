package io.openems.backend.uiwebsocket.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import io.openems.common.access_control.AccessControl;
import org.java_websocket.WebSocket;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;

import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.backend.edgewebsocket.api.EdgeWebsocket;
import io.openems.backend.metadata.api.BackendUser;
import io.openems.backend.metadata.api.Metadata;
import io.openems.backend.timedata.api.Timedata;
import io.openems.backend.uiwebsocket.api.UiWebsocket;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.session.Role;

@Designate(ocd = Config.class, factory = false)
@Component(name = "Ui.Websocket", configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true)
public class UiWebsocketImpl extends AbstractOpenemsBackendComponent implements UiWebsocket {

	// private final Logger log = LoggerFactory.getLogger(UiWebsocket.class);

	protected WebsocketServer server = null;

	@Reference
	protected volatile Metadata metadata;

	@Reference
	protected volatile EdgeWebsocket edgeWebsocket;

	@Reference
	protected volatile Timedata timeData;

	@Reference
	protected AccessControl accessControl;

	public UiWebsocketImpl() {
		super("Ui.Websocket");
	}

	@Activate
	void activate(Config config) {
		this.startServer(config.port());
	}

	@Deactivate
	void deactivate() {
		this.stopServer();
	}

	/**
	 * Create and start new server.
	 * 
	 * @param port the port
	 */
	private synchronized void startServer(int port) {
		this.server = new WebsocketServer(this, "Ui.Websocket", port);
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
	public void send(UUID token, JsonrpcNotification notification) throws OpenemsNamedException {
		WsData wsData = this.getWsDataForTokenOrError(token);
		wsData.send(notification);
	}

	@Override
	public CompletableFuture<JsonrpcResponseSuccess> send(UUID token, JsonrpcRequest request)
			throws OpenemsNamedException {
		WsData wsData = this.getWsDataForTokenOrError(token);
		return wsData.send(request);
	}

	@Override
	public void send(String edgeId, JsonrpcNotification notification) throws OpenemsNamedException {
		List<WsData> wsDatas = this.getWsDatasForEdgeId(edgeId);
		for (WsData wsData : wsDatas) {
			wsData.send(notification);
		}
	}

	/**
	 * Gets the WebSocket connection attachment for a UI token.
	 * 
	 * @param token the UI token
	 * @return the WsData
	 * @throws OpenemsNamedException if there is no connection with this token
	 */
	private WsData getWsDataForTokenOrError(UUID token) throws OpenemsNamedException {
		Collection<WebSocket> connections = this.server.getConnections();
		for (Iterator<WebSocket> iter = connections.iterator(); iter.hasNext();) {
			WebSocket websocket = iter.next();
			WsData wsData = websocket.getAttachment();
			Optional<UUID> thisToken = wsData.getToken();
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
		List<WsData> result = new ArrayList<>();
		Collection<WebSocket> connections = this.server.getConnections();
		for (Iterator<WebSocket> iter = connections.iterator(); iter.hasNext();) {
			WebSocket websocket = iter.next();
			WsData wsData = websocket.getAttachment();
			// get attachment User-ID
			Optional<String> userIdOpt = wsData.getUserId();
			if (userIdOpt.isPresent()) {
				String userId = userIdOpt.get();
				// get BackendUser for User-ID
				Optional<BackendUser> userOpt = this.metadata.getUser(userId);
				if (userOpt.isPresent()) {
					BackendUser user = userOpt.get();
					Optional<Role> edgeRoleOpt = user.getEdgeRole(edgeId);
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
