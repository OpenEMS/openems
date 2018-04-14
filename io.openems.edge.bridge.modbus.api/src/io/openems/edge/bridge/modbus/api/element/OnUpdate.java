package io.openems.edge.bridge.modbus.api.element;

public interface OnUpdate<T> {

	public void call(T value);

}
