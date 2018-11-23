package io.openems.edge.bridge.modbus.api;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface BridgeModbusSerial extends BridgeModbus {

	/**
	 * Gets the Port-Name (e.g. '/dev/ttyUSB0' or 'COM3')
	 * 
	 * @return
	 */
	public String getPortName();

	/**
	 * Gets the Baudrate (e.g. 9600)
	 * 
	 * @return
	 */
	public int getBaudrate();

	/**
	 * Gets the Databits (e.g. 8)
	 * 
	 * @return
	 */
	public int getDatabits();

	/**
	 * Gets the Stopbits
	 * 
	 * @return
	 */
	public Stopbit getStopbits();

	/**
	 * Gets the parity
	 * 
	 * @return
	 */
	public Parity getParity();

}
