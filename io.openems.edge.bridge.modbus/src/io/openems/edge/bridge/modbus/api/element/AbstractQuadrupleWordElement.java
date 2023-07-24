package io.openems.edge.bridge.modbus.api.element;

import com.ghgande.j2mod.modbus.procimg.Register;

import io.openems.common.types.OpenemsType;

/**
 * A QuadrupleWordElement has a size of four Modbus Registers or 64 bit.
 *
 * @param <SELF> the subclass of myself
 * @param <T>    the OpenEMS type
 */
public abstract class AbstractQuadrupleWordElement<SELF extends AbstractModbusElement<SELF, Register[], T>, T>
		extends AbstractMultipleWordsElement<SELF, T> {

	public AbstractQuadrupleWordElement(OpenemsType type, int startAddress) {
		super(type, startAddress, 4);
	}

}