package io.openems.edge.edge2edge.websocket.bridge;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
import io.openems.common.jsonrpc.request.ComponentJsonApiRequest;
import io.openems.common.jsonrpc.request.GetChannelsOfComponent;
import io.openems.common.jsonrpc.request.GetChannelsOfComponent.ChannelRecord;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;

public class BridgeComponentStateHandler {

	public enum State {
		NOT_CONNECTED, //
		CONNECTED, //
		COMPONENT_NOT_AVAILABLE, //
		;
	}

	private final Logger log = LoggerFactory.getLogger(BridgeComponentStateHandler.class);

	private State state = State.NOT_CONNECTED;
	private String componentId;
	private Edge2EdgeWebsocketBridge bridge;

	private final List<BiConsumer<State, State>> onStateChangeListener = new CopyOnWriteArrayList<>();
	private final List<BiConsumer<Edge2EdgeWebsocketBridge, List<ChannelRecord>>> subscribeListener = new CopyOnWriteArrayList<>();
	private final List<Consumer<Edge2EdgeWebsocketBridge>> unsubscribeListener = new CopyOnWriteArrayList<>();

	private final ExecutorService executor = Executors.newSingleThreadExecutor(Thread.ofVirtual().factory());

	/**
	 * Deactivates and closes all opened resources.
	 */
	public void deactivate() {
		this.executor.close();
	}

	/**
	 * Binds a bridge.
	 *
	 * @param bridge the bridge to bind
	 */
	public synchronized void bindBridge(Edge2EdgeWebsocketBridge bridge) {
		this.bridge = Objects.requireNonNull(bridge);

		this.bridge.addOnConnectionStateChangeListener(this::onConnectionStateChange);
		this.bridge.addOnEdgeConfigChangeListener(this::onEdgeConfigChange);
		this.bridge.addOnChannelChangeListener(this::onChannelChange);

		this.checkUpdateParams();
	}

	/**
	 * Unbinds the bridge.
	 *
	 * @param bridge the bridge to unbind
	 */
	public synchronized void unbindBridge(Edge2EdgeWebsocketBridge bridge) {
		Objects.requireNonNull(bridge);
		this.bridge = null;
		bridge.removeOnConnectionStateChangeListener(this::onConnectionStateChange);
		bridge.removeOnEdgeConfigChangeListener(this::onEdgeConfigChange);
		bridge.removeOnChannelChangeListener(this::onChannelChange);

		this.checkUpdateParams();
	}

	/**
	 * Updates the remote component id.
	 *
	 * @param componentId the new remote component id
	 */
	public synchronized void updateComponentId(String componentId) {
		this.componentId = componentId;

		this.checkUpdateParams();
	}

	/**
	 * Adds a listener when a subscribe should happen.
	 *
	 * @param onSubscribe the listener to execute
	 * @return the added listener
	 */
	public BiConsumer<Edge2EdgeWebsocketBridge, List<ChannelRecord>> addOnSubscribeListener(
			BiConsumer<Edge2EdgeWebsocketBridge, List<ChannelRecord>> onSubscribe) {
		this.subscribeListener.add(onSubscribe);
		return onSubscribe;
	}

	/**
	 * Removes a listener when a subscribe should happen.
	 * 
	 * @param onSubscribe the listener to remove
	 * @return true if the listener was removed
	 */
	public boolean removeOnSubscribeListener(BiConsumer<Edge2EdgeWebsocketBridge, List<ChannelRecord>> onSubscribe) {
		return this.subscribeListener.remove(onSubscribe);
	}

	/**
	 * Adds a listener when a unsubscribe should happen.
	 *
	 * @param onUnsubscribe the listener to execute
	 * @return the added listener
	 */
	public Consumer<Edge2EdgeWebsocketBridge> addOnUnsubscribeListener(
			Consumer<Edge2EdgeWebsocketBridge> onUnsubscribe) {
		this.unsubscribeListener.add(onUnsubscribe);
		return onUnsubscribe;
	}

	/**
	 * Removes a listener when a unsubscribe should happen.
	 * 
	 * @param onUnsubscribe the listener to remove
	 * @return true if the listener was removed
	 */
	public boolean removeOnUnsubscribeListener(Consumer<Edge2EdgeWebsocketBridge> onUnsubscribe) {
		return this.unsubscribeListener.remove(onUnsubscribe);
	}

	/**
	 * Adds a listener for a bridge state change.
	 *
	 * @param listener the listener to execute when the event happens
	 * @return the added listener
	 * @see State
	 */
	public BiConsumer<State, State> addOnStateChangeListener(BiConsumer<State, State> listener) {
		this.onStateChangeListener.add(listener);
		return listener;
	}

	/**
	 * Removes a listener for a bridge state change.
	 *
	 * @param listener the listener to remove
	 * @return true if the listener was removed
	 */
	public boolean removeOnStateChangeListener(BiConsumer<State, State> listener) {
		return this.onStateChangeListener.remove(listener);
	}

	/**
	 * Sets a channel value only if the component id is set the bridge is set and
	 * the bridge has an authenticated connection.
	 * 
	 * @param channelId the channel id
	 * @param value     the value to set
	 */
	public void setChannelValue(String channelId, JsonElement value) {
		final var bridge = this.bridge;
		final var componentId = this.componentId;
		if (componentId == null //
				|| bridge == null //
				|| bridge.getConnectionState() != ConnectionState.CONNECTED) {
			return;
		}

		this.bridge.setChannelValue(componentId, channelId, value);
	}

	private void onConnectionStateChange(ConnectionState prev, ConnectionState curr) {
		if (prev == ConnectionState.CONNECTED) {
			this.setState(State.NOT_CONNECTED);
			this.notifyUnsubscribeListener();
		}

		if (curr == ConnectionState.CONNECTED) {
			this.notifySubscribeListener();
		}
	}

	private void onEdgeConfigChange(EdgeConfig edgeConfig) {
		final var component = edgeConfig.getComponent(this.componentId);
		if (component.flatMap(t -> t.getProperty("enabled")) //
				.flatMap(JsonUtils::getAsOptionalBoolean) //
				.orElse(false)) {
			this.setState(State.CONNECTED);
			this.notifySubscribeListener();
		} else {
			this.setState(State.COMPONENT_NOT_AVAILABLE);
			this.notifyUnsubscribeListener();
		}
	}

	private void onChannelChange() {
		this.notifySubscribeListener(true);
	}

	private void checkUpdateParams() {
		if (this.componentId == null || this.bridge == null) {
			this.setState(State.NOT_CONNECTED);
			this.notifyUnsubscribeListener();
			return;
		}

		this.notifySubscribeListener();
	}

	private void notifySubscribeListener() {
		this.notifySubscribeListener(false);
	}

	private synchronized void notifySubscribeListener(boolean force) {
		final var componentId = this.componentId;
		final var bridge = this.bridge;
		if (componentId == null || bridge == null) {
			return;
		}

		if (bridge.getConnectionState() != ConnectionState.CONNECTED) {
			return;
		}

		final var witnessValue = this.compareAndExchangeState(State.NOT_CONNECTED, State.CONNECTED);
		if (witnessValue == State.COMPONENT_NOT_AVAILABLE) {
			return;
		}
		if (witnessValue != State.NOT_CONNECTED && !force) {
			return;
		}

		this.executor.execute(() -> {
			try {
				final var c = bridge
						.sendRequest(new ComponentJsonApiRequest("_componentManager",
								GenericJsonrpcRequest.createRequest(new GetChannelsOfComponent(),
										new GetChannelsOfComponent.Request(componentId, true))))
						.handle((response, error) -> {
							if (error != null) {
								this.log.warn("Unable to get channels", error);

								if (error instanceof OpenemsError.OpenemsNamedException openemsException
										&& openemsException.getError() == OpenemsError.EDGE_NO_COMPONENT_WITH_ID) {
									this.setState(State.COMPONENT_NOT_AVAILABLE);
								} else {
									this.setState(State.NOT_CONNECTED);
								}
								return Collections.<ChannelRecord>emptyList();
							}
							try {
								final var channels = GetChannelsOfComponent.Response.serializer().deserialize(
										response.getResult().get("payload").getAsJsonObject().get("result"));

								return channels.channels();
							} catch (Exception e) {
								this.log.warn("Unable to subscribe channels", e);
								return Collections.<ChannelRecord>emptyList();
							}
						}).get();

				for (var listener : this.subscribeListener) {
					listener.accept(bridge, c);
				}

			} catch (InterruptedException | ExecutionException e) {
				this.setState(State.NOT_CONNECTED);
				this.log.error("Unable to get channels for '{}'.", componentId, e);
			}
		});
	}

	private synchronized void notifyUnsubscribeListener() {
		this.executor.execute(() -> {
			for (var listener : this.unsubscribeListener) {
				listener.accept(this.bridge);
			}
		});
	}

	@Override
	public String toString() {
		return "BridgeComponentStateHandler{" //
				+ "state=" + this.state //
				+ ", componentId='" + this.componentId + '\'' //
				+ '}';
	}

	private State compareAndExchangeState(State expected, State newValue) {
		synchronized (this.onStateChangeListener) {
			if (this.state != expected || expected == newValue) {
				return this.state;
			}
			this.state = newValue;
			this.notifyStateChange(expected, this.state);
			return expected;
		}
	}

	private State setState(State state) {
		synchronized (this.onStateChangeListener) {
			var prev = this.state;
			this.state = state;
			if (prev == state) {
				return state;
			}
			this.notifyStateChange(prev, state);
			return prev;
		}
	}

	private void notifyStateChange(State prev, State current) {
		for (var listener : this.onStateChangeListener) {
			listener.accept(prev, current);
		}
	}
}
