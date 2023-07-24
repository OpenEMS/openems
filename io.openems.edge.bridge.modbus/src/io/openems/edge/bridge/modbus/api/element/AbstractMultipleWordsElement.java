package io.openems.edge.bridge.modbus.api.element;

import com.ghgande.j2mod.modbus.procimg.Register;

import io.openems.common.types.OpenemsType;

/**
 * A WordElement has a size of one Modbus Registers or 16 bit.
 *
 * @param <SELF> the subclass of myself
 * @param <T>    the OpenEMS type
 */
public abstract class AbstractMultipleWordsElement<SELF extends ModbusElement<SELF, Register[], T>, T>
		extends ModbusRegisterElement<SELF, T> {

	private WordOrder wordOrder = WordOrder.MSWLSW;

	protected AbstractMultipleWordsElement(OpenemsType type, int startAddress, int length) {
		super(type, startAddress, length);
	}

	@Override
	protected final T rawToValue(Register[] registers) {
		return this.rawToValue(registers, this.wordOrder);
	}

	@Override
	protected Register[] valueToRaw(T value) {
		return this.valueToRaw(value, this.wordOrder);
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

	protected WordOrder getWordOrder() {
		return this.wordOrder;
	}

}
