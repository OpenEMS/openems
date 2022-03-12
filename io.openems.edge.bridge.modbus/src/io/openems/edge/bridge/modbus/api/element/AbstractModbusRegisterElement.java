package io.openems.edge.bridge.modbus.api.element;

import java.nio.ByteOrder;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.procimg.Register;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;

/**
 * A ModbusRegisterElement represents one or more Modbus Registers.
 *
 * @param <E> the subclass of myself
 * @param <T> the target OpenemsType
 */
public abstract class AbstractModbusRegisterElement<E, T> extends AbstractModbusElement<T>
		implements ModbusRegisterElement<T> {

	private final Logger log = LoggerFactory.getLogger(AbstractModbusRegisterElement.class);

	private Optional<Register[]> nextWriteValue = Optional.empty();

	public AbstractModbusRegisterElement(OpenemsType type, int startAddress) {
		super(type, startAddress);
	}

	/**
	 * Gets an instance of the correct subclass of myself.
	 *
	 * @return myself
	 */
	protected abstract E self();

	protected void setNextWriteValueRegisters(Optional<Register[]> writeValueOpt) throws OpenemsException {
		if (writeValueOpt.isPresent() && writeValueOpt.get().length != this.getLength()) {
			throw new OpenemsException("Modbus Element [" + this + "]: write registers length ["
					+ writeValueOpt.get().length + "] does not match required size of [" + this.getLength() + "]");
		}
		this.nextWriteValue = writeValueOpt;
	}

	@Override
	public Optional<Register[]> getNextWriteValue() {
		return this.nextWriteValue;
	}

	/*
	 * ByteOrder of the input registers
	 */
	private ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;

	/**
	 * Sets the Byte-Order. Default is "BIG_ENDIAN". See
	 * http://www.simplymodbus.ca/FAQ.htm#Order.
	 *
	 * @param byteOrder the ByteOrder
	 * @return myself
	 */
	public final E byteOrder(ByteOrder byteOrder) {
		this.byteOrder = byteOrder;
		return this.self();
	}

	public ByteOrder getByteOrder() {
		return this.byteOrder;
	}

	@Override
	public void setInputRegisters(InputRegister... registers) throws OpenemsException {
		if (this.isDebug()) {
			var b = new StringBuilder("Element [" + this + "] set input registers to [");
			for (var i = 0; i < registers.length; i++) {
				b.append(registers[i].getValue());
				if (i < registers.length - 1) {
					b.append(",");
				}
			}
			b.append("].");
			this.log.info(b.toString());
		}
		if (registers.length != this.getLength()) {
			throw new OpenemsException("Modbus Element [" + this + "]: registers length [" + registers.length
					+ "] does not match required size of [" + this.getLength() + "]");
		}
		this._setInputRegisters(registers);
	}

	protected abstract void _setInputRegisters(InputRegister... registers);

}
