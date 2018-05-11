package io.openems.edge.controller.api.websocket;

import java.util.Optional;
import java.util.UUID;

import org.java_websocket.WebSocket;

import io.openems.common.session.Role;
import io.openems.edge.api.user.User;
import io.openems.edge.controller.api.apicontrollerutils.ApiController;
import io.openems.edge.controller.api.apicontrollerutils.EdgeWebsocketHandler;

public class UiEdgeWebsocketHandler extends EdgeWebsocketHandler {

	private final String sessionToken;
	private final UUID uuid;

	private Optional<User> userOpt = Optional.empty();

	public UiEdgeWebsocketHandler(ApiController parent, WebSocket websocket, String sessionToken, UUID uuid) {
		super(parent, websocket);
		this.sessionToken = sessionToken;
		this.uuid = uuid;
	}

	public String getSessionToken() {
		return sessionToken;
	}

	public UUID getUuid() {
		return uuid;
	}

	/**
	 * Sets the User and the Role
	 * 
	 * @param user
	 */
	public void setUser(User user) {
		this.userOpt = Optional.ofNullable(user);
		if (user != null) {
			super.setRole(user.getRole());
		} else {
			super.unsetRole();
		}
	}

	public Optional<User> getUserOpt() {
		return userOpt;
	}

	/**
	 * User setUser instead
	 */
	@Override
	@Deprecated
	public synchronized void setRole(Optional<Role> roleOpt) {
		super.setRole(roleOpt);
	}

	/**
	 * User setUser instead
	 */
	@Override
	@Deprecated
	public synchronized void setRole(Role role) {
		super.setRole(role);
	}
}
