package io.openems.edge.common.channel;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.doc.ChannelId;
import io.openems.edge.common.component.OpenemsComponent;

public abstract class AbstractReadChannel<T> implements Channel<T> {

	private final Logger log = LoggerFactory.getLogger(AbstractReadChannel.class);

	private final ChannelId channelId;
	private final OpenemsComponent component;
	private final OpenemsType type;

	private volatile T nextValue = null; // TODO add timeout for nextValue validity
	private volatile T activeValue = null;

	public AbstractReadChannel(OpenemsType type, OpenemsComponent component, ChannelId channelId) {
		this(type, component, channelId, null);
	}

	public AbstractReadChannel(OpenemsType type, OpenemsComponent component, ChannelId channelId, T initialValue) {
		this.type = type;
		this.component = component;
		this.channelId = channelId;
		// call onInitCallback from Doc
		Optional<Consumer<Channel<?>>> onInitCallback = this.channelId.doc().getOnInitCallback();
		if (onInitCallback.isPresent()) {
			onInitCallback.get().accept(this);
		}
		this.setNextValue(initialValue);
	}

	@Override
	public ChannelId channelId() {
		return this.channelId;
	}

	@Override
	public OpenemsComponent getComponent() {
		return component;
	}

	@Override
	public void nextProcessImage() {
		this.activeValue = this.nextValue;
		this.onUpdateCallbacks.forEach(callback -> callback.accept(this.activeValue));
	}

	@Override
	public ChannelAddress address() {
		return new ChannelAddress(this.component.id(), this.channelId().id());
	}

	@Override
	public OpenemsType getType() {
		return this.type;
	}

	@Override
	public T getActiveValueOrNull() {
		return activeValue;
	}

	/**
	 * Sets the next value. Internal method. Do not call directly.
	 * 
	 * @param value
	 * @throws OpenemsException
	 */
	@Deprecated
	public final void _setNextValue(T value) {
		this.nextValue = value;
		if (this.channelDoc().isDebug()) {
			log.info("Next value for [" + this.address() + "]: "
					+ this.channelDoc().getUnit().format(value, this.getType()));
		}
		this.onSetNextValueCallbacks.forEach(callback -> callback.accept(value));
	}

	@Override
	public String toString() {
		return "Channel [ID=" + channelId + ", type=" + type + ", activeValue=" + this.format() + "]";
	}

	private final List<Consumer<T>> onUpdateCallbacks = new CopyOnWriteArrayList<>();

	@Override
	public void onUpdate(Consumer<T> callback) {
		this.onUpdateCallbacks.add(callback);
	}

	private final List<Consumer<T>> onSetNextValueCallbacks = new CopyOnWriteArrayList<>();

	public void onSetNextValue(Consumer<T> callback) {
		this.onSetNextValueCallbacks.add(callback);
	}

}
