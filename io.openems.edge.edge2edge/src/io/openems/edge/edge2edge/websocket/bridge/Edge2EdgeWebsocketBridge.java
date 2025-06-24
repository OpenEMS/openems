package io.openems.edge.edge2edge.websocket.bridge;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.google.gson.JsonElement;

import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.EdgeConfig;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface Edge2EdgeWebsocketBridge extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		CONNECTION_STATE(Doc.of(ConnectionStateOption.values()) //
				.persistencePriority(PersistencePriority.HIGH)), //
		NO_CONNECTION(Doc.of(Level.WARNING)), //
		NOT_AUTHENTICATED(Doc.of(Level.INFO)), //

		/**
		 * Copies the _sum/State of the remote edge.
		 */
		REMOTE_SUM_STATE(Doc.of(Level.values()) //
				.persistencePriority(PersistencePriority.HIGH)), //
		REMOTE_SUM_STATE_FAULT(Doc.of(Level.FAULT)), //
		REMOTE_SUM_STATE_WARNING(Doc.of(Level.WARNING)), //
		REMOTE_SUM_STATE_INFO(Doc.of(Level.INFO)), //
		REMOTE_SUM_STATE_OK(Doc.of(Level.OK)), //
		;

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
	 * Gets the Channel for {@link ChannelId#CONNECTION_STATE}.
	 *
	 * @return the Channel
	 */
	public default EnumReadChannel getConnectionStateChannel() {
		return this.channel(ChannelId.CONNECTION_STATE);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#CONNECTION_STATE}
	 * Channel.
	 *
	 * @param value the next write value
	 */
	public default void _setConnectionState(ConnectionStateOption value) {
		this.getConnectionStateChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#REMOTE_SUM_STATE}.
	 *
	 * @return the Channel
	 */
	public default EnumReadChannel getRemoteSumStateChannel() {
		return this.channel(ChannelId.REMOTE_SUM_STATE);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#REMOTE_SUM_STATE}
	 * Channel.
	 *
	 * @param value the next write value
	 */
	public default void _setRemoteSumState(Level value) {
		this.getRemoteSumStateChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#REMOTE_SUM_STATE_FAULT}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getRemoteSumStateFaultChannel() {
		return this.channel(ChannelId.REMOTE_SUM_STATE_FAULT);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#REMOTE_SUM_STATE_FAULT} Channel.
	 *
	 * @param value the next write value
	 */
	public default void _setRemoteSumStateFault(Boolean value) {
		this.getRemoteSumStateFaultChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#REMOTE_SUM_STATE_WARNING}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getRemoteSumStateWarningChannel() {
		return this.channel(ChannelId.REMOTE_SUM_STATE_WARNING);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#REMOTE_SUM_STATE_WARNING} Channel.
	 *
	 * @param value the next write value
	 */
	public default void _setRemoteSumStateWarning(Boolean value) {
		this.getRemoteSumStateWarningChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#REMOTE_SUM_STATE_INFO}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getRemoteSumStateInfoChannel() {
		return this.channel(ChannelId.REMOTE_SUM_STATE_INFO);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#REMOTE_SUM_STATE_INFO} Channel.
	 *
	 * @param value the next write value
	 */
	public default void _setRemoteSumStateInfo(Boolean value) {
		this.getRemoteSumStateInfoChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#REMOTE_SUM_STATE_OK}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getRemoteSumStateOkChannel() {
		return this.channel(ChannelId.REMOTE_SUM_STATE_OK);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#REMOTE_SUM_STATE_OK} Channel.
	 *
	 * @param value the next write value
	 */
	public default void _setRemoteSumStateOk(Boolean value) {
		this.getRemoteSumStateOkChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#NO_CONNECTION}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getNoConnectionChannel() {
		return this.channel(ChannelId.NO_CONNECTION);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#NO_CONNECTION}
	 * Channel.
	 *
	 * @param value the next write value
	 */
	public default void _setNoConnection(Boolean value) {
		this.getNoConnectionChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#NOT_AUTHENTICATED}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getNotAuthenticatedChannel() {
		return this.channel(ChannelId.NOT_AUTHENTICATED);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#NOT_AUTHENTICATED}
	 * Channel.
	 *
	 * @param value the next write value
	 */
	public default void _setNotAuthenticated(Boolean value) {
		this.getNotAuthenticatedChannel().setNextValue(value);
	}

	/**
	 * Sets a channel value asynchronously.
	 * 
	 * @param componentId the id of the component
	 * @param channelId   the id of the channel
	 * @param value       the value to set the channel to
	 */
	public void setChannelValue(String componentId, String channelId, JsonElement value);

	/**
	 * Adds a channel subscriber.
	 *
	 * @param channelSubscriber the channel subscriber to add
	 * @param onData            the on data callback to call when data was received
	 * @return a {@link Disposable} to unregister the subscriber
	 */
	public Disposable addChannelSubscriber(//
			final ChannelSubscriber channelSubscriber, //
			final Consumer<Map<ChannelAddress, JsonElement>> onData //
	);

	/**
	 * Adds a {@link ConnectionState} listener.
	 *
	 * @param listener the listener to add
	 * @return the added listener
	 */
	public BiConsumer<ConnectionState, ConnectionState> addOnConnectionStateChangeListener(
			BiConsumer<ConnectionState, ConnectionState> listener);

	/**
	 * Removes a {@link ConnectionState} listener.
	 *
	 * @param listener the listener to remove
	 * @return true if the listener got removed; else false
	 */
	public boolean removeOnConnectionStateChangeListener(BiConsumer<ConnectionState, ConnectionState> listener);

	/**
	 * Adds a {@link EdgeConfig} listener.
	 *
	 * @param listener the listener to add
	 * @return the added listener
	 */
	public Consumer<EdgeConfig> addOnEdgeConfigChangeListener(Consumer<EdgeConfig> listener);

	/**
	 * Removes a {@link EdgeConfig} listener.
	 *
	 * @param listener the listener to remove
	 * @return true if the listener got removed; else false
	 */
	public boolean removeOnEdgeConfigChangeListener(Consumer<EdgeConfig> listener);

	/**
	 * Adds a channel change listener.
	 *
	 * @param listener the listener to add
	 * @return the added listener
	 */
	public Runnable addOnChannelChangeListener(Runnable listener);

	/**
	 * Removes a channel change listener.
	 *
	 * @param listener the listener to remove
	 * @return true if the listener got removed; else false
	 */
	public boolean removeOnChannelChangeListener(Runnable listener);

	/**
	 * Sends a request to the remove edge.
	 *
	 * @param request the request to send
	 * @return the result future
	 */
	public CompletableFuture<JsonrpcResponseSuccess> sendRequest(JsonrpcRequest request);

	/**
	 * Gets the current connection state.
	 *
	 * @return the {@link ConnectionState}
	 */
	public ConnectionState getConnectionState();

}
