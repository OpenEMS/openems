package io.openems.impl.protocol.modbus.internal;

import java.math.BigInteger;

import com.ghgande.j2mod.modbus.procimg.Register;

public interface DoublewordElement {
	/**
	 * Updates the value of this Element from two Registers.
	 *
	 * @param register
	 */
	public void setValue(Register register1, Register register2);

	/**
	 * Converts the given value to a Register[2]-Array, fitting with the hardware format of this Element. Use it to
	 * prepare a
	 * Modbus write.
	 *
	 * @param value
	 * @return
	 */
	public Register[] toRegister(BigInteger value);
}
