package io.openems.backend.edgewebsocket.impl;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import io.openems.backend.metadata.api.Edge;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.DeprecatedJsonrpcNotification;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.notification.EdgeRpcNotification;
import io.openems.common.jsonrpc.notification.SystemLogNotification;
import io.openems.common.jsonrpc.request.SubscribeSystemLogRequest;
import io.openems.common.types.SemanticVersion;
import io.openems.common.utils.JsonUtils;

public class SystemLogHandler {

	private final Logger log = LoggerFactory.getLogger(SystemLogHandler.class);
	private final EdgeWebsocketImpl parent;
	private final Multimap<String, UUID> subscriptions = HashMultimap.create();

	public SystemLogHandler(EdgeWebsocketImpl parent) {
		this.parent = parent;
	}

	/**
	 * Handles a {@link SubscribeSystemLogRequest}.
	 * 
	 * @param edgeId  the Edge-ID
	 * @param token   the UI session token
	 * @param request the {@link SubscribeSystemLogRequest}
	 * @return a reply
	 * @throws OpenemsNamedException on error
	 */
	public CompletableFuture<JsonrpcResponseSuccess> handleSubscribeSystemLogRequest(String edgeId,
																					 UUID token,
																					 SubscribeSystemLogRequest request) throws OpenemsNamedException {
		if (request.getSubscribe()) {
			/*
			 * Start subscription
			 */
			boolean wasSubscriptionForThisEdgeExisting;
			synchronized (this.subscriptions) {
				// search existing subscription
				wasSubscriptionForThisEdgeExisting = this.subscriptions.containsKey(edgeId);

				// add subscription to list
				this.subscriptions.put(edgeId, token);
			}

			if (wasSubscriptionForThisEdgeExisting) {
				// announce success
				return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
			} else {
				// send subscribe to Edge
				return this.sendSubscribe(edgeId, request, true);
				// return this.parent.send(edgeId, request);
			}

		} else {
			/*
			 * End subscription
			 */
			boolean isAnySubscriptionForThisEdgeLeft;
			synchronized (this.subscriptions) {
				this.subscriptions.remove(edgeId, token);

				isAnySubscriptionForThisEdgeLeft = this.subscriptions.containsKey(edgeId);
			}

			if (isAnySubscriptionForThisEdgeLeft) {
				// announce success
				return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));

			} else {
				// send unsubscribe to Edge
				return this.sendSubscribe(edgeId, request, false);
				// return this.parent.send(edgeId, request);
			}
		}
	}

	/**
	 * Handles a {@link SystemLogNotification}, i.e. the replies to
	 * {@link SubscribeSystemLogRequest}.
	 *  @param edgeId       the Edge-ID
	 * @param notification the SystemLogNotification
	 */
	public void handleSystemLogNotification(String edgeId, SystemLogNotification notification) {
		Collection<UUID> tokens;
		synchronized (this.subscriptions) {
			tokens = this.subscriptions.get(edgeId);
		}

		for (UUID token : tokens) {
			try {
				this.parent.uiWebsocket.send(token, new EdgeRpcNotification(edgeId, notification));
			} catch (OpenemsNamedException e) {
				this.log.warn("Unable to handle SystemLogNotification from [" + edgeId + "]: " + e.getMessage());
				this.unsubscribe(edgeId, token);
			}
		}
	}

	/**
	 * Unsubscribe from System-Log.
	 *  @param edgeId the Edge-ID#
	 * @param token  the UI token
	 */
	private void unsubscribe(String edgeId, UUID token) {
		boolean isAnySubscriptionForThisEdgeLeft;
		synchronized (this.subscriptions) {
			this.subscriptions.remove(edgeId, token);

			isAnySubscriptionForThisEdgeLeft = this.subscriptions.containsKey(edgeId);
		}

		if (isAnySubscriptionForThisEdgeLeft) {
			return;

		} else {
			// send unsubscribe to Edge
			try {
				this.parent.send(edgeId, SubscribeSystemLogRequest.unsubscribe());
			} catch (OpenemsNamedException e) {
				this.log.error("Unable to Unsubscribe from Edge [" + edgeId + "]");
				e.printStackTrace();
			}
		}
	}

	@Deprecated
	private CompletableFuture<JsonrpcResponseSuccess> sendSubscribe(String edgeId,
																	SubscribeSystemLogRequest request, boolean subscribe) throws OpenemsNamedException {
		// handling deprecated: remove after full migration
		Optional<Edge> edge = this.parent.metadata.getEdge(edgeId);
		if (edge.isPresent()) {
			if (!edge.get().getVersion().isAtLeast(new SemanticVersion(2018, 11, 0))) {
				this.parent.send(edgeId, new DeprecatedJsonrpcNotification(JsonUtils.buildJsonObject() //
						.add("messageId", JsonUtils.buildJsonObject() //
								.addProperty("ui", request.getId().toString()) //
								.addProperty("backend", request.getId().toString()).build()) //
						.add("log", JsonUtils.buildJsonObject() //
								.addProperty("mode", subscribe ? "subscribe" : "unsubscribe") //
								.build()) //
						.build()));
				return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
			}
		}
		// current version
		return this.parent.send(edgeId, request);
	}
}
