package io.openems.edge.common.channel;

import java.util.List;
import java.util.Optional;

import io.openems.common.exceptions.CheckedConsumer;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.type.TypeUtils;

public interface WriteChannel<T> extends Channel<T> {

	/**
	 * Updates the 'next' write value of Channel.
	 * 
	 * @param value the typed value
	 * @throws OpenemsNamedException on error
	 */
	public default void setNextWriteValue(T value) throws OpenemsNamedException {
		this.setNextWriteValueFromObject(value);
	}

	/**
	 * Updates the 'next' write value of Channel from an Object value.
	 * 
	 * <p>
	 * Use this method if the value is not yet in the correct Type. Otherwise use
	 * {@link WriteChannel#setNextWriteValue(Object)} directly.
	 * 
	 * @param value the value as an Object
	 * @throws OpenemsNamedException on error
	 */
	public default void setNextWriteValueFromObject(Object value) throws OpenemsNamedException {
		T typedValue = TypeUtils.<T>getAsType(this.getType(), value);
		OpenemsNamedException exception = null;
		// set the write value
		this._setNextWriteValue(typedValue);
		for (CheckedConsumer<T> callback : this.getOnSetNextWrites()) {
			try {
				callback.accept(typedValue);
			} catch (OpenemsNamedException e) {
				exception = e;
			}
		}
		if (exception != null) {
			throw exception;
		}
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
	 * 
	 * <p>
	 * The callback can throw an {@link OpenemsNamedException}.
	 */
	public void onSetNextWrite(CheckedConsumer<T> callback);

	public List<CheckedConsumer<T>> getOnSetNextWrites();

}
