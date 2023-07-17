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
 * @param <SELF>   the subclass of myself
 * @param <BINARY> the binary type
 * @param <T>      the OpenEMS type
 */
public abstract class ModbusRegisterElement<SELF extends ModbusElement<SELF, InputRegister[], T>, T>
		extends ModbusElement<SELF, InputRegister[], T> {

	private final Logger log = LoggerFactory.getLogger(ModbusRegisterElement.class);

	/** ByteOrder of the input registers. */
	private ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;

	protected ModbusRegisterElement(OpenemsType type, int startAddress, int length) {
		super(type, startAddress, length);
	}

//	/**
//	 * Sets the value of this Element from InputRegisters.
//	 *
//	 * @param registers the InputRegisters
//	 * @throws OpenemsException on error
//	 */
//	// TODO final?
//	public void setInputRegisters(InputRegister... registers) throws OpenemsException {
//		if (this.isDebug()) {
//			var b = new StringBuilder("Element [" + this + "] set input registers to [");
//			for (var i = 0; i < registers.length; i++) {
//				b.append(registers[i].getValue());
//				if (i < registers.length - 1) {
//					b.append(",");
//				}
//			}
//			b.append("].");
//			this.log.info(b.toString());
//		}
//		if (registers.length != this.length) {
//			throw new OpenemsException("Modbus Element [" + this + "]: registers length [" + registers.length
//					+ "] does not match required size of [" + this.length + "]");
//		}
//		this._setInputRegisters(registers);
//	}

//	/**
//	 * Sets a value that should be written to the Modbus device.
//	 *
//	 * @param valueOpt the Optional value
//	 * @throws OpenemsException         on error
//	 * @throws IllegalArgumentException on error
//	 */
//	public final void setNextWriteValue(Optional<Object> valueOpt) throws OpenemsException, IllegalArgumentException {
//		if (valueOpt.isPresent()) {
//			this._setNextWriteValue(//
//					Optional.of(//
//							TypeUtils.<TARGET>getAsType(this.getType(), valueOpt.get())));
//		} else {
//			this._setNextWriteValue(Optional.empty());
//		}
//	}

	/**
	 * Gets the next write value and resets it.
	 *
	 * <p>
	 * This method should be called once in every cycle on the
	 * TOPIC_CYCLE_EXECUTE_WRITE event. It makes sure, that the nextWriteValue gets
	 * initialized in every Cycle. If registers need to be written again in every
	 * cycle, next setNextWriteValue()-method needs to called on every Cycle.
	 *
	 * @return the next value as an Optional array of Registers
	 */
	// TODO final?
	public Optional<Register[]> getNextWriteValueAndReset() {
		var valueOpt = this.getNextWriteValue();
//		try {
//			if (valueOpt.isPresent()) {
//				this._setNextWriteValue(Optional.empty());
//			}
//		} catch (OpenemsException e) {
//			// can be safely ignored
//		}
		return valueOpt;
	}

	private Optional<Register[]> nextWriteValue = Optional.empty();

	protected final void setNextWriteValueRegisters(Optional<Register[]> writeValueOpt) throws OpenemsException {
		if (writeValueOpt.isPresent() && writeValueOpt.get().length != this.length) {
			throw new OpenemsException("Modbus Element [" + this + "]: write registers length ["
					+ writeValueOpt.get().length + "] does not match required size of [" + this.length + "]");
		}
		this.nextWriteValue = writeValueOpt;
	}

	/**
	 * Gets the next write value.
	 *
	 * @return the next value as an Optional array of Registers
	 */
	// TODO final
	public Optional<Register[]> getNextWriteValue() {
		return this.nextWriteValue;
	}

	/**
	 * Sets the Byte-Order. Default is "BIG_ENDIAN". See
	 * http://www.simplymodbus.ca/FAQ.htm#Order.
	 *
	 * @param byteOrder the ByteOrder
	 * @return myself
	 */
	public final SELF byteOrder(ByteOrder byteOrder) {
		this.byteOrder = byteOrder;
		return this.self();
	}

	public final ByteOrder getByteOrder() {
		return this.byteOrder;
	}

//	protected abstract void _setInputRegisters(InputRegister... registers);

}
