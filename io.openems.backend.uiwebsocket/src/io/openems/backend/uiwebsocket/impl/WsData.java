package io.openems.backend.uiwebsocket.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import io.openems.backend.common.metadata.Metadata;
import io.openems.backend.common.metadata.User;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

public class WsData extends io.openems.common.websocket.WsData {

	private final WebsocketServer parent;
	private final Map<String, SubscribedChannelsWorker> subscribedChannelsWorkers = new HashMap<>();
	private Optional<String> userId = Optional.empty();
	private Optional<String> token = Optional.empty();

	public WsData(WebsocketServer parent) {
		this.parent = parent;
	}

	@Override
	public synchronized void dispose() {
		for (SubscribedChannelsWorker subscribedChannelsWorker : this.subscribedChannelsWorkers.values()) {
			subscribedChannelsWorker.dispose();
		}
	}

	/**
	 * Logout and invalidate Session.
	 */
	public void logout() {
		this.unsetToken();
		this.unsetUserId();
		this.dispose();
	}

	public synchronized void setUserId(String userId) {
		this.userId = Optional.ofNullable(userId);
	}

	/**
	 * Unsets the User-Token.
	 */
	public synchronized void unsetUserId() {
		this.userId = Optional.empty();
	}

	/**
	 * Gets the authenticated User-ID.
	 *
	 * @return the User-ID or Optional.Empty if the User was not authenticated.
	 */
	public synchronized Optional<String> getUserId() {
		return this.userId;
	}

	/**
	 * Gets the authenticated User.
	 *
	 * @param metadata the Metadata service
	 * @return the User or Optional.Empty if the User was not authenticated or it is
	 *         not available from Metadata service
	 */
	public synchronized Optional<User> getUser(Metadata metadata) {
		var userIdOpt = this.getUserId();
		if (userIdOpt.isPresent()) {
			return metadata.getUser(userIdOpt.get());
		}
		return Optional.empty();
	}

	public synchronized void setToken(String token) {
		this.token = Optional.ofNullable(token);
	}

	/**
	 * Gets the Login-Token.
	 *
	 * @return the Login-Token
	 */
	public Optional<String> getToken() {
		return this.token;
	}

	/**
	 * Unsets the Login-Token.
	 */
	public void unsetToken() {
		this.token = Optional.empty();
	}

	/**
	 * Gets the token or throws an error if no token was set.
	 *
	 * @return the token
	 * @throws OpenemsNamedException if no token has been set
	 */
	public String assertToken() throws OpenemsNamedException {
		var tokenOpt = this.token;
		if (tokenOpt.isPresent()) {
			return tokenOpt.get();
		}
		throw OpenemsError.BACKEND_UI_TOKEN_MISSING.exception();
	}

	/**
	 * Gets the SubscribedChannelsWorker to take care of subscribe to CurrentData.
	 *
	 * @param edgeId the Edge-ID
	 * @return the SubscribedChannelsWorker
	 */
	public synchronized SubscribedChannelsWorker getSubscribedChannelsWorker(String edgeId) {
		var result = this.subscribedChannelsWorkers.get(edgeId);
		if (result == null) {
			result = new SubscribedChannelsWorker(this.parent.parent, edgeId, this);
			this.subscribedChannelsWorkers.put(edgeId, result);
		}
		return result;
	}

	@Override
	public String toString() {
		String tokenString;
		if (this.token.isPresent()) {
			tokenString = this.token.get().toString();
		} else {
			tokenString = "UNKNOWN";
		}
		return "UiWebsocket.WsData [userId=" + this.userId.orElse("UNKNOWN") + ", token=" + tokenString + "]";
	}

	@Override
	protected ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay,
			TimeUnit unit) {
		return this.parent.scheduleWithFixedDelay(command, initialDelay, delay, unit);
	}

}
