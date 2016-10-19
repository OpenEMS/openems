package io.openems.impl.protocol.modbus.internal;

import org.eclipse.jdt.annotation.NonNull;

import com.ghgande.j2mod.modbus.procimg.Register;

public interface WordElement {
	/**
	 * Updates the value of this Element from a Register.
	 *
	 * @param register
	 */
	public void setValue(@NonNull Register register);

	/**
	 * Converts the given value to a Register fitting with the hardware format of this Element. Use it to prepare a
	 * Modbus write.
	 *
	 * @param value
	 * @return
	 */
	public Register toRegister(@NonNull Long value);
}
