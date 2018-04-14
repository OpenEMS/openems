package io.openems.edge.bridge.modbus.api.element;

import java.nio.ByteOrder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.procimg.InputRegister;

import io.openems.common.exceptions.OpenemsException;

public abstract class AbstractModbusRegisterElement<T> extends AbstractModbusElement<T> {

	private final Logger log = LoggerFactory.getLogger(AbstractModbusRegisterElement.class);

	public AbstractModbusRegisterElement(int startAddress) {
		super(startAddress);
	}

	public AbstractModbusRegisterElement<T> byteOrder(ByteOrder byteOrder) {
		this.byteOrder = byteOrder;
		return this;
	}

	/*
	 * ByteOrder of the input registers
	 */
	private ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;

	public ByteOrder getByteOrder() {
		return byteOrder;
	}

	@Override
	public void setInputRegisters(InputRegister... registers) throws OpenemsException {
		if (this.isDebug()) {
			StringBuilder b = new StringBuilder("Element at [" + this.getStartAddress() + "/0x"
					+ Integer.toHexString(this.getStartAddress()) + "] set input registers to [");
			for (int i = 0; i < registers.length; i++) {
				b.append(registers[i].getValue());
				if (i < registers.length - 1) {
					b.append(",");
				}
			}
			b.append("].");
			log.info(b.toString());
		}
		if (registers.length != this.getLength()) {
			throw new OpenemsException("Modbus address [" + this.getStartAddress() + "]: registers length ["
					+ registers.length + "] does not match required size of [" + this.getLength() + "]");
		}
		this._setInputRegisters(registers);
	}

	protected abstract void _setInputRegisters(InputRegister... registers);
}
