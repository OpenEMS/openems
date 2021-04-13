package io.openems.edge.controller.api.websocket;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.user.EdgeUser;

public class WsData extends io.openems.common.websocket.WsData {

	private final WebsocketApi parent;
	private final SubscribedChannelsWorker subscribedChannelsWorker;

	/**
	 * The token that is stored in the Browser Cookie. Be aware that this can be
	 * 'null' for a short period of time on open of the websocket.
	 */
	private UUID sessionToken = null;

	private Optional<EdgeUser> user = Optional.empty();

	public WsData(WebsocketApi parent) {
		this.parent = parent;
		this.subscribedChannelsWorker = new SubscribedChannelsWorker(parent, this);
	}

	public void setSessionToken(UUID sessionToken) {
		this.sessionToken = sessionToken;
	}

	public UUID getSessionToken() {
		return sessionToken;
	}

	public void setUser(EdgeUser user) {
		this.user = Optional.ofNullable(user);
	}

	public void unsetUser() {
		this.user = Optional.empty();
	}

	public Optional<EdgeUser> getUser() {
		return user;
	}

	/**
	 * Throws an exception if the User is not authenticated.
	 * 
	 * @param resource a resource identifier; used for the exception
	 * @return the current Role
	 * @throws OpenemsNamedException if the current Role privileges are less
	 */
	public EdgeUser assertUserIsAuthenticated(String resource) throws OpenemsNamedException {
		if (this.getUser().isPresent()) {
			return this.getUser().get();
		} else {
			throw OpenemsError.COMMON_USER_NOT_AUTHENTICATED
					.exception("Session [" + this.getSessionToken() + "]. Ignoring [" + resource + "]");
		}
	}

	/**
	 * Gets the SubscribedChannelsWorker to take care of subscribe to CurrentData.
	 * 
	 * @return the SubscribedChannelsWorker
	 */
	public SubscribedChannelsWorker getSubscribedChannelsWorker() {
		return subscribedChannelsWorker;
	}

	@Override
	public String toString() {
		String tokenString;
		if (this.sessionToken != null) {
			tokenString = this.sessionToken.toString();
		} else {
			tokenString = "UNKNOWN";
		}
		return "WebsocketApi.WsData [sessionToken=" + tokenString + ", user=" + user + "]";
	}

	@Override
	protected ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay,
			TimeUnit unit) {
		return this.parent.executor.scheduleWithFixedDelay(command, initialDelay, delay, unit);
	}

}
