package io.openems.backend.edge.manager;

import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toUnmodifiableMap;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

import org.java_websocket.util.NamedThreadFactory;
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

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.backend.common.edge.EdgeCache;
import io.openems.backend.common.edge.EdgeManager;
import io.openems.backend.common.edge.subscription.SubscriptionManager;
import io.openems.backend.common.jsonrpc.syncable.SyncableRepository;
import io.openems.backend.common.metadata.AppCenterMetadata;
import io.openems.backend.common.metadata.Metadata;
import io.openems.backend.common.metadata.User;
import io.openems.backend.common.timedata.TimedataManager;
import io.openems.backend.common.uiwebsocket.UiWebsocket;
import io.openems.backend.metrics.prometheus.DebugExecutor;
import io.openems.backend.oauthregistry.OAuthRegistry;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.notification.AllSyncableEntriesNotification;
import io.openems.common.jsonrpc.notification.ScheduleNotification;
import io.openems.common.jsonrpc.notification.SystemLogNotification;
import io.openems.common.jsonrpc.request.AuthenticatedRpcRequest;
import io.openems.common.jsonrpc.request.EdgeRpcRequest;
import io.openems.common.jsonrpc.request.SubscribeScheduleRequest;
import io.openems.common.jsonrpc.request.SubscribeSystemLogRequest;
import io.openems.common.jsonrpc.syncable.SyncableEntry;
import io.openems.common.logger.DebugLoggable;
import io.openems.common.session.Role;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.StringUtils;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = "Edge.Manager", //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		immediate = true //
)
@EventTopics({ //
		Metadata.Events.AFTER_IS_INITIALIZED //
})
public class EdgeManagerImpl extends AbstractOpenemsBackendComponent
		implements EdgeManager, EventHandler, DebugLoggable {

	private static final String COMPONENT_ID = "edgewebsocket0";
	private static final String SYNCABLE_REPOSITORY_EXECUTOR_ID = "Edge.Manager.SyncableRepository";

	protected final SystemLogHandler systemLogHandler;

	protected final ScheduleHandler scheduleHandler;

	private final Logger log = LoggerFactory.getLogger(EdgeManagerImpl.class);
	private final DebugExecutor syncableRepositoryExecutor = new DebugExecutor(SYNCABLE_REPOSITORY_EXECUTOR_ID,
			Executors.newSingleThreadExecutor(new NamedThreadFactory(SYNCABLE_REPOSITORY_EXECUTOR_ID, true)));

	@Reference
	protected volatile Metadata metadata;

	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
	protected volatile AppCenterMetadata.EdgeData appCenterMetadata;

	@Reference
	protected OAuthRegistry oAuthRegistry;

	@Reference
	protected volatile TimedataManager timedataManager;

	@Reference
	protected volatile EventAdmin eventAdmin;

	@Reference(cardinality = ReferenceCardinality.OPTIONAL)
	protected volatile UiWebsocket uiWebsocket;

	@Reference
	protected SyncableRepository syncableRepository;

	@Reference
	protected SubscriptionManager subscriptionManager;

	private final Consumer<SyncableRepository.ChangeEvent> syncableRepositoryChangeHandler = this::handleSyncableRepositoryChange;

	private WebsocketServer server = null;
	private Config config;
	private ImmutableList<SyncableEntry<?>> previousSentEntries;

	public EdgeManagerImpl() {
		super("Edge.Manager");
		this.systemLogHandler = new SystemLogHandler(//
				() -> this.uiWebsocket, //
				this::send);
		this.scheduleHandler = new ScheduleHandler(//
				() -> this.uiWebsocket, //
				this::send);
	}

	@Activate
	private void activate(Config config) {
		this.config = config;

		if (this.metadata.isInitialized()) {
			this.startServer();
		}

		this.syncableRepository.getChangeEvent().subscribe(this.syncableRepositoryChangeHandler);
	}

	@Deactivate
	private void deactivate() {
		this.syncableRepository.getChangeEvent().unsubscribe(this.syncableRepositoryChangeHandler);
		this.stopServer();
	}

	/**
	 * Create and start new server.
	 */
	private synchronized void startServer() {
		if (this.server == null) {
			this.server = new WebsocketServer(this, this.getName(), this.config.port(), this.config.poolSize());
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
	public CompletableFuture<JsonrpcResponseSuccess> send(String edgeId, User user, Role role, JsonrpcRequest request) {
		final var wsData = this.getWebSocketForEdgeId(edgeId);
		if (wsData == null) {
			return CompletableFuture.failedFuture(OpenemsError.BACKEND_EDGE_NOT_CONNECTED.exception(edgeId));
		}

		// Wrap Request in AuthenticatedRpc
		var authenticatedRpc = new AuthenticatedRpcRequest<>(user, role, request);
		var edgeRpc = new EdgeRpcRequest(edgeId, authenticatedRpc);
		var responseFuture = wsData.send(edgeRpc);

		// Unwrap Response
		var result = new CompletableFuture<JsonrpcResponseSuccess>();
		responseFuture.whenComplete((r, ex) -> {
			if (ex != null) {
				result.completeExceptionally(ex);
			} else if (r != null) {
				try {
					var edgeRpcResponse = getAsJsonObject(r.getResult(), "payload");
					var authenticatedRpcResponse = getAsJsonObject(//
							getAsJsonObject(//
									getAsJsonObject(edgeRpcResponse, "result"), //
									"payload"),
							"result");
					result.complete(new GenericJsonrpcResponseSuccess(request.id, authenticatedRpcResponse));
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
	public boolean send(String edgeId, JsonrpcNotification notification) {
		var wsData = this.getWebSocketForEdgeId(edgeId);
		if (wsData == null) {
			return false;
		}
		return wsData.send(notification);
	}

	/**
	 * Gets the {@link WsData} of the WebSocket connection for an Edge-ID. If more
	 * than one connection exists, the first one is returned. Returns null if none
	 * is found.
	 *
	 * @param edgeId the Edge-ID
	 * @return the {@link WsData}
	 */
	private final WsData getWebSocketForEdgeId(String edgeId) {
		var server = this.server;
		if (server == null) {
			return null;
		}
		return server.getConnections().stream() //
				.map(ws -> (WsData) ws.getAttachment()) //
				.filter(Objects::nonNull) //
				.filter(wsData -> wsData.containsEdgeId(edgeId)) //
				.findFirst().orElse(null);
	}

	@Override
	public final EdgeCache getEdgeCacheForEdgeId(String edgeId) {
		var edgeClient = this.getEdgeClientForEdgeId(edgeId);
		return (edgeClient == null) ? null : edgeClient.getCache();
	}

	@Override
	public final BackendEdgeClient getEdgeClientForEdgeId(String edgeId) {
		var server = this.server;
		if (server == null) {
			return null;
		}
		return server.getConnections().stream() //
				.map(ws -> (WsData) ws.getAttachment()) //
				.map(wsData -> wsData.getEdgeClient(edgeId)) //
				.filter(Objects::nonNull) //
				.findFirst().orElse(null);
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
			Role role, UUID websocketId, SubscribeSystemLogRequest request) {
		return this.systemLogHandler.handleSubscribeSystemLogRequest(edgeId, user, role, websocketId, request);
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
	public CompletableFuture<JsonrpcResponseSuccess> handleSubscribeScheduleRequest(String edgeId, User user, Role role,
			UUID websocketId, SubscribeScheduleRequest request) {
		return this.scheduleHandler.handleSubscribeScheduleRequest(edgeId, user, role, websocketId, request);
	}

	/**
	 * Handles a {@link ScheduleNotification}, i.e. the replies to
	 * {@link SubscribeScheduleRequest}.
	 *
	 * @param edgeId       the Edge-ID
	 * @param notification the ScheduleNotification
	 */
	public void handleScheduleNotification(String edgeId, ScheduleNotification notification) {
		this.scheduleHandler.handleScheduleNotification(edgeId, notification);
	}

	private void handleSyncableRepositoryChange(SyncableRepository.ChangeEvent event) {
		this.syncableRepositoryExecutor.submit("sendUpdates", () -> {
			var server = this.server;
			if (server == null) {
				return;
			}

			try {
				var allEntries = this.syncableRepository.getAllStaticEntries();
				var changes = SyncableEntry.compareAndGetChanges(this.previousSentEntries, allEntries);
				this.previousSentEntries = allEntries;
				if (changes.isEmpty()) {
					return;
				}

				var changesAsStr = changes.stream() //
						.map(c -> StringUtils.toShortString(c.newEntry().toString(), 300)) //
						.collect(joining(", "));
				getComponentLogger(this).info("Send syncable repository update: {}", changesAsStr);

				for (var connection : server.getConnections()) {
					if (connection.getAttachment() == null || !(connection.getAttachment() instanceof WsData wsData)) {
						continue;
					}

					wsData.send(new AllSyncableEntriesNotification(allEntries));
				}
			} catch (Exception ex) {
				getComponentLogger(this).error("Error while sending syncable repository update to all edge managers.",
						ex);
			}
		});
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
		var result = channelAddresses.stream() //
				.collect(toMap(//
						Function.identity(), //
						c -> JsonNull.INSTANCE, //
						(t, u) -> u, TreeMap<ChannelAddress, JsonElement>::new));
		var edgeCache = this.getEdgeCacheForEdgeId(edgeId);
		if (edgeCache == null) {
			return result;
		}
		for (var channelAddress : channelAddresses) {
			result.put(channelAddress, edgeCache.getChannelValue(channelAddress.toString()));
		}
		return result;
	}

	@Override
	public String debugLog() {
		var server = this.server;
		var b = new StringBuilder() //
				.append("[").append(this.getName()).append("] ");

		if (server == null) {
			b.append("NOT STARTED"); //
		} else {
			b.append(server.debugLog());
			var remoteConnections = server.getConnections().stream() //
					.map(ws -> (WsData) ws.getAttachment()) //
					.map(wsData -> wsData.getId() + ":" + wsData.getNumberOfEdges()) //
					.collect(joining(","));
			if (!remoteConnections.isEmpty()) {
				b.append(", RemoteConnections: ").append(remoteConnections); //
			}
		}
		return b.toString();
	}

	@Override
	public Map<String, JsonElement> debugMetrics() {
		if (this.server == null) {
			return null;
		}

		return this.server.debugMetrics().entrySet().stream() //
				.collect(toUnmodifiableMap(//
						e -> COMPONENT_ID + "/" + e.getKey(), //
						e -> new JsonPrimitive(e.getValue())));
	}

}
