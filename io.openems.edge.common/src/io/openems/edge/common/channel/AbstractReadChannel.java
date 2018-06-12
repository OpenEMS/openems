package io.openems.edge.common.channel;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.doc.ChannelId;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public abstract class AbstractReadChannel<T> implements Channel<T> {

	private final Logger log = LoggerFactory.getLogger(AbstractReadChannel.class);

	private final ChannelId channelId;
	private final OpenemsComponent component;
	private final OpenemsType type;
	private final List<Consumer<Value<T>>> onUpdateCallbacks = new CopyOnWriteArrayList<>();
	private final List<Consumer<Value<T>>> onSetNextValueCallbacks = new CopyOnWriteArrayList<>();

	private volatile Value<T> nextValue = null; // TODO add timeout for nextValue validity
	private volatile Value<T> activeValue = null;

	public AbstractReadChannel(OpenemsType type, OpenemsComponent component, ChannelId channelId) {
		this(type, component, channelId, null);
	}

	public AbstractReadChannel(OpenemsType type, OpenemsComponent component, ChannelId channelId, T initialValue) {
		this.nextValue = new Value<T>(this, null);
		this.activeValue = new Value<T>(this, null);
		this.type = type;
		this.component = component;
		this.channelId = channelId;
		// validate Type
		if (channelId.doc().getType().isPresent()) {
			if (!type.equals(channelId.doc().getType().get())) {
				throw new IllegalArgumentException("[" + this.address() + "]: Types do not match. Got [" + type
						+ "]. Expected [" + channelId.doc().getType().get() + "].");
			}
		}
		// validate isWritable
		if (channelId.doc().getIsWritable()) {
			if (!(this instanceof WriteChannel)) {
				throw new IllegalArgumentException(
						"[" + this.address() + "]: This Channel needs to implement WriteChannel.");
			}
		}
		// call onInitCallback from Doc
		this.channelId.doc().getOnInitCallback().forEach(callback -> {
			callback.accept(this);
		});
		// set initial value
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

	/**
	 * Sets the next value. Internal method. Do not call directly.
	 * 
	 * @param value
	 * @throws OpenemsException
	 */
	@Deprecated
	public final void _setNextValue(T value) {
		this.nextValue = new Value<T>(this, value);
		if (this.channelDoc().isDebug()) {
			log.info("Next value for [" + this.address() + "]: " + this.nextValue.asString());
		}
		this.onSetNextValueCallbacks.forEach(callback -> callback.accept(this.nextValue));
	}

	@Override
	public Value<T> getNextValue() {
		return this.nextValue;
	}

	@Override
	public Value<T> value() {
		return this.activeValue;
	}

	@Override
	public String toString() {
		return "Channel [ID=" + channelId + ", type=" + type + ", activeValue=" + this.activeValue.asString() + "]";
	}

	@Override
	public void onUpdate(Consumer<Value<T>> callback) {
		this.onUpdateCallbacks.add(callback);
	}

	public void onSetNextValue(Consumer<Value<T>> callback) {
		this.onSetNextValueCallbacks.add(callback);
	}

	/*
	 * This is to help WriteChannels implement the WriteChannel interface.
	 * 'onSetNextWriteCallbacks' is not final by purpose, because it might be called
	 * in construction and would not be initialised then.
	 */
	private List<Consumer<T>> onSetNextWriteCallbacks = null;

	protected List<Consumer<T>> getOnSetNextWrites() {
		if (this.onSetNextWriteCallbacks == null) {
			this.onSetNextWriteCallbacks = new CopyOnWriteArrayList<>();
		}
		return this.onSetNextWriteCallbacks;
	}
}
