package io.openems.edge.bridge.modbus.api.element;

import com.ghgande.j2mod.modbus.procimg.Register;

import io.openems.common.types.OpenemsType;

/**
 * A DoubleWordElement has a size of two Modbus Registers or 32 bit.
 *
 * @param <SELF> the subclass of myself
 * @param <T>    the OpenEMS type
 */
public abstract class AbstractDoubleWordElement<SELF extends ModbusElement<SELF, Register[], T>, T>
		extends AbstractMultipleWordsElement<SELF, T> {

	public AbstractDoubleWordElement(OpenemsType type, int startAddress) {
		super(type, startAddress, 2);
	}

}