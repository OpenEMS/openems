package io.openems.edge.common.channel;

import java.util.Optional;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.TypeUtils;

public interface WriteChannel<T> extends Channel<T> {

	/**
	 * Updates the 'next' write value of Channel.
	 * 
	 * @param value
	 */
	public default void setNextWriteValue(Object value) throws OpenemsException {
		this._setNextWriteValue(TypeUtils.<T>getAsType(this.getType(), value));
	}

	public void _setNextWriteValue(T value);

	/**
	 * Gets the next write value and resets it.
	 * 
	 * @return
	 */
	public default Optional<T> getNextWriteValueAndReset() {
		Optional<T> valueOpt = this._getNextWriteValue();
		this._setNextValue(null);
		return valueOpt;
	}

	public Optional<T> _getNextWriteValue();
}
