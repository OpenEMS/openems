package io.openems.edge.common.modbusslave;

import java.nio.ByteBuffer;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.type.TypeUtils;

public class ModbusRecordFloat64 extends ModbusRecordConstant {

	public static final byte[] UNDEFINED_VALUE = { (byte) 0x7F, (byte) 0xF8, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00 };

	public static final int BYTE_LENGTH = 8;

	private final Double value;

	public ModbusRecordFloat64(int offset, String name, Double value) {
		super(offset, name, ModbusType.FLOAT64, toByteArray(value));
		this.value = value;
	}

	@Override
	public String toString() {
		return "ModbusRecordFloat64 [value=" + this.value + ", type=" + this.getType() + "]";
	}

	/**
	 * Convert to byte array.
	 * 
	 * @param value the value
	 * @return the byte array
	 */
	public static byte[] toByteArray(double value) {
		return ByteBuffer.allocate(BYTE_LENGTH).putDouble(value).array();
	}

	/**
	 * Convert to byte array.
	 * 
	 * @param value the value
	 * @return the byte array
	 */
	public static byte[] toByteArray(Object value) {
		if (value == null) {
			return UNDEFINED_VALUE;
		}
		return toByteArray((double) TypeUtils.getAsType(OpenemsType.DOUBLE, value));
	}

	@Override
	public String getValueDescription() {
		return this.value != null ? "\"" + this.value.toString() + "\"" : "";
	}

}
