package io.openems.edge.bridge.modbus.api.element;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

import io.openems.common.types.OpenemsType;

/**
 * A ModbusRegisterElement represents one or more Modbus Registers.
 *
 * @param <SELF>   the subclass of myself
 * @param <BINARY> the binary type
 * @param <T>      the OpenEMS type
 */
public abstract class ModbusRegisterElement<SELF extends AbstractModbusElement<SELF, Register[], T>, T>
		extends AbstractModbusElement<SELF, Register[], T> {

	/** ByteOrder of the input registers. */
	private ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;

	protected ModbusRegisterElement(OpenemsType type, int startAddress, int length) {
		super(type, startAddress, length);
	}

	protected abstract T byteBufferToValue(ByteBuffer buff);

	protected abstract void valueToByteBuffer(ByteBuffer buff, T value);

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

	protected final ByteOrder getByteOrder() {
		return this.byteOrder;
	}

	private final ByteBuffer buildByteBuffer() {
		return ByteBuffer.allocate(this.length * 2).order(this.getByteOrder());
	}

	protected final Register[] valueToRaw(T value, WordOrder wordOrder) {
		var buff = this.buildByteBuffer();
		this.valueToByteBuffer(buff, value);
		var b = buff.array();
		var result = new Register[this.length];
		switch (wordOrder) {
		case LSWMSW:
			// Least significant word, most significant word
			for (int i = 0; i < this.length; i++) {
				result[i] = new SimpleRegister(b[i * 2], b[i * 2 + 1]);
			}
			break;

		case MSWLSW:
			// Most significant word, least significant word
			for (int i = 0; i < this.length; i++) {
				result[i] = new SimpleRegister(b[i * 2], b[i * 2 + 1]);
			}
			break;
		}
		return result;
	}

	protected final T rawToValue(Register[] registers, WordOrder wordOrder) {
		if (registers.length != this.length) {
			throw new IllegalArgumentException("Registers length does not match. " //
					+ "Expected [" + this.length + "] " //
					+ "Got [" + registers.length + "] " //
					+ "for " + this.toString());
		}
		// fill buffer
		var buff = this.buildByteBuffer();
		switch (wordOrder) {

		case LSWMSW:
			// Least significant word, most significant word
			for (int i = 0; i < this.length; i++) {
				buff.put(registers[i].toBytes());
			}
			break;

		case MSWLSW:
			// Most significant word, least significant word
			for (int i = this.length - 1; i >= 0; i--) {
				buff.put(registers[i].toBytes());
			}
			break;
		}
		buff.rewind();
		return this.byteBufferToValue(buff);
	}
}