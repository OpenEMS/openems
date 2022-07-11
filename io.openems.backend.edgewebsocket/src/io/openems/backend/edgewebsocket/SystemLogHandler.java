package io.openems.backend.edgewebsocket;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import io.openems.backend.common.metadata.User;
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

	/**
	 * Edge-ID to Session-Token.
	 */
	private final Multimap<String, String> subscriptions = HashMultimap.create();

	public SystemLogHandler(EdgeWebsocketImpl parent) {
		this.parent = parent;
	}

	/**
	 * Handles a {@link SubscribeSystemLogRequest}.
	 *
	 * @param edgeId  the Edge-ID
	 * @param user    the {@link User}
	 * @param token   the UI session token
	 * @param request the {@link SubscribeSystemLogRequest}
	 * @return a reply
	 * @throws OpenemsNamedException on error
	 */
	public CompletableFuture<JsonrpcResponseSuccess> handleSubscribeSystemLogRequest(String edgeId, User user,
			String token, SubscribeSystemLogRequest request) throws OpenemsNamedException {
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
				return this.sendSubscribe(edgeId, user, request, true);
				// return this.parent.send(edgeId, request);
			}

		}
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
			return this.sendSubscribe(edgeId, user, request, false);
			// return this.parent.send(edgeId, request);
		}
	}

	/**
	 * Handles a {@link SystemLogNotification}, i.e. the replies to
	 * {@link SubscribeSystemLogRequest}.
	 *
	 * @param edgeId       the Edge-ID
	 * @param user         the {@link User}
	 * @param notification the {@link SystemLogNotification}
	 */
	public void handleSystemLogNotification(String edgeId, User user, SystemLogNotification notification) {
		Collection<String> tokens;
		synchronized (this.subscriptions) {
			tokens = this.subscriptions.get(edgeId);
		}

		for (String token : tokens) {
			try {
				this.parent.uiWebsocket.send(token, new EdgeRpcNotification(edgeId, notification));
			} catch (OpenemsNamedException | NullPointerException e) {
				this.log.warn("Unable to handle SystemLogNotification from [" + edgeId + "]: " + e.getMessage());
				this.unsubscribe(edgeId, user, token);
			}
		}
	}

	/**
	 * Unsubscribe from System-Log.
	 *
	 * @param edgeId the Edge-ID#
	 * @param user   the {@link User}; possibly null
	 * @param token  the UI token
	 */
	private void unsubscribe(String edgeId, User user, String token) {
		boolean isAnySubscriptionForThisEdgeLeft;
		synchronized (this.subscriptions) {
			this.subscriptions.remove(edgeId, token);

			isAnySubscriptionForThisEdgeLeft = this.subscriptions.containsKey(edgeId);
		}

		if (isAnySubscriptionForThisEdgeLeft) {
			return;
		}

		// send unsubscribe to Edge
		try {
			this.parent.send(edgeId, user, SubscribeSystemLogRequest.unsubscribe());
		} catch (OpenemsNamedException e) {
			this.log.error("Unable to Unsubscribe from Edge [" + edgeId + "]");
			e.printStackTrace();
		}
	}

	@Deprecated
	private CompletableFuture<JsonrpcResponseSuccess> sendSubscribe(String edgeId, User user,
			SubscribeSystemLogRequest request, boolean subscribe) throws OpenemsNamedException {
		// handling deprecated: remove after full migration
		var edgeOpt = this.parent.metadata.getEdge(edgeId);
		if (edgeOpt.isPresent()) {
			if (!edgeOpt.get().getVersion().isAtLeast(new SemanticVersion(2018, 11, 0))) {
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
		return this.parent.send(edgeId, user, request);
	}
}
