package io.openems.edge.common.channel;

import java.time.LocalDateTime;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.internal.AbstractReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.type.CircularTreeMap;
import io.openems.edge.common.type.TypeUtils;

/**
 * An OpenEMS Channel holds one specific piece of information of an
 * {@link OpenemsComponent}.
 *
 * <p>
 * A Channel has
 * <ul>
 * <li>a Channel-ID which is unique among the OpenemsComponent. (see
 * {@link io.openems.edge.common.channel.ChannelId})
 * <li>a {@link Doc} as static meta information. (via
 * {@link Channel#channelDoc()})
 * <li>a system-wide unique {@link ChannelAddress} built from Component-ID and
 * Channel-ID. (via {@link Channel#address()}
 * <li>a {@link OpenemsType} which needs to map to the generic parameter
 * &lt;T&gt;. (via {@link Channel#getType()})
 * <li>an (active) {@link Value}. (via {@link Channel#value()})
 * <li>callback methods to listen on value updates and changes. (see
 * {@link Channel#onChange(Consumer)}, {@link Channel#onUpdate(Consumer)} and
 * {@link Channel#onSetNextValue(Consumer)})
 * </ul>
 *
 * <p>
 * Channels implement a 'Process Image' pattern. They provide an 'active' value
 * which should be used for any operations on the channel value. The 'next'
 * value is filled by asynchronous workers in the background. At the 'Process
 * Image Switch' the 'next' value is copied to the 'current' value.
 *
 * <p>
 * The recommended implementation of an OpenEMS Channel is via
 * {@link AbstractReadChannel}.
 *
 * @param <T> the type of the Channel. One out of {@link OpenemsType}.
 */
public interface Channel<T> {

	/**
	 * Holds the number of past values for this Channel that are kept in the
	 * 'pastValues' variable.
	 */
	public static final int NO_OF_PAST_VALUES = 300;

	/**
	 * Gets the ChannelId of this Channel.
	 *
	 * @return the ChannelId
	 */
	io.openems.edge.common.channel.ChannelId channelId();

	/**
	 * Gets the ChannelDoc of this Channel.
	 *
	 * @return the ChannelDoc
	 */
	default Doc channelDoc() {
		return this.channelId().doc();
	}

	/**
	 * Gets the OpenemsComponent this Channel belongs to.
	 *
	 * @return the OpenemsComponent
	 */
	OpenemsComponent getComponent();

	/**
	 * Gets the address of this Channel.
	 *
	 * @return the {@link ChannelAddress}
	 */
	ChannelAddress address();

	/**
	 * Switches to the next process image, i.e. copies the "next"-value into
	 * "current"-value.
	 */
	void nextProcessImage();

	/**
	 * Gets the type of this Channel, e.g. INTEGER, BOOLEAN,..
	 *
	 * @return the {@link OpenemsType}
	 */
	OpenemsType getType();

	/**
	 * Updates the 'next value' of Channel.
	 *
	 * @param value the 'next value'. It is going to be the 'value' after the next
	 *              ProcessImage gets activated.
	 */
	public default void setNextValue(Object value) {
		try {
			this._setNextValue(TypeUtils.<T>getAsType(this.getType(), value));
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(
					"Unable to set value [" + value + "] for Channel [" + this.address() + "]: " + e.getMessage(), e);
		}
	}

	/**
	 * Gets the 'next value'.
	 *
	 * <p>
	 * Note that usually you should prefer the value() method.
	 *
	 * @return the 'next value'
	 */
	public Value<T> getNextValue();

	/**
	 * Add an onSetNextValue callback. It is called, after a new NextValue was set.
	 * Note that usually you should prefer the onUpdate() callback.
	 *
	 * <p>
	 * Remember to remove the callback using
	 * {@link #removeOnSetNextValueCallback(Consumer)} once it is not needed
	 * anymore, e.g. on deactivate().
	 *
	 * @see #onUpdate
	 *
	 * @param callback the callback {@link Consumer}
	 * @return the callback to enable fluent programming
	 */
	// TODO rename to 'addOnSetNextValueCallback()'; apply same naming also for
	// other callbacks
	public Consumer<Value<T>> onSetNextValue(Consumer<Value<T>> callback);

	/**
	 * Removes an onSetNextValue callback.
	 *
	 * @see #onSetNextValue(Consumer)
	 * @param callback the callback {@link Consumer}
	 */
	public void removeOnSetNextValueCallback(Consumer<?> callback);

	/**
	 * Internal method. Do not call directly.
	 *
	 * @param value the 'next value'
	 */
	@Deprecated
	public void _setNextValue(T value);

	/**
	 * Gets the currently active value, wrapped in a @{link Value}.
	 *
	 * @return the active value
	 * @throws IllegalArgumentException if value cannot be access, e.g. because the
	 *                                  Channel is Write-Only.
	 */
	Value<T> value() throws IllegalArgumentException;

	/**
	 * Gets the past values for this Channel.
	 *
	 * @return a map of recording time and historic value at that time
	 */
	// TODO this should be a ZonedDateTime
	public CircularTreeMap<LocalDateTime, Value<T>> getPastValues();

	/**
	 * Add an onUpdate callback. It is called, after the active value was updated by
	 * nextProcessImage().
	 * 
	 * @param callback the callback
	 * @return the same callback for fluent coding
	 */
	public Consumer<Value<T>> onUpdate(Consumer<Value<T>> callback);

	/**
	 * Removes an onUpdate callback.
	 *
	 * @see #onUpdate(Consumer)
	 * @param callback the callback {@link Consumer}
	 */
	public void removeOnUpdateCallback(Consumer<Value<?>> callback);

	/**
	 * Add an onChange callback. It is called, after a new, different active value
	 * was set by nextProcessImage().
	 *
	 * @param callback old value and new value
	 * @return the same callback for fluent coding
	 */
	public BiConsumer<Value<T>, Value<T>> onChange(BiConsumer<Value<T>, Value<T>> callback);

	/**
	 * Removes an onChange callback.
	 *
	 * @see #onChange(BiConsumer)
	 * @param callback the callback {@link BiConsumer}
	 */
	public void removeOnChangeCallback(BiConsumer<?, ?> callback);

	/**
	 * Deactivates the Channel and makes sure all callbacks are released for garbe
	 * collection to avoid memory-leaks.
	 */
	public void deactivate();

	/**
	 * Sets an object that holds meta information about the Channel, e.g. a read
	 * source or write target of this Channel, like a Modbus Register or REST-Api
	 * endpoint address. Defaults to null.
	 *
	 * @param <META_INFO> the type of the meta info
	 * @param metaInfo    the meta info object
	 * @throws IllegalArgumentException if there is already a different meta-info
	 *                                  registered with the Channel
	 */
	public <META_INFO> void setMetaInfo(META_INFO metaInfo) throws IllegalArgumentException;

	/**
	 * Gets the meta information object. Defaults to null.
	 *
	 * @param <META_INFO> the type of the meta info attachment
	 * @return the meta info object
	 */
	public <META_INFO> META_INFO getMetaInfo();

}
