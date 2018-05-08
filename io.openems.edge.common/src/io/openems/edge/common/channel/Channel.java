package io.openems.edge.common.channel;

import java.util.function.Consumer;

import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.TypeUtils;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public interface Channel<T> {
	/**
	 * Gets the ChannelId of this Channel
	 * 
	 * @return
	 */
	io.openems.edge.common.channel.doc.ChannelId channelId();

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
		this._setNextValue(TypeUtils.<T>getAsType(this.getType(), value));
	}

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
	 * Add an onUpdate callback. It is called, after a new ActiveValue was set via
	 * nextProcessImage().
	 */
	public void onUpdate(Consumer<Value<T>> callback);
}
