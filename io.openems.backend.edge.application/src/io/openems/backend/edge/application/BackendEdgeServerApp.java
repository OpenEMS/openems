package io.openems.backend.edge.application;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import io.openems.backend.common.jsonrpc.syncable.SyncableRepository;
import io.openems.backend.edge.client.WebsocketClient;
import io.openems.backend.edge.server.WebsocketServer;
import io.openems.backend.edge.syncable.SyncableManager;
import io.openems.common.OpenemsConstants;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = "Backend.Edge.App", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class BackendEdgeServerApp {

	private final Logger log = LoggerFactory.getLogger(BackendEdgeServerApp.class);
	private final Config config;
	private final Cache cache;
	private final WebsocketClient client;
	private final CyclicTask cyclicTask;

	@Reference
	private SyncableRepository syncableRepository;

	@Reference
	private SyncableManager syncableManager;

	private WebsocketServer server;

	@Activate
	public BackendEdgeServerApp(@Reference ConfigurationAdmin cm, Config config) throws URISyntaxException {
		this.config = config;
		this.cache = new Cache(this::evaluateServerStart);

		final var message = "OpenEMS Backend Edge Application version [" + OpenemsConstants.VERSION + "] started";
		final var line = Strings.repeat("=", message.length());
		this.log.info(line);
		this.log.info(message);
		this.log.info(line);

		// Prepare Client
		this.client = new WebsocketClient("Backend.Edge.Client", new URI(config.uri()), config.id(),
				config.clientPoolSize(), //
				this::evaluateServerStart, //
				this);
		this.client.start();

		// Prepare Cyclic-Task
		this.cyclicTask = CyclicTask.from(this.client, () -> this.server);
	}

	@Deactivate
	private void deactivate() {
		this.log.debug("Deactivate BackendApp");
		this.cyclicTask.deactivate();
		this.client.deactivate();
	}

	/**
	 * Sends the given request to the specified edge id. When the edge is not
	 * available, a JSONRPC_SEND_FAILED OpenemsError is thrown.
	 * 
	 * @param edgeId  Edge id to send the request to.
	 * @param request Request to send.
	 * @return Returns the response from the edge as a CompletableFuture.
	 */
	public CompletableFuture<JsonrpcResponseSuccess> sendRequestToEdge(String edgeId, JsonrpcRequest request) {
		var server = this.server;
		if (server != null) {
			return server.sendRequestToEdge(edgeId, request);
		} else {
			return CompletableFuture.failedFuture(OpenemsError.JSONRPC_SEND_FAILED.exception());
		}
	}

	/**
	 * Sends the given notification to the specified edge id. When the fems is not
	 * available, nothing it sent. The send operation is async, but there is
	 * currently no way to wait.
	 *
	 * @param edgeId       Edge id to send the request to.
	 * @param notification Notification to send.
	 */
	public void sendNotificationToEdge(String edgeId, JsonrpcNotification notification) {
		var server = this.server;
		if (server != null) {
			server.sendNotificationToEdge(edgeId, notification);
		}
	}

	/**
	 * Starts the {@link WebsocketServer} if client is connected and cache is
	 * initialized.
	 * 
	 * @param clientOrCache either {@link WebsocketClient} connected status or
	 *                      {@link Cache} initialized status
	 */
	private synchronized void evaluateServerStart(boolean clientOrCache) {
		if (!clientOrCache) {
			this.stopServer();
		}

		if (this.client.isConnected() && this.cache.isInitialized()) {
			this.startServer();

		} else {
			this.stopServer();
		}
	}

	private synchronized void startServer() {
		if (this.server != null) {
			return;
		}
		var server = this.createServer();
		server.start();
		this.server = server;
	}

	private synchronized void stopServer() {
		if (this.server == null) {
			return;
		}
		this.server.stop();
		this.server = null;
	}

	private synchronized WebsocketServer createServer() {
		return new WebsocketServer("Backend.Edge.Server", this.config.port(), this.config.serverPoolSize(), this,
				this.cyclicTask::connectedEdgesChanged);
	}

	/**
	 * Sends a {@link JsonrpcRequest} to the Edge-Manager.
	 *
	 * @param edgeId  the Edge-ID
	 * @param request the {@link JsonrpcRequest}
	 * @return a promise for a successful JSON-RPC Response
	 */
	public CompletableFuture<JsonrpcResponseSuccess> sendRequestToEdgeManager(String edgeId, JsonrpcRequest request) {
		return this.client.sendRequestToEdgeManager(edgeId, request);
	}

	/**
	 * Sends a {@link JsonrpcNotification} to the Edge-Manager.
	 *
	 * @param edgeId       the Edge-ID
	 * @param notification the {@link JsonrpcNotification}
	 */
	public void sendNotificationToEdgeManager(String edgeId, JsonrpcNotification notification) {
		this.client.sendNotificationToEdgeManager(edgeId, notification);
	}

	public SyncableRepository getSyncableRepository() {
		return this.syncableRepository;
	}

	public SyncableManager getSyncableManager() {
		return this.syncableManager;
	}

	public Cache getCache() {
		return this.cache;
	}
}
