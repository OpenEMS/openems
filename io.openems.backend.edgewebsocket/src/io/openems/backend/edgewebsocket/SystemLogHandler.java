package io.openems.backend.edgewebsocket;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.common.metadata.User;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.notification.EdgeRpcNotification;
import io.openems.common.jsonrpc.notification.SystemLogNotification;
import io.openems.common.jsonrpc.request.SubscribeSystemLogRequest;
import io.openems.common.session.Language;
import io.openems.common.session.Role;

public class SystemLogHandler {

	private final Logger log = LoggerFactory.getLogger(SystemLogHandler.class);
	private final EdgeWebsocketImpl parent;

	/**
	 * Edge-ID to Session-Token.
	 */
	private final ConcurrentHashMap<String, Set<String>> subscriptions = new ConcurrentHashMap<>();

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
		if (request.isSubscribe()) {
			// Add subscription
			this.addToken(edgeId, token);

			// Always forward subscribe to Edge
			return this.parent.send(edgeId, user, request);

		} else {
			// Remove subscription
			this.removeToken(edgeId, token);

			if (this.getTokens(edgeId) != null) {
				// Remaining Tokens left for this Edge -> announce success
				return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));

			} else {
				// No remaining Tokens left for this Edge -> send unsubscribe
				return this.parent.send(edgeId, user, request);
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
		var tokens = this.getTokens(edgeId);

		if (tokens == null) {
			// No Tokens exist, but we still receive Notification? -> send unsubscribe
			try {
				var dummyGuestUser = new User("internal", "UnsubscribeSystemLogNotification",
						UUID.randomUUID().toString(), Language.EN, Role.GUEST, new TreeMap<>());
				this.parent.send(edgeId, dummyGuestUser, SubscribeSystemLogRequest.unsubscribe());
				this.parent.logInfo(this.log, edgeId, "Was still sending SystemLogNotification. Sent unsubscribe.");

			} catch (OpenemsNamedException e) {
				this.parent.logWarn(this.log, edgeId,
						"Was still sending SystemLogNotification. Unable to send unsubscribe: " + e.getMessage());
			}
			return;
		}

		// Forward Notification to each Session token
		for (String token : tokens) {
			try {
				// TODO use events
				this.parent.uiWebsocket.send(token, new EdgeRpcNotification(edgeId, notification));

			} catch (OpenemsNamedException | NullPointerException e) {
				this.parent.logWarn(this.log, edgeId, "Unable to handle SystemLogNotification: " + e.getMessage());
				// error -> send unsubscribe
				try {
					var dummyGuestUser = new User("internal", "UnsubscribeSystemLogNotification",
							UUID.randomUUID().toString(), Language.EN, Role.GUEST, new TreeMap<>());
					this.handleSubscribeSystemLogRequest(edgeId, dummyGuestUser, token,
							SubscribeSystemLogRequest.unsubscribe());

				} catch (OpenemsNamedException e1) {
					this.parent.logWarn(this.log, edgeId, "Unable to send unsubscribe: " + e1.getMessage());
				}
			}
		}
	}

	/**
	 * Adds a subscription Token for the given Edge-ID.
	 * 
	 * @param edgeId the Edge-ID
	 * @param token  the Token
	 */
	protected void addToken(String edgeId, String token) {
		this.subscriptions.compute(edgeId, (key, tokens) -> {
			if (tokens == null) {
				// Create new Set for this Edge-ID
				tokens = new HashSet<>();
			}
			tokens.add(token);
			return tokens;
		});
	}

	/**
	 * Removes a subscription Token from the given Edge-ID.
	 * 
	 * @param edgeId the Edge-ID
	 * @param token  the Token
	 */
	protected void removeToken(String edgeId, String token) {
		this.subscriptions.compute(edgeId, (key, tokens) -> {
			if (tokens == null) {
				// There was no entry for this Edge-ID
				return null;
			}
			tokens.remove(token);
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
	protected Set<String> getTokens(String edgeId) {
		return this.subscriptions.get(edgeId);
	}
}
