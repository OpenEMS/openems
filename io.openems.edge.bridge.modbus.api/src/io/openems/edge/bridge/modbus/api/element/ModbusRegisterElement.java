package io.openems.edge.bridge.modbus.api.element;

import com.ghgande.j2mod.modbus.procimg.InputRegister;

import io.openems.common.exceptions.OpenemsException;

public interface ModbusRegisterElement extends ModbusElement {

	/**
	 * Sets the value of this Element from InputRegisters
	 * 
	 * @param registers
	 * @throws OpenemsException
	 */
	public void setInputRegisters(InputRegister... registers) throws OpenemsException;
}
