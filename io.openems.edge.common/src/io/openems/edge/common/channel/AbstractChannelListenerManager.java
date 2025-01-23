package io.openems.edge.common.channel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Helper wrapping class to manage everything related to Channel Listeners; It
 * takes care of registering and unregistering listeners.
 */
public abstract class AbstractChannelListenerManager {

	private abstract static class Listener<CALLBACK> {
		protected final OpenemsComponent component;
		protected final ChannelId channelId;
		protected final CALLBACK callback;

		public Listener(OpenemsComponent component, ChannelId channelId, CALLBACK callback) {
			this.component = component;
			this.channelId = channelId;
			this.callback = callback;
		}
	}

	private static class OnSetNextValueListener<T> extends Listener<Consumer<Value<T>>> {

		public OnSetNextValueListener(OpenemsComponent component, ChannelId channelId, Consumer<Value<T>> callback) {
			super(component, channelId, callback);
		}

	}

	private static class OnChangeListener<T> extends Listener<BiConsumer<Value<T>, Value<T>>> {

		public OnChangeListener(OpenemsComponent component, ChannelId channelId,
				BiConsumer<Value<T>, Value<T>> callback) {
			super(component, channelId, callback);
		}

	}

	private final List<OnSetNextValueListener<?>> onSetNextValueListeners = new ArrayList<>();
	private final List<OnChangeListener<?>> onChangeListeners = new ArrayList<>();

	/**
	 * Called on deactivate(). Remove all callbacks from Channels.
	 */
	public synchronized void deactivate() {
		for (OnSetNextValueListener<?> listener : this.onSetNextValueListeners) {
			Channel<?> channel = listener.component.channel(listener.channelId);
			channel.removeOnSetNextValueCallback(listener.callback);
		}
		this.onSetNextValueListeners.clear();
		for (OnChangeListener<?> listener : this.onChangeListeners) {
			Channel<?> channel = listener.component.channel(listener.channelId);
			channel.removeOnChangeCallback(listener.callback);
		}
		this.onChangeListeners.clear();
	}

	/**
	 * Adds a Listener. Also applies the callback once to make sure it applies
	 * already existing values.
	 *
	 * @param <T>       the Channel value type
	 * @param component the Component
	 * @param channelId the ChannelId
	 * @param callback  the callback
	 */
	protected <T> void addOnSetNextValueListener(OpenemsComponent component, ChannelId channelId,
			Consumer<Value<T>> callback) {
		this.onSetNextValueListeners.add(new OnSetNextValueListener<>(component, channelId, callback));
		Channel<T> channel = component.channel(channelId);
		channel.onSetNextValue(callback);
		callback.accept(channel.getNextValue());
	}

	/**
	 * Adds a Listener. Also applies the callback once to make sure it applies
	 * already existing values.
	 *
	 * @param <T>       the Channel value type
	 * @param component the Component
	 * @param channelId the ChannelId
	 * @param callback  the callback
	 */
	protected <T> void addOnChangeListener(OpenemsComponent component, ChannelId channelId,
			BiConsumer<Value<T>, Value<T>> callback) {
		this.onChangeListeners.add(new OnChangeListener<>(component, channelId, callback));
		Channel<T> channel = component.channel(channelId);
		channel.onChange(callback);
		callback.accept(null, channel.getNextValue());
	}
}
