package io.openems.backend.edge.manager;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.google.gson.JsonObject;

import io.openems.backend.common.metadata.User;
import io.openems.backend.common.uiwebsocket.UiWebsocket;
import io.openems.common.function.QuadFunction;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.notification.EdgeRpcNotification;
import io.openems.common.jsonrpc.notification.SystemLogNotification;
import io.openems.common.jsonrpc.request.SubscribeSystemLogRequest;
import io.openems.common.session.Language;
import io.openems.common.session.Role;

public class SystemLogHandler {

	/**
	 * Edge-ID to Session-Tokens.
	 */
	private final ConcurrentHashMap<String, Set<UUID>> subscriptions = new ConcurrentHashMap<>();

	private final Supplier<UiWebsocket> uiWebsocket;
	private final QuadFunction<String, User, Role, JsonrpcRequest, CompletableFuture<JsonrpcResponseSuccess>> send;

	public SystemLogHandler(//
			Supplier<UiWebsocket> uiWebsocket, //
			QuadFunction<String, User, Role, JsonrpcRequest, CompletableFuture<JsonrpcResponseSuccess>> send) {
		this.uiWebsocket = uiWebsocket;
		this.send = send;
	}

	/**
	 * Handles a {@link SubscribeSystemLogRequest}.
	 *
	 * @param edgeId      the Edge-ID
	 * @param user        the {@link User}
	 * @param role        the {@link Role}
	 * @param websocketId the id of the UI websocket connection
	 * @param request     the {@link SubscribeSystemLogRequest}
	 * @return a reply
	 */
	public CompletableFuture<JsonrpcResponseSuccess> handleSubscribeSystemLogRequest(String edgeId, User user,
			Role role, UUID websocketId, SubscribeSystemLogRequest request) {
		if (request.isSubscribe()) {
			// Add subscription
			this.addSubscriptionId(edgeId, websocketId);

			// Always forward subscribe to Edge
			return this.send.apply(edgeId, user, role, request);

		} else {
			// Remove subscription
			this.removeSubscriptionId(edgeId, websocketId);

			if (this.getSubscribedWebsocketIds(edgeId) != null) {
				// Remaining Tokens left for this Edge -> announce success
				return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));

			} else {
				// No remaining Tokens left for this Edge -> send unsubscribe
				return this.send.apply(edgeId, user, role, request);
			}
		}
	}

	/**
	 * Handles a {@link SystemLogNotification}, i.e. the replies to
	 * {@link SubscribeSystemLogRequest}.
	 *
	 * @param edgeId       the Edge-ID
	 * @param notification the {@link SystemLogNotification}
	 */
	public void handleSystemLogNotification(String edgeId, SystemLogNotification notification) {
		final var ids = this.getSubscribedWebsocketIds(edgeId);

		if (ids == null) {
			// No Tokens exist, but we still receive Notification? -> send unsubscribe
			var dummyGuestUser = new User("internal", "UnsubscribeSystemLogNotification", UUID.randomUUID().toString(),
					Language.EN, Role.GUEST, false, new JsonObject());
			this.send.apply(edgeId, dummyGuestUser, Role.GUEST, SubscribeSystemLogRequest.unsubscribe());
			return;
		}

		// Forward Notification to each Session token
		var uiWebsocket = this.uiWebsocket.get();
		if (uiWebsocket != null) {
			final var unsuccessfulIds = new HashSet<UUID>();
			for (var id : ids) {
				// TODO use events
				if (!uiWebsocket.send(id, new EdgeRpcNotification(edgeId, notification))) {
					unsuccessfulIds.add(id);
				}
			}
			this.removeSubscriptionIds(edgeId, unsuccessfulIds);
		}
	}

	/**
	 * Adds a subscription Token for the given Edge-ID.
	 * 
	 * @param edgeId      the Edge-ID
	 * @param websocketId the id of the UI websocket connection
	 */
	protected void addSubscriptionId(String edgeId, UUID websocketId) {
		this.subscriptions.compute(edgeId, (key, tokens) -> {
			if (tokens == null) {
				// Create new Set for this Edge-ID
				tokens = new HashSet<>();
			}
			tokens.add(websocketId);
			return tokens;
		});
	}

	/**
	 * Removes a subscription Token from the given Edge-ID.
	 * 
	 * @param edgeId      the Edge-ID
	 * @param websocketId the id of the UI websocket connection
	 */
	protected void removeSubscriptionId(String edgeId, UUID websocketId) {
		this.removeSubscriptionIds(edgeId, Set.of(websocketId));
	}

	protected void removeSubscriptionIds(String edgeId, Set<UUID> websocketIds) {
		this.subscriptions.compute(edgeId, (key, tokens) -> {
			if (tokens == null) {
				// There was no entry for this Edge-ID
				return null;
			}
			tokens.removeAll(websocketIds);
			if (tokens.isEmpty()) {
				return null;
			}
			return tokens;
		});
	}

	/**
	 * Gets all subscription Tokens for the given Edge-ID.
	 * 
	 * @param edgeId the Edge-ID
	 * @return a Set of Tokens; or null
	 */
	protected Set<UUID> getSubscribedWebsocketIds(String edgeId) {
		return this.subscriptions.get(edgeId);
	}
}
