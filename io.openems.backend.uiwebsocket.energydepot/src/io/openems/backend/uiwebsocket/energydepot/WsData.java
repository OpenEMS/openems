package io.openems.backend.uiwebsocket.energydepot;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import io.openems.backend.metadata.api.BackendUser;
import io.openems.backend.metadata.api.Metadata;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

public class WsData extends io.openems.common.websocket.WsData {

	private final UiWebsocketKaco parent;
	private final Map<String, SubscribedChannelsWorker> subscribedChannelsWorkers = new HashMap<>();
	private Optional<String> userId = Optional.empty();
	private Optional<UUID> token = Optional.empty();

	public WsData(UiWebsocketKaco parent) {
		this.parent = parent;
	}

	@Override
	public synchronized void dispose() {
		for (SubscribedChannelsWorker subscribedChannelsWorker : this.subscribedChannelsWorkers.values()) {
			subscribedChannelsWorker.dispose();
		}
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
		return this.userId;
	}

	/**
	 * Gets the authenticated User.
	 * 
	 * @param metadata the Metadata service
	 * @return the User or Optional.Empty if the User was not authenticated or it is
	 *         not available from Metadata service
	 */
	public synchronized Optional<BackendUser> getUser(Metadata metadata) {
		Optional<String> userId = this.getUserId();
		if (userId.isPresent()) {
			Optional<BackendUser> user = metadata.getUser(userId.get());
			return user;
		}
		return Optional.empty();
	}

	public void setToken(UUID token) {
		this.token = Optional.ofNullable(token);
	}

	public Optional<UUID> getToken() {
		return this.token;
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
	 * @param edgeId the Edge-ID
	 * @return the SubscribedChannelsWorker
	 */
	public synchronized SubscribedChannelsWorker getSubscribedChannelsWorker(String edgeId) {
		SubscribedChannelsWorker result = this.subscribedChannelsWorkers.get(edgeId);
		if (result == null) {
			result = new SubscribedChannelsWorker(this.parent, edgeId, this);
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
}
