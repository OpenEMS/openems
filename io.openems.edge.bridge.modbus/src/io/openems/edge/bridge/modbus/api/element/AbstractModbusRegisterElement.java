package io.openems.edge.bridge.modbus.api.element;

import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.procimg.Register;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteOrder;
import java.util.Optional;

public abstract class AbstractModbusRegisterElement<T> extends AbstractModbusElement<T>
		implements ModbusRegisterElement<T> {

	private final Logger log = LoggerFactory.getLogger(AbstractModbusRegisterElement.class);

	private Optional<Register[]> nextWriteValue = Optional.empty();

	public AbstractModbusRegisterElement(OpenemsType type, int startAddress) {
		super(type, startAddress);
	}

	public AbstractModbusRegisterElement<T> byteOrder(ByteOrder byteOrder) {
		this.byteOrder = byteOrder;
		return this;
	}

	protected void setNextWriteValueRegisters(Optional<Register[]> writeValueOpt) throws OpenemsException {
		if (writeValueOpt.isPresent() && writeValueOpt.get().length != this.getLength()) {
			throw new OpenemsException("Modbus Element [" + this + "]: write registers length ["
					+ writeValueOpt.get().length + "] does not match required size of [" + this.getLength() + "]");
		}
		this.nextWriteValue = writeValueOpt;
	}

	public Optional<Register[]> getNextWriteValue() {
		return this.nextWriteValue;
	}

	/*
	 * ByteOrder of the input registers
	 */
	protected ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;

	public ByteOrder getByteOrder() {
		return byteOrder;
	}

	@Override
	public void setInputRegisters(InputRegister... registers) throws OpenemsException {
		if (this.isDebug()) {
			StringBuilder b = new StringBuilder("Element [" + this + "] set input registers to [");
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
			throw new OpenemsException("Modbus Element [" + this + "]: registers length [" + registers.length
					+ "] does not match required size of [" + this.getLength() + "]");
		}
		this._setInputRegisters(registers);
	}

	protected abstract void _setInputRegisters(InputRegister... registers);

}
