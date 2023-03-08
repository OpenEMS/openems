package io.openems.edge.common.channel.internal;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingConsumer;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public abstract class AbstractReadChannel<D extends AbstractDoc<T>, T> implements Channel<T> {

	private final Logger log = LoggerFactory.getLogger(AbstractReadChannel.class);

	protected final OpenemsComponent parent;

	private final OpenemsType type;
	private final ChannelId channelId;
	private final D channelDoc;
	private final List<Consumer<Value<T>>> onUpdateCallbacks = new CopyOnWriteArrayList<>();
	private final List<Consumer<Value<T>>> onSetNextValueCallbacks = new CopyOnWriteArrayList<>();
	private final List<BiConsumer<Value<T>, Value<T>>> onChangeCallbacks = new CopyOnWriteArrayList<>();
	private final TreeMap<LocalDateTime, Value<T>> pastValues = new TreeMap<>();

	private volatile Value<T> nextValue = null;
	private volatile Value<T> activeValue = null;

	protected AbstractReadChannel(OpenemsType type, OpenemsComponent parent, ChannelId channelId, D channelDoc) {
		this.type = type;
		this.parent = parent;
		this.channelId = channelId;
		this.channelDoc = channelDoc;
		this.nextValue = new Value<>(this, null);
		this.activeValue = new Value<>(this, null);

		// validate Type
		if (!this.validateType(channelDoc.getType(), type)) {
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
		this.setNextValue(channelDoc.getInitialValue());
	}

	@Override
	public void deactivate() {
		this.onChangeCallbacks.clear();
		this.onSetNextValueCallbacks.clear();
		this.onUpdateCallbacks.clear();
		if (this.onSetNextWriteCallbacks != null) {
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
		return this.parent;
	}

	@Override
	public void nextProcessImage() {
		var oldValue = this.activeValue;
		final boolean valueHasChanged;
		if (oldValue == null && this.nextValue == null) {
			valueHasChanged = false;
		} else if (oldValue == null || this.nextValue == null) {
			valueHasChanged = true;
		} else {
			valueHasChanged = !Objects.equals(oldValue.get(), this.nextValue.get());
		}
		this.activeValue = this.nextValue;
		this.onUpdateCallbacks.forEach(callback -> callback.accept(this.activeValue));
		if (valueHasChanged) {
			this.onChangeCallbacks.forEach(callback -> callback.accept(oldValue, this.activeValue));
		}
		this.appendPastValue(this.activeValue);
	}

	/**
	 * Appends a value to `pastValues` and deletes entries that are elder than
	 * {@link Channel#MAX_AGE_OF_PAST_VALUES}.
	 * 
	 * @param value a new {@link Value}
	 */
	private void appendPastValue(Value<T> value) {
		final var compareTime = value.getTimestamp().minus(Channel.MAX_AGE_OF_PAST_VALUES);
		this.pastValues.put(value.getTimestamp(), value);
		// changes to sub map are also applied to the backed map
		this.pastValues.headMap(compareTime).clear();
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
	@Override
	@Deprecated
	public void _setNextValue(T value) {
		this.nextValue = new Value<>(this, value);
		if (this.channelDoc.isDebug()) {
			this.log.info("Next value for [" + this.address() + "]: " + this.nextValue.asString());
		}
		this.onSetNextValueCallbacks.forEach(callback -> callback.accept(this.nextValue));
	}

	@Override
	public Value<T> getNextValue() {
		return this.nextValue;
	}

	@Override
	public Value<T> value() throws IllegalArgumentException {
		switch (this.channelDoc.getAccessMode()) {
		case WRITE_ONLY:
			throw new IllegalArgumentException("Channel [" + this.channelId.id() + "] is WRITE_ONLY.");
		case READ_ONLY:
		case READ_WRITE:
			break;
		}
		return this.activeValue;
	}

	@Override
	public String toString() {
		return "Channel [" //
				+ "ID=" + this.channelId.id() + ", " //
				+ "type=" + this.type + ", " //
				+ "activeValue=" + this.activeValue.asString() + ", "//
				+ "access=" + this.channelDoc.getAccessMode() //
				+ "]";
	}

	@Override
	public Consumer<Value<T>> onUpdate(Consumer<Value<T>> callback) {
		this.onUpdateCallbacks.add(callback);
		return callback;
	}

	@Override
	public void removeOnUpdateCallback(Consumer<Value<?>> callback) {
		this.onUpdateCallbacks.remove(callback);
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
	public BiConsumer<Value<T>, Value<T>> onChange(BiConsumer<Value<T>, Value<T>> callback) {
		this.onChangeCallbacks.add(callback);
		return callback;
	}

	@Override
	public void removeOnChangeCallback(BiConsumer<?, ?> callback) {
		this.onChangeCallbacks.remove(callback);
	}

	/*
	 * This is to help WriteChannels implement the WriteChannel interface.
	 * 'onSetNextWriteCallbacks' is not final by purpose, because it might be called
	 * in construction and would not be initialised then.
	 */
	private List<ThrowingConsumer<T, OpenemsNamedException>> onSetNextWriteCallbacks = null;

	protected List<ThrowingConsumer<T, OpenemsNamedException>> getOnSetNextWrites() {
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
	public TreeMap<LocalDateTime, Value<T>> getPastValues() {
		return this.pastValues;
	}

	/**
	 * An object that holds information about the source of this Channel, i.e. a
	 * Modbus Register or REST-Api endpoint address. Defaults to null.
	 */
	private Object source = null;

	@Override
	public <SOURCE> void setMetaInfo(SOURCE source) throws IllegalArgumentException {
		if (this.source != null && source != null && !Objects.equals(this.source, source)) {
			throw new IllegalArgumentException("Unable to set meta info [" + source.toString() + "]." //
					+ " Channel [" + this.address() + "] already has one [" + this.source.toString() + "]. " //
					+ "Hint: Possibly you are trying to map a single Channel to multiple Modbus Registers. " //
					+ "If this is on purpose, you can manually provide a `ChannelMetaInfoReadAndWrite` object.");
		}
		this.source = source;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <SOURCE> SOURCE getMetaInfo() {
		return (SOURCE) this.source;
	}
}
