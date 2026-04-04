package io.openems.edge.bridge.modbus.tester;

/**
 * Modbus function codes for reading registers. Currently only FC3 is
 * supported; add further types here when needed.
 */
public enum RegisterType {

	/** Function Code 3 — Read Holding Registers. */
	FC3_READ_HOLDING_REGISTERS;

}
