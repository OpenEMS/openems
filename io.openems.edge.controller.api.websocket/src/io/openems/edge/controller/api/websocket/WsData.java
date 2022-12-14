package io.openems.edge.controller.api.websocket;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.notification.CurrentDataNotification;
import io.openems.common.jsonrpc.notification.EdgeRpcNotification;
import io.openems.common.jsonrpc.request.SubscribeChannelsRequest;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.user.User;

public class WsData extends io.openems.common.websocket.WsData {

	private static class SubscribedChannels {

		private final Logger log = LoggerFactory.getLogger(SubscribedChannels.class);

		private int lastRequestCount = Integer.MIN_VALUE;
		private final SortedSet<ChannelAddress> subscribedChannels = new TreeSet<>();

		/**
		 * Applies a SubscribeChannelsRequest.
		 *
		 * @param request the SubscribeChannelsRequest
		 * @throws OpenemsNamedException on error
		 */
		public synchronized void handleSubscribeChannelsRequest(SubscribeChannelsRequest request)
				throws OpenemsNamedException {
			if (this.lastRequestCount < request.getCount()) {
				this.subscribedChannels.clear();
				for (var channel : request.getChannels()) {
					this.subscribedChannels.add(ChannelAddress.fromString(channel));
				}
			}
		}

		public Map<String, JsonElement> getChannelValues(ComponentManager componentManager) {
			var subscribedChannels = this.subscribedChannels;
			if (subscribedChannels == null || subscribedChannels.isEmpty()) {
				return Collections.emptyMap();
			}

			var result = new HashMap<String, JsonElement>(subscribedChannels.size());
			for (var channel : subscribedChannels) {
				JsonElement value;
				try {
					Channel<?> c = componentManager.getChannel(channel);
					value = c.value().asJson();
				} catch (IllegalArgumentException | OpenemsNamedException e) {
					this.log.warn("Unable to read value for Channel [" + channel + "]");
					value = JsonNull.INSTANCE;
				}
				result.put(channel.toString(), value);
			}
			return result;
		}

		protected void dispose() {
			this.subscribedChannels.clear();
		}
	}

	private final Logger log = LoggerFactory.getLogger(WsData.class);
	private final WebsocketApi parent;
	private final SubscribedChannels subscribedChannels = new SubscribedChannels();

	/**
	 * The token that is stored in the Browser Cookie. Be aware that this can be
	 * 'null' for a short period of time on open of the websocket.
	 */
	private String sessionToken = null;

	private Optional<User> user = Optional.empty();

	public WsData(WebsocketApi parent) {
		this.parent = parent;
	}

	/**
	 * Logout and invalidate Session.
	 */
	public void logout() {
		this.unsetUser();
		this.subscribedChannels.dispose();
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

	/**
	 * Applies a SubscribeChannelsRequest.
	 *
	 * @param request the {@link SubscribeChannelsRequest}
	 * @throws OpenemsNamedException on error
	 */
	public synchronized void handleSubscribeChannelsRequest(SubscribeChannelsRequest request)
			throws OpenemsNamedException {
		this.subscribedChannels.handleSubscribeChannelsRequest(request);
	}

	/**
	 * Sends the subscribed Channels to the UI session.
	 */
	public void sendSubscribedChannels() {
		var values = this.subscribedChannels.getChannelValues(this.parent.componentManager);
		if (values.isEmpty()) {
			return;
		}
		this.parent.server.execute(() -> {
			try {
				this.send(//
						new EdgeRpcNotification(WebsocketApi.EDGE_ID, //
								new CurrentDataNotification(values)));

			} catch (OpenemsException e) {
				this.parent.logWarn(this.log, "Unable to send CurrentDataNotification: " + e.getMessage());
			}
		});
	}

}
