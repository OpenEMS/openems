package io.openems.edge.bridge.modbus.api.element;

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
public abstract class AbstractMultipleWordsElement<SELF extends ModbusElement<SELF, InputRegister[], T>, T>
		extends ModbusRegisterElement<SELF, T> {

	private final Logger log = LoggerFactory.getLogger(AbstractMultipleWordsElement.class);

	private WordOrder wordOrder = WordOrder.MSWLSW;

	protected AbstractMultipleWordsElement(OpenemsType type, int startAddress, int length) {
		super(type, startAddress, length);
	}

	/**
	 * Sets the Word-Order. Default is "MWSLSW" - "Most Significant Word; Least
	 * Significant Word". See http://www.simplymodbus.ca/FAQ.htm#Order.
	 *
	 * @param wordOrder the WordOrder
	 * @return myself
	 */
	public final SELF wordOrder(WordOrder wordOrder) {
		this.wordOrder = wordOrder;
		return this.self();
	}

	public final WordOrder getWordOrder() {
		return this.wordOrder;
	}

//	@Override
//	public void setInput(InputRegister[] registers) {
//		// TODO check length
//	}

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

}
