package io.openems.edge.common.channel;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.type.TypeUtils;

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
	 * Updates the 'next' write value of Channel from an Object value.
	 * 
	 * <p>
	 * Use this method if the value is not yet in the correct Type. Otherwise use
	 * setNextWriteValue() directly.
	 * 
	 * @param value
	 */
	@Deprecated
	public default void setNextWriteValueFromObject(T value) throws OpenemsException {
		T typedValue = TypeUtils.<T>getAsType(this.getType(), value);
		// set the write value
		this._setNextWriteValue(typedValue);
		this.getOnSetNextWrites().forEach(callback -> callback.accept(typedValue));
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
		Optional<T> valueOpt = this.getNextWriteValue();
		if (valueOpt.isPresent()) {
			this._setNextWriteValue(null);
		}
		return valueOpt;
	}

	/**
	 * Gets the next write value.
	 * 
	 * @return the next write value; not-present if no write value had been set
	 */
	public Optional<T> getNextWriteValue();

	/**
	 * Add an onSetNextWrite callback. It is called when a 'next write value' was
	 * set.
	 */
	public void onSetNextWrite(Consumer<T> callback);

	public List<Consumer<T>> getOnSetNextWrites();

}
