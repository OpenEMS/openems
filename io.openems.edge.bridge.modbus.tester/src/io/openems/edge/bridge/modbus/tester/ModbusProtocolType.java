package io.openems.edge.bridge.modbus.tester;

/**
 * The Modbus transport protocol type. Required for reconstructing the full
 * Modbus frame in the {@code Message} channel.
 */
public enum ModbusProtocolType {

	/** Modbus/TCP (MBAP header). */
	TCP,

	/** Modbus/RTU (binary with CRC-16). */
	RTU,

	/** Modbus/ASCII (colon-delimited hex with LRC). */
	ASCII;

}
