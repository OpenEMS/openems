package io.openems.backend.uiwebsocket.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import io.openems.backend.common.edgewebsocket.EdgeCache;
import io.openems.backend.common.metadata.Metadata;
import io.openems.backend.common.metadata.User;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.notification.CurrentDataNotification;
import io.openems.common.jsonrpc.notification.EdgeRpcNotification;
import io.openems.common.jsonrpc.request.SubscribeChannelsRequest;

public class WsData extends io.openems.common.websocket.WsData {

	private static class SubscribedChannels {

		private int lastRequestCount = Integer.MIN_VALUE;
		private final Map<String, SortedSet<String>> subscribedChannels = new HashMap<>();

		/**
		 * Applies a SubscribeChannelsRequest.
		 *
		 * @param edgeId  the Edge-ID
		 * @param request the {@link SubscribeChannelsRequest}
		 */
		public synchronized void handleSubscribeChannelsRequest(String edgeId, SubscribeChannelsRequest request) {
			if (this.lastRequestCount < request.getCount()) {
				this.subscribedChannels.put(edgeId, request.getChannels());
			}
		}

		/**
		 * Gets the values for subscribed Channels.
		 * 
		 * @param edgeId    the Edge-ID
		 * @param edgeCache the {@link EdgeCache}
		 * @return a map of channel values
		 */
		public Map<String, JsonElement> getChannelValues(String edgeId, EdgeCache edgeCache) {
			var subscribedChannels = this.subscribedChannels.get(edgeId);
			if (subscribedChannels == null || subscribedChannels.isEmpty()) {
				return Collections.emptyMap();
			}

			var result = new HashMap<String, JsonElement>(subscribedChannels.size());
			for (var channel : subscribedChannels) {
				result.put(channel, edgeCache.getChannelValue(channel));
			}
			return result;
		}

		protected void dispose() {
			this.subscribedChannels.clear();
		}
	}

	private final Logger log = LoggerFactory.getLogger(WsData.class);

	private final WebsocketServer parent;
	private final SubscribedChannels subscribedChannels = new SubscribedChannels();
	private Optional<String> userId = Optional.empty();
	private Optional<String> token = Optional.empty();

	private Set<String> subscribedEdges = new HashSet<>();

	public WsData(WebsocketServer parent) {
		this.parent = parent;
	}

	/**
	 * Logout and invalidate Session.
	 */
	public void logout() {
		this.unsetToken();
		this.unsetUserId();
		this.subscribedChannels.dispose();
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

	/**
	 * Applies a SubscribeChannelsRequest.
	 *
	 * @param edgeId  the Edge-ID
	 * @param request the {@link SubscribeChannelsRequest}
	 */
	public synchronized void handleSubscribeChannelsRequest(String edgeId, SubscribeChannelsRequest request) {
		this.subscribedChannels.handleSubscribeChannelsRequest(edgeId, request);
	}

	/**
	 * Applies a SubscribeEdgesRequest.
	 * 
	 * @param edgeIds the edges to subscribe
	 */
	public void handleSubscribeEdgesRequest(Set<String> edgeIds) {
		this.subscribedEdges = edgeIds;
	}

	/**
	 * Sends the subscribed Channels to the UI session.
	 * 
	 * @param edgeId    the Edge-ID
	 * @param edgeCache the {@link EdgeCache} for the Edge-ID
	 */
	public void sendSubscribedChannels(String edgeId, EdgeCache edgeCache) {
		if (!this.isEdgeSubscribed(edgeId)) {
			return;
		}
		var values = this.subscribedChannels.getChannelValues(edgeId, edgeCache);
		if (values.isEmpty()) {
			return;
		}
		try {
			this.send(//
					new EdgeRpcNotification(edgeId, //
							new CurrentDataNotification(values)));

		} catch (OpenemsException e) {
			// Log & stop subscribes
			this.parent.logWarn(this.log, "Unable to send CurrentDataNotification: " + e.getMessage());
			this.subscribedChannels.dispose();
		}
	}

	/**
	 * Is the given Edge subscribed by this UI session?.
	 * 
	 * @param edgeId the Edge-ID
	 * @return true if subscribed
	 */
	public boolean isEdgeSubscribed(String edgeId) {
		return this.subscribedEdges.contains(edgeId);
	}

}
