package io.openems.edge.bridge.modbus.api;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface BridgeModbusSerial extends BridgeModbus {

	/**
	 * Gets the Port-Name (e.g. '/dev/ttyUSB0' or 'COM3').
	 * 
	 * @return the Port-Name
	 */
	public String getPortName();

	/**
	 * Gets the Baudrate (e.g. 9600).
	 * 
	 * @return the Baudrate
	 */
	public int getBaudrate();

	/**
	 * Gets the Databits (e.g. 8).
	 * 
	 * @return the Databits
	 */
	public int getDatabits();

	/**
	 * Gets the Stopbits.
	 * 
	 * @return the Stopbits
	 */
	public Stopbit getStopbits();

	/**
	 * Gets the Parity.
	 * 
	 * @return the Parity.
	 */
	public Parity getParity();

}
