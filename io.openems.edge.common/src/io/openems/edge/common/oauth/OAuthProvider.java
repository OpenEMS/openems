package io.openems.edge.common.oauth;

import java.util.concurrent.CompletableFuture;

import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.oauth.jsonrpc.InitiateOAuthConnect;

public interface OAuthProvider extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		OAUTH_CONNECTION_STATE(Doc.of(ConnectionState.values()) //
				.persistencePriority(PersistencePriority.HIGH)), //
		OAUTH_NOT_AUTHENTICATED(Doc.of(Level.INFO) //
				.translationKey(OAuthProvider.class, "OAuthProvider.OauthNotAuthenticated") //
				.onInit(channel -> {
					((OAuthProvider) channel.getComponent()).getOAuthConnectionStateChannel()
							.onUpdate(connectionStateValue -> {
								channel.setNextValue(connectionStateValue.asEnum() != ConnectionState.CONNECTED);
							});
				}));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ConnectionState} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setOAuthConnectionState(ConnectionState value) {
		this.getOAuthConnectionStateChannel().setNextValue(value);
	}

	/**
	 * Gets the HttpStatusCode value. See {@link ConnectionState}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<ConnectionState> getOAuthConnectionState() {
		return this.getOAuthConnectionStateChannel().value();
	}

	/**
	 * Gets the Channel for {@link ConnectionState}.
	 *
	 * @return the Channel
	 */
	public default Channel<ConnectionState> getOAuthConnectionStateChannel() {
		return this.channel(ChannelId.OAUTH_CONNECTION_STATE);
	}

	/**
	 * Gets oauth metainfo.
	 *
	 * @return the meta info
	 */
	public OAuthCore.OAuthMetaInfo getMetaInfo();

	/**
	 * Initiates a connection.
	 *
	 * @return the result
	 */
	public CompletableFuture<InitiateOAuthConnect.Response> initiateConnect();

	/**
	 * Connects a code.
	 *
	 * @param state the state
	 * @param code  the code
	 * @return a future when the task is done
	 */
	public CompletableFuture<Void> connectCode(String state, String code);

	/**
	 * Disconnects the current oauth connection.
	 */
	public void disconnect();

}