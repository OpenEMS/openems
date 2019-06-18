package io.openems.backend.uiwebsocket.impl;

import java.util.Optional;
import java.util.UUID;

import io.openems.backend.metadata.api.Metadata;
import io.openems.backend.metadata.api.BackendUser;
import io.openems.common.access_control.RoleId;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.websocket.SubscribedChannelsWorker;

public class WsData extends io.openems.common.websocket.WsData {

	private final SubscribedChannelsWorker subscribedChannelsWorker;
	private Optional<String> userId = Optional.empty();
	private Optional<UUID> token = Optional.empty();
	private RoleId roleId;

	public WsData(UiWebsocketImpl parent) {
		this.subscribedChannelsWorker = new SubscribedChannelsWorkerMultipleEdges(parent, this);
	}

	public RoleId getRoleId() {
		return roleId;
	}

	public void setRoleId(RoleId roleId) {
		this.roleId = roleId;
	}

	@Override
	public void dispose() {
		this.subscribedChannelsWorker.dispose();
	}

	public synchronized void setUserId(String userId) {
		this.userId = Optional.ofNullable(userId);
	}

	/**
	 * Gets the authenticated User-ID.
	 * 
	 * @return the User-ID or Optional.Empty if the User was not authenticated.
	 */
	public synchronized Optional<String> getUserId() {
		return userId;
	}

	public void setToken(UUID token) {
		this.token = Optional.ofNullable(token);
	}

	public Optional<UUID> getToken() {
		return token;
	}

	/**
	 * Gets the token or throws an error if no token was set.
	 * 
	 * @return the token
	 * @throws OpenemsNamedException if no token has been set
	 */
	public UUID assertToken() throws OpenemsNamedException {
		Optional<UUID> token = this.token;
		if (token.isPresent()) {
			return token.get();
		}
		throw OpenemsError.BACKEND_UI_TOKEN_MISSING.exception();
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
		if (this.token.isPresent()) {
			tokenString = this.token.get().toString();
		} else {
			tokenString = "UNKNOWN";
		}
		return "UiWebsocket.WsData [userId=" + userId.orElse("UNKNOWN") + ", token=" + tokenString + "]";
	}
}
