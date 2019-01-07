package io.openems.edge.common.channel;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.doc.ChannelId;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public abstract class AbstractReadChannel<T> implements Channel<T> {

	private final Logger log = LoggerFactory.getLogger(AbstractReadChannel.class);

	protected final OpenemsComponent parent;

	private final ChannelId channelId;
	private final OpenemsType type;
	private final List<Consumer<Value<T>>> onUpdateCallbacks = new CopyOnWriteArrayList<>();
	private final List<Consumer<Value<T>>> onSetNextValueCallbacks = new CopyOnWriteArrayList<>();
	private final List<Consumer<Value<T>>> onChangeCallbacks = new CopyOnWriteArrayList<>();

	private volatile Value<T> nextValue = null;
	private volatile Value<T> activeValue = null;

	public AbstractReadChannel(OpenemsType type, OpenemsComponent component, ChannelId channelId) {
		this(type, component, channelId, null);
	}

	public AbstractReadChannel(OpenemsType type, OpenemsComponent parent, ChannelId channelId, T initialValue) {
		this.nextValue = new Value<T>(this, null);
		this.activeValue = new Value<T>(this, null);
		this.type = type;
		this.parent = parent;
		this.channelId = channelId;

		// validate Type
		if (channelId.doc().getType().isPresent()) {
			if (!validateType(channelId.doc().getType().get(), type)) {
				throw new IllegalArgumentException("[" + this.address() + "]: Types do not match. Got [" + type
						+ "]. Expected [" + channelId.doc().getType().get() + "].");
			}
		}

		// validate Access-Mode
		switch (channelId.doc().getAccessMode()) {
		case READ_ONLY:
			break;
		case READ_WRITE:
		case WRITE_ONLY:
			if (!(this instanceof WriteChannel)) {
				throw new IllegalArgumentException(
						"[" + this.address() + "]: This Channel needs to implement WriteChannel.");
			}
			break;
		}
		// call onInitCallback from Doc
		this.channelId.doc().getOnInitCallback().forEach(callback -> {
			callback.accept(this);
		});
		// set initial value
		this.setNextValue(initialValue);
	}

	@Override
	public void deactivate() {
		this.onChangeCallbacks.clear();
		this.onSetNextValueCallbacks.clear();
		this.onUpdateCallbacks.clear();
		if (onSetNextWriteCallbacks != null) {
			this.onSetNextWriteCallbacks.clear();
		}
	}

	@Override
	public ChannelId channelId() {
		return this.channelId;
	}

	@Override
	public OpenemsComponent getComponent() {
		return parent;
	}

	@Override
	public void nextProcessImage() {
		boolean valueHasChanged = !Objects.equals(this.activeValue, this.nextValue);
		this.activeValue = this.nextValue;
		this.onUpdateCallbacks.forEach(callback -> callback.accept(this.activeValue));
		if (valueHasChanged) {
			this.onChangeCallbacks.forEach(callback -> callback.accept(this.activeValue));
		}
	}

	@Override
	public ChannelAddress address() {
		return new ChannelAddress(this.parent.id(), this.channelId().id());
	}

	@Override
	public OpenemsType getType() {
		return this.type;
	}

	/**
	 * Sets the next value. Internal method. Do not call directly.
	 * 
	 * @param value the next value
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

	@Override
	public void onSetNextValue(Consumer<Value<T>> callback) {
		this.onSetNextValueCallbacks.add(callback);
	}

	@Override
	public void onChange(Consumer<Value<T>> callback) {
		this.onChangeCallbacks.add(callback);
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

	/**
	 * Validates the Type of the Channel.
	 * 
	 * @param expected the expected Type
	 * @param actual   the actual Type
	 * @return true if validation ok
	 */
	private boolean validateType(OpenemsType expected, OpenemsType actual) {
		switch (expected) {
		case BOOLEAN:
		case FLOAT:
		case SHORT:
		case STRING:
			return actual == expected;
		case DOUBLE:
			switch (actual) {
			case DOUBLE:
			case FLOAT:
				return true;
			default:
				return false;
			}
		case INTEGER:
			switch (actual) {
			case SHORT:
			case INTEGER:
				return true;
			default:
				return false;
			}
		case LONG:
			switch (actual) {
			case SHORT:
			case INTEGER:
			case LONG:
				return true;
			default:
				return false;
			}
		}
		return false;
	}
}
