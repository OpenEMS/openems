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
 * A WordElement has a size of one Modbus Registers or 16 bit.
 *
 * @param <SELF> the subclass of myself
 * @param <T>    the target type
 */
public abstract class AbstractWordElement<SELF extends ModbusElement<SELF, T>, T>
		extends ModbusRegisterElement<SELF, T> {

	private final Logger log = LoggerFactory.getLogger(AbstractWordElement.class);

	public AbstractWordElement(OpenemsType type, int startAddress) {
		super(type, startAddress);
	}

	@Override
	public final int getLength() {
		return 1;
	}

	@Override
	protected void _setInputRegisters(InputRegister... registers) {
		// convert registers
		var buff = ByteBuffer.allocate(2).order(this.getByteOrder());
		buff.put(registers[0].toBytes());
		var value = this.fromByteBuffer(buff);
		// set value
		super.setValue(value);
	}

	/**
	 * Converts a 2-byte ByteBuffer to the the current OpenemsType.
	 *
	 * @param buff the ByteBuffer
	 * @return an instance of the current OpenemsType
	 */
	protected abstract T fromByteBuffer(ByteBuffer buff);

	@Override
	public void _setNextWriteValue(Optional<T> valueOpt) throws OpenemsException {
		if (this.isDebug()) {
			this.log.info("Element [" + this + "] set next write value to [" + valueOpt.orElse(null) + "].");
		}
		if (valueOpt.isPresent()) {
			var buff = ByteBuffer.allocate(2).order(this.getByteOrder());
			buff = this.toByteBuffer(buff, valueOpt.get());
			var b = buff.array();
			this.setNextWriteValueRegisters(Optional.of(new Register[] { //
					new SimpleRegister(b[0], b[1]) }));
		} else {
			this.setNextWriteValueRegisters(Optional.empty());
		}
		this.onSetNextWriteCallbacks.forEach(callback -> callback.accept(valueOpt));
	}

	/**
	 * Converts the current OpenemsType to a 2-byte ByteBuffer.
	 *
	 * @param buff  the target ByteBuffer
	 * @param value the value
	 * @return the ByteBuffer
	 */
	protected abstract ByteBuffer toByteBuffer(ByteBuffer buff, Object value);

}
