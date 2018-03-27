package io.openems.edge.bridge.modbus.protocol;

public interface OnUpdate<T> {

	public void call(T value);

}
