package io.openems.edge.common.channel;

import java.util.List;
import java.util.Optional;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingConsumer;
import io.openems.edge.common.type.TypeUtils;

public interface WriteChannel<T> extends Channel<T> {

	/**
	 * Updates the 'next' write value of Channel.
	 * 
	 * @param value the typed value
	 * @throws OpenemsNamedException on error
	 */
	public default void setNextWriteValue(T value) throws OpenemsNamedException {
		try {
			this.setNextWriteValueFromObject(value);
		} catch (IllegalArgumentException e) {
			System.out.println("Unable to set Next Write Value [" + value + "] on Channel [" + this.address() + "]: "
					+ e.getMessage());
			throw e;
		}
	}

	/**
	 * Updates the 'next' write value of Channel from an Object value.
	 * 
	 * <p>
	 * Use this method if the value is not yet in the correct Type. Otherwise use
	 * {@link WriteChannel#setNextWriteValue(Object)} directly.
	 * 
	 * @param value the value as an Object
	 * @throws OpenemsNamedException    on error
	 * @throws IllegalArgumentException on error
	 */
	public default void setNextWriteValueFromObject(Object value)
			throws OpenemsNamedException, IllegalArgumentException {
		T typedValue = TypeUtils.<T>getAsType(this.getType(), value);
		OpenemsNamedException exception = null;
		// set the write value
		this._setNextWriteValue(typedValue);
		for (ThrowingConsumer<T, OpenemsNamedException> callback : this.getOnSetNextWrites()) {
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
	public void onSetNextWrite(ThrowingConsumer<T, OpenemsNamedException> callback);

	public List<ThrowingConsumer<T, OpenemsNamedException>> getOnSetNextWrites();

	/**
	 * Sets an object that holds information about the write target of this Channel,
	 * i.e. a Modbus Register or REST-Api endpoint address. Defaults to null.
	 * 
	 * @param <TARGET> the type of the target attachment
	 * @param target   the target object
	 * @return myself
	 * @throws IllegalArgumentException if there is already a target registered with
	 *                                  the Channel
	 */
	public <TARGET> void setWriteTarget(TARGET target) throws IllegalArgumentException;

	/**
	 * Gets the write target information object. Defaults to empty String.
	 * 
	 * @param <SOURCE> the type of the target attachment
	 * @return the target information object
	 */
	public <TARGET> TARGET getWriteTarget();
}
