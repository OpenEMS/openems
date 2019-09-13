package io.openems.edge.common.channel;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.internal.AbstractReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.type.TypeUtils;

/**
 * An OpenEMS Channel holds one specific piece of information of an
 * {@link OpenemsComponent}.
 *
 * A Channel has
 * <ul>
 * <li>a Channel-ID which is unique among the OpenemsComponent. (see
 * {@link io.openems.edge.common.channel.ChannelId})
 * <li>a {@link Doc} as static meta information. (via {@link #channelDoc()})
 * <li>a system-wide unique {@link ChannelAddress} built from Component-ID and
 * Channel-ID. (via {@link #address()}
 * <li>a {@link OpenemsType} which needs to map to the generic parameter
 * &lt;T&gt;. (via {@link #getType()})
 * <li>an (active) {@link Value}. (via {@link #value()})
 * <li>callback methods to listen on value updates and changes. (see
 * {@link #onChange(Consumer)}, {@link #onUpdate(Consumer)} and
 * {@link #onSetNextValue(Consumer)})
 * </ul>
 * 
 * Channels implement a 'Process Image' pattern. They provide an 'active' value
 * which should be used for any operations on the channel value. The 'next'
 * value is filled by asynchronous workers in the background. At the 'Process
 * Image Switch' the 'next' value is copied to the 'current' value.
 * 
 * The recommended implementation of an OpenEMS Channel is via
 * {@link AbstractReadChannel}.
 *
 * @param <T>
 */
public interface Channel<T> {
	/**
	 * Gets the ChannelId of this Channel
	 * 
	 * @return
	 */
	io.openems.edge.common.channel.ChannelId channelId();

	/**
	 * Gets the ChannelDoc of this Channel
	 * 
	 * @return
	 */
	default Doc channelDoc() {
		return this.channelId().doc();
	}

	/**
	 * Gets the OpenemsComponent this Channel belongs to
	 * 
	 * @return
	 */
	OpenemsComponent getComponent();

	/**
	 * Gets the address of this Channel
	 * 
	 * @return
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
	 * @return
	 */
	OpenemsType getType();

	/**
	 * Updates the 'next' value of Channel.
	 * 
	 * @param value
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
	 * Gets the NextValue.
	 * 
	 * Note that usually you should prefer the value() method.
	 * 
	 * @return
	 */
	public Value<T> getNextValue();

	/**
	 * Add an onSetNextValue callback. It is called, after a new NextValue was set.
	 * Note that usually you should prefer the onUpdate() callback.
	 * 
	 * @see #onUpdate
	 */
	public void onSetNextValue(Consumer<Value<T>> callback);

	/**
	 * Internal method. Do not call directly.
	 * 
	 * @param value
	 */
	@Deprecated
	public void _setNextValue(T value);

	/**
	 * Gets the currently active value, wrapped in a @{link Value}.
	 */
	Value<T> value();

	/**
	 * Add an onUpdate callback. It is called, after the active value was updated by
	 * nextProcessImage().
	 */
	public void onUpdate(Consumer<Value<T>> callback);

	/**
	 * Add an onChange callback. It is called, after a new, different active value
	 * was set by nextProcessImage().
	 * 
	 * @param callback old value and new value
	 */
	public void onChange(BiConsumer<Value<T>, Value<T>> callback);

	/**
	 * Deactivates the Channel and makes sure all callbacks are released for garbe
	 * collection to avoid memory-leaks.
	 */
	public void deactivate();
}
