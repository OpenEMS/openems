package io.openems.edge.bridge.modbus.api.element;

import java.util.function.Consumer;

public interface ModbusReadElement<T> extends ModbusElement<T> {

	/**
	 * Adds an On-Update-Callback.
	 * 
	 * @param onUpdateCallback the Callback
	 * @return myself
	 */
	public AbstractModbusElement<T> onUpdateCallback(Consumer<T> onUpdateCallback);

}
