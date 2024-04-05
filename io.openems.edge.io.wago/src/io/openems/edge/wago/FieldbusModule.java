package io.openems.edge.wago;

import io.openems.edge.bridge.modbus.api.element.CoilElement;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.Channel;

public abstract class FieldbusModule {

	/**
	 * Gets the Name of the {@link FieldbusModule}.
	 *
	 * @return the name
	 */
	public abstract String getName();

	/**
	 * Gets the {@link ModbusCoilElement} for the input coils starting at address 0.
	 *
	 * @return the array; empty for no input coils
	 */
	public abstract CoilElement[] getInputCoil0Elements();

	/**
	 * Gets the {@link ModbusCoilElement} for the input coils starting at address
	 * 512.
	 *
	 * @return the array; empty for no input coils
	 */
	public abstract CoilElement[] getInputCoil512Elements();

	/**
	 * Gets the {@link ModbusCoilElement} for the output coils starting at address
	 * 512.
	 *
	 * @return the array; empty for no output coils
	 */
	public abstract CoilElement[] getOutputCoil512Elements();

	/**
	 * Gets the Channels of the {@link FieldbusModule}.
	 *
	 * @return the {@link Channel}
	 */
	public abstract BooleanReadChannel[] getChannels();

}
