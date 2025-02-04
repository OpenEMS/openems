package io.openems.edge.controller.api.websocket;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
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

	private final ControllerApiWebsocketImpl parent;
	private final SubscribedChannels subscribedChannels = new SubscribedChannels();

	/**
	 * The token that is stored in the Browser Cookie. Be aware that this can be
	 * 'null' for a short period of time on open of the websocket.
	 */
	private String sessionToken = null;

	private Optional<User> user = Optional.empty();

	public WsData(WebSocket ws, ControllerApiWebsocketImpl parent) {
		super(ws);
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

	@Override
	public String toLogString() {
		return new StringBuilder("WebsocketApi.WsData [sessionToken=") //
				.append(this.sessionToken != null //
						? this.sessionToken.toString() //
						: "UNKNOWN") //
				.append(", user=") //
				.append(this.user) //
				.append("]") //
				.toString();
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
			this.send(//
					new EdgeRpcNotification(ControllerApiWebsocket.EDGE_ID, //
							new CurrentDataNotification(values)));
		});
	}

}
