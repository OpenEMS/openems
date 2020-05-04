package io.openems.edge.common.channel.internal;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.CheckedConsumer;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.type.CircularTreeMap;

public abstract class AbstractReadChannel<D extends AbstractDoc<T>, T> implements Channel<T> {

	/**
	 * Holds the number of past values for this Channel that are kept in the
	 * 'pastValues' variable.
	 */
	public final static int NO_OF_PAST_VALUES = 100;

	private final Logger log = LoggerFactory.getLogger(AbstractReadChannel.class);

	protected final OpenemsComponent parent;

	private final OpenemsType type;
	private final ChannelId channelId;
	private final D channelDoc;
	private final List<Consumer<Value<T>>> onUpdateCallbacks = new CopyOnWriteArrayList<>();
	private final List<Consumer<Value<T>>> onSetNextValueCallbacks = new CopyOnWriteArrayList<>();
	private final List<BiConsumer<Value<T>, Value<T>>> onChangeCallbacks = new CopyOnWriteArrayList<>();
	private final CircularTreeMap<LocalDateTime, Value<T>> pastValues = new CircularTreeMap<>(NO_OF_PAST_VALUES);

	private volatile Value<T> nextValue = null;
	private volatile Value<T> activeValue = null;

	protected AbstractReadChannel(OpenemsType type, OpenemsComponent parent, ChannelId channelId, D channelDoc,
			T initialValue) {
		this.type = type;
		this.parent = parent;
		this.channelId = channelId;
		this.channelDoc = channelDoc;
		this.nextValue = new Value<T>(this, null);
		this.activeValue = new Value<T>(this, null);

		// validate Type
		if (!validateType(channelDoc.getType(), type)) {
			throw new IllegalArgumentException("[" + this.address() + "]: Types do not match. Got [" + type
					+ "]. Expected [" + channelDoc.getType() + "].");
		}

		// validate Access-Mode
		switch (channelDoc.getAccessMode()) {
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
		channelDoc.getOnInitCallbacks().forEach(callback -> {
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
	public D channelDoc() {
		return this.channelDoc;
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
		Value<T> oldValue = this.activeValue;
		boolean valueHasChanged = !Objects.equals(oldValue, this.nextValue);
		this.activeValue = this.nextValue;
		this.onUpdateCallbacks.forEach(callback -> callback.accept(this.activeValue));
		if (valueHasChanged) {
			this.onChangeCallbacks.forEach(callback -> callback.accept(oldValue, this.activeValue));
		}
		this.pastValues.put(this.activeValue.getTimestamp(), this.activeValue);
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
	public void _setNextValue(T value) {
		this.nextValue = new Value<T>(this, value);
		if (this.channelDoc.isDebug()) {
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
		switch (this.channelDoc.getAccessMode()) {
		case WRITE_ONLY:
			throw new IllegalArgumentException("Channel [" + this.channelId + "] is WRITE_ONLY.");
		case READ_ONLY:
		case READ_WRITE:
			break;
		}
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
	public Consumer<Value<T>> onSetNextValue(Consumer<Value<T>> callback) {
		this.onSetNextValueCallbacks.add(callback);
		return callback;
	}

	@Override
	public void removeOnSetNextValueCallback(Consumer<?> callback) {
		this.onSetNextValueCallbacks.remove(callback);
	}

	@Override
	public void onChange(BiConsumer<Value<T>, Value<T>> callback) {
		this.onChangeCallbacks.add(callback);
	}

	/*
	 * This is to help WriteChannels implement the WriteChannel interface.
	 * 'onSetNextWriteCallbacks' is not final by purpose, because it might be called
	 * in construction and would not be initialised then.
	 */
	private List<CheckedConsumer<T>> onSetNextWriteCallbacks = null;

	protected List<CheckedConsumer<T>> getOnSetNextWrites() {
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

	/**
	 * Gets the past values for this Channel.
	 * 
	 * @return a map of recording time and historic value at that time
	 */
	@Override
	public CircularTreeMap<LocalDateTime, Value<T>> getPastValues() {
		return this.pastValues;
	}
}
