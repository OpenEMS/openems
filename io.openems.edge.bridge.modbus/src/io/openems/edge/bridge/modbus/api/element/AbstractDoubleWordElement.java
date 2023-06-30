package io.openems.edge.bridge.modbus.api.element;

import java.nio.ByteBuffer;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;

/**
 * A DoubleWordElement has a size of two Modbus Registers or 32 bit.
 *
 * @param <E> the subclass of myself
 * @param <T> the target OpenemsType
 */
public abstract class AbstractDoubleWordElement<E, T> extends AbstractModbusRegisterElement<E, T> {

	private final Logger log = LoggerFactory.getLogger(AbstractDoubleWordElement.class);

	public AbstractDoubleWordElement(OpenemsType type, int startAddress) {
		super(type, startAddress);
	}

	/**
	 * Gets an instance of the correct subclass of myself.
	 *
	 * @return myself
	 */
	@Override
	protected abstract E self();

	@Override
	public final int getLength() {
		return 2;
	}

	@Override
	protected final void _setInputRegisters(InputRegister... registers) {
		// fill buffer
		var buff = ByteBuffer.allocate(4).order(this.getByteOrder());
		if (this.wordOrder == WordOrder.MSWLSW) {
			buff.put(registers[0].toBytes());
			buff.put(registers[1].toBytes());
		} else {
			buff.put(registers[1].toBytes());
			buff.put(registers[0].toBytes());
		}
		buff.rewind();
		// convert registers to Long
		var value = this.fromByteBuffer(buff);
		// set value
		super.setValue(value);
	}

	/**
	 * Converts a 4-byte ByteBuffer to the the current OpenemsType.
	 *
	 * @param buff the ByteBuffer
	 * @return an instance of the given OpenemsType
	 */
	protected abstract T fromByteBuffer(ByteBuffer buff);

	@Override
	public final void _setNextWriteValue(Optional<T> valueOpt) throws OpenemsException {
		if (valueOpt.isPresent()) {
			if (this.isDebug()) {
				this.log.info("Element [" + this + "] set next write value to [" + valueOpt.orElse(null) + "].");
			}
			var buff = ByteBuffer.allocate(4).order(this.getByteOrder());
			buff = this.toByteBuffer(buff, valueOpt.get());
			var b = buff.array();
			if (this.wordOrder == WordOrder.MSWLSW) {
				this.setNextWriteValueRegisters(Optional.of(new Register[] { //
						new SimpleRegister(b[0], b[1]), new SimpleRegister(b[2], b[3]) }));
			} else {
				this.setNextWriteValueRegisters(Optional.of(new Register[] { //
						new SimpleRegister(b[2], b[3]), new SimpleRegister(b[0], b[1]) }));
			}
		} else {
			this.setNextWriteValueRegisters(Optional.empty());
		}
		this.onSetNextWriteCallbacks.forEach(callback -> callback.accept(valueOpt));
	}

	/**
	 * Converts the current OpenemsType to a 4-byte ByteBuffer.
	 *
	 * @param buff  the target ByteBuffer
	 * @param value an instance of the given OpenemsType
	 * @return the ByteBuffer
	 */
	protected abstract ByteBuffer toByteBuffer(ByteBuffer buff, T value);

	/**
	 * Sets the Word-Order. Default is "MSWLSW" - "Most Significant Word; Least
	 * Significant Word". See http://www.simplymodbus.ca/FAQ.htm#Order.
	 *
	 * @param wordOrder the new Word-Order
	 * @return myself
	 */
	public final E wordOrder(WordOrder wordOrder) {
		this.wordOrder = wordOrder;
		return this.self();
	}

	private WordOrder wordOrder = WordOrder.MSWLSW;

}
