package io.openems.edge.controller.api.websocket;

import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.user.User;

public class WsData extends io.openems.common.websocket.WsData {

	private final WebsocketApi parent;
	private final SubscribedChannelsWorker subscribedChannelsWorker;

	/**
	 * The token that is stored in the Browser Cookie. Be aware that this can be
	 * 'null' for a short period of time on open of the websocket.
	 */
	private String sessionToken = null;

	private Optional<User> user = Optional.empty();

	public WsData(WebsocketApi parent) {
		this.parent = parent;
		this.subscribedChannelsWorker = new SubscribedChannelsWorker(parent, this);
	}

	/**
	 * Logout and invalidate Session.
	 */
	public void logout() {
		this.unsetUser();
		this.subscribedChannelsWorker.dispose();
	}

	/**
	 * Sets the Session Token.
	 *
	 * @param sessionToken the Session Token
	 */
	public void setSessionToken(String sessionToken) {
		this.sessionToken = sessionToken;
	}

	/**
	 * Gets the Session Token.
	 *
	 * @return the Session Token
	 */
	public String getSessionToken() {
		return this.sessionToken;
	}

	/**
	 * Sets the {@link User}.
	 *
	 * @param user the {@link User}
	 */
	public void setUser(User user) {
		this.user = Optional.ofNullable(user);
	}

	/**
	 * Unsets the {@link User}.
	 */
	public void unsetUser() {
		this.user = Optional.empty();
	}

	/**
	 * Gets the {@link User}.
	 *
	 * @return the {@link Optional} {@link User}
	 */
	public Optional<User> getUser() {
		return this.user;
	}

	/**
	 * Throws an exception if the User is not authenticated.
	 *
	 * @param resource a resource identifier; used for the exception
	 * @return the current {@link User}
	 * @throws OpenemsNamedException if the current Role privileges are less
	 */
	public User assertUserIsAuthenticated(String resource) throws OpenemsNamedException {
		if (this.getUser().isPresent()) {
			return this.getUser().get();
		}
		throw OpenemsError.COMMON_USER_NOT_AUTHENTICATED
				.exception("Session [" + this.getSessionToken() + "]. Ignoring [" + resource + "]");
	}

	/**
	 * Gets the {@link SubscribedChannelsWorker} to take care of subscribe to
	 * CurrentData.
	 *
	 * @return the {@link SubscribedChannelsWorker}
	 */
	public SubscribedChannelsWorker getSubscribedChannelsWorker() {
		return this.subscribedChannelsWorker;
	}

	@Override
	public String toString() {
		String tokenString;
		if (this.sessionToken != null) {
			tokenString = this.sessionToken.toString();
		} else {
			tokenString = "UNKNOWN";
		}
		return "WebsocketApi.WsData [sessionToken=" + tokenString + ", user=" + this.user + "]";
	}

	@Override
	protected ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay,
			TimeUnit unit) {
		return this.parent.executor.scheduleWithFixedDelay(command, initialDelay, delay, unit);
	}

}
