package io.openems.edge.bridge.modbus.api.element;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.procimg.InputRegister;

import io.openems.common.types.OpenemsType;

/**
 * A WordElement has a size of one Modbus Registers or 16 bit.
 *
 * @param <SELF> the subclass of myself
 * @param <T>    the OpenEMS type
 */
public abstract class AbstractSingleWordElement<SELF extends ModbusRegisterElement<SELF, T>, T>
		extends ModbusRegisterElement<SELF, T> {

	private final Logger log = LoggerFactory.getLogger(AbstractSingleWordElement.class);

	public AbstractSingleWordElement(OpenemsType type, int startAddress) {
		super(type, startAddress, 1);
	}

	@Override
	public final void setInput(InputRegister[] registers) {
		// TODO check length
		this.setInput(registers[0]);
	}

	protected void setInput(InputRegister register) {
		// convert register
		var buff = ByteBuffer.allocate(2).order(this.getByteOrder());
		buff.put(register.toBytes());
		var value = this.fromByteBuffer(buff);
		// set value
		super.setValue(value);
	}

	/**
	 * Converts a 2-byte ByteBuffer to the the target OpenemsType.
	 *
	 * @param buff the ByteBuffer
	 * @return an instance of the current OpenemsType
	 */
	protected abstract T fromByteBuffer(ByteBuffer buff);

//	@Override
//	public void _setNextWriteValue(Optional<T> valueOpt) throws OpenemsException {
//		if (this.isDebug()) {
//			this.log.info("Element [" + this + "] set next write value to [" + valueOpt.orElse(null) + "].");
//		}
//		if (valueOpt.isPresent()) {
//			var buff = ByteBuffer.allocate(2).order(this.getByteOrder());
//			buff = this.toByteBuffer(buff, valueOpt.get());
//			var b = buff.array();
//			this.setNextWriteValueRegisters(Optional.of(new Register[] { //
//					new SimpleRegister(b[0], b[1]) }));
//		} else {
//			this.setNextWriteValueRegisters(Optional.empty());
//		}
//		this.onSetNextWriteCallbacks.forEach(callback -> callback.accept(valueOpt));
//	}

	/**
	 * Converts the current OpenemsType to a 2-byte ByteBuffer.
	 *
	 * @param buff  the target ByteBuffer
	 * @param value the value
	 * @return the ByteBuffer
	 */
	protected abstract ByteBuffer toByteBuffer(ByteBuffer buff, Object value);

}
