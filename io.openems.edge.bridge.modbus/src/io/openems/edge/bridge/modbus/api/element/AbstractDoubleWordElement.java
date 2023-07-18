package io.openems.edge.bridge.modbus.api.element;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.procimg.InputRegister;

import io.openems.common.types.OpenemsType;

/**
 * A DoubleWordElement has a size of two Modbus Registers or 32 bit.
 *
 * @param <SELF> the subclass of myself
 * @param <T>    the OpenEMS type
 */
public abstract class AbstractDoubleWordElement<SELF extends ModbusElement<SELF, InputRegister[], T>, T>
		extends AbstractMultipleWordsElement<SELF, T> {

	private final Logger log = LoggerFactory.getLogger(AbstractDoubleWordElement.class);

	public AbstractDoubleWordElement(OpenemsType type, int startAddress) {
		super(type, startAddress, 2);
	}

//	@Override
//	public final void _setNextWriteValue(Optional<T> valueOpt) throws OpenemsException {
//		if (valueOpt.isPresent()) {
//			if (this.isDebug()) {
//				this.log.info("Element [" + this + "] set next write value to [" + valueOpt.orElse(null) + "].");
//			}
//			var buff = ByteBuffer.allocate(4).order(this.getByteOrder());
//			buff = this.toByteBuffer(buff, valueOpt.get());
//			var b = buff.array();
//			if (this.wordOrder == WordOrder.MSWLSW) {
//				this.setNextWriteValueRegisters(Optional.of(new Register[] { //
//						new SimpleRegister(b[0], b[1]), new SimpleRegister(b[2], b[3]) }));
//			} else {
//				this.setNextWriteValueRegisters(Optional.of(new Register[] { //
//						new SimpleRegister(b[2], b[3]), new SimpleRegister(b[0], b[1]) }));
//			}
//		} else {
//			this.setNextWriteValueRegisters(Optional.empty());
//		}
//		this.onSetNextWriteCallbacks.forEach(callback -> callback.accept(valueOpt));
//	}

	/**
	 * Converts the current OpenemsType to a 4-byte ByteBuffer.
	 *
	 * @param buff  the target ByteBuffer
	 * @param value an instance of the given OpenemsType
	 * @return the ByteBuffer
	 */
	protected abstract ByteBuffer toByteBuffer(ByteBuffer buff, T value);
}
