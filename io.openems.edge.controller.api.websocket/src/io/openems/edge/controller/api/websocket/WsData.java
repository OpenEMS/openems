package io.openems.edge.controller.api.websocket;

import java.util.Optional;
import java.util.UUID;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.access_control.RoleId;
import io.openems.edge.common.user.EdgeUser;

public class WsData extends io.openems.common.websocket.WsData {

	private final SubscribedChannelsWorker subscribedChannelsWorker;

	/**
	 * The token that is stored in the Browser Cookie. Be aware that this can be
	 * 'null' for a short period of time on open of the websocket.
	 */
	private UUID sessionToken = null;

	private Optional<EdgeUser> user = Optional.empty();

	private RoleId roleId;

	public WsData(WebsocketApi parent) {
		this.subscribedChannelsWorker = new SubscribedChannelsWorker(parent, this);
	}

	public void setSessionToken(UUID sessionToken) {
		this.sessionToken = sessionToken;
	}

	public UUID getSessionToken() {
		return sessionToken;
	}

	public void setRoleId(RoleId roleId) {
		this.roleId = roleId;
	}

	public RoleId getRoleId() {
		return roleId;
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

}
