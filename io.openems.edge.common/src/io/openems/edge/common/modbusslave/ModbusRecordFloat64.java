package io.openems.edge.common.modbusslave;

import java.nio.ByteBuffer;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.type.TypeUtils;

public class ModbusRecordFloat64 extends ModbusRecordConstant {

	public static final double UNDEFINED_VALUE = Double.NaN;
	public static final byte[] UNDEFINED_BYTE_ARRAY = toByteArray(UNDEFINED_VALUE);
	public static final int BYTE_LENGTH = 8;

	private final Double value;

	public ModbusRecordFloat64(int offset, String name, Double value) {
		super(offset, name, ModbusType.FLOAT64, toByteArray(value));
		this.value = value;
	}

	@Override
	public String toString() {
		return this.generateToString("ModbusRecordFloat64", this.value);
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
			return UNDEFINED_BYTE_ARRAY;
		}
		return toByteArray((double) TypeUtils.getAsType(OpenemsType.DOUBLE, value));
	}

	@Override
	public String getValueDescription() {
		return this.value != null ? "\"" + this.value.toString() + "\"" : "";
	}

}
