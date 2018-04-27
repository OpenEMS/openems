package io.openems.edge.common.channel;

import java.util.Optional;
import java.util.function.Consumer;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.TypeUtils;

public interface WriteChannel<T> extends Channel<T> {

	/**
	 * Updates the 'next' write value of Channel.
	 * 
	 * @param value
	 */
	public default void setNextWriteValue(T value) throws OpenemsException {
		this.setNextWriteValueFromObject(value);
	}

	/**
	 * Updates the 'next' write value of Channel from an Object value. Use this
	 * method if the value is not yet in the correct Type. Otherwise use
	 * setNextWriteValue() directly.
	 * 
	 * @param value
	 */
	public default void setNextWriteValueFromObject(Object value) throws OpenemsException {
		T typedValue = TypeUtils.<T>getAsType(this.getType(), value);
		// set the write value
		this._setNextWriteValue(typedValue);
		// set the read value to the same value to enable debugging
		this.setNextValue(value);
		this.getOnSetNextWrite().accept(typedValue);
	}

	/**
	 * Internal method. Do not call directly.
	 * 
	 * @param value
	 */
	@Deprecated
	public void _setNextWriteValue(T value);

	/**
	 * Gets the next write value and resets it.
	 * 
	 * @return
	 */
	public default Optional<T> getNextWriteValueAndReset() {
		Optional<T> valueOpt = this._getNextWriteValue();
		this._setNextWriteValue(null);
		return valueOpt;
	}

	public Optional<T> _getNextWriteValue();

	/**
	 * The onSetNextWrite callback is called when a 'next write value' was set.
	 */
	public default void onSetNextWrite(Consumer<T> callback) {
		Consumer<T> existingCallback = this.getOnSetNextWrite();
		if (existingCallback != null) {
			System.out.println("Setting new onSetNextWriteCallback for WriteChannel [" + this.channelId()
					+ "] overrides existing one!");
		}
		this._onSetNextWrite(callback);
	}

	public Consumer<T> getOnSetNextWrite();

	/**
	 * Internal method. Do not call directly.
	 * 
	 * @param value
	 */
	@Deprecated
	public void _onSetNextWrite(Consumer<T> onSetNextWriteCallback);
}
