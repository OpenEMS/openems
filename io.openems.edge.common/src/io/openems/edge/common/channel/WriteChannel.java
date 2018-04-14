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
	public default void setNextWriteValue(Object value) throws OpenemsException {
		T typedValue = TypeUtils.<T>getAsType(this.getType(), value);
		this._setNextWriteValue(typedValue);
		this.getOnSetNextWriteCallback().accept(typedValue);
	}

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
	 * The onSetNextWriteCallback is called when a 'next write value' was set.
	 */
	public default void onSetNextWriteCallback(Consumer<T> onSetNextWriteCallback) {
		Consumer<T> existingCallback = this.getOnSetNextWriteCallback();
		if (existingCallback != null) {
			System.out.println("Setting new onSetNextWriteCallback for WriteChannel [" + this.channelId()
					+ "] overrides existing one!");
		}
		this._onSetNextWriteCallback(onSetNextWriteCallback);
	}

	public Consumer<T> getOnSetNextWriteCallback();

	public void _onSetNextWriteCallback(Consumer<T> onSetNextWriteCallback);
}
