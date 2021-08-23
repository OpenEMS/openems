package io.openems.edge.bridge.modbus.api.element;

import java.util.function.Consumer;

public interface ModbusReadElement<T> extends ModbusElement<T> {

	public AbstractModbusElement<T> onUpdateCallback(Consumer<T> onUpdateCallback);

}
