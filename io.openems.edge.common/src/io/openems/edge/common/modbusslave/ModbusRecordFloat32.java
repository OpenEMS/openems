package io.openems.edge.common.modbusslave;

import java.nio.ByteBuffer;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.type.TypeUtils;

public class ModbusRecordFloat32 extends ModbusRecordConstant {

	public static final float UNDEFINED_VALUE = Float.NaN;
	public static final byte[] UNDEFINED_BYTE_ARRAY = toByteArray(UNDEFINED_VALUE);
	public static final int BYTE_LENGTH = 4;

	private final Float value;

	public ModbusRecordFloat32(int offset, String name, Float value) {
		super(offset, name, ModbusType.FLOAT32, toByteArray(value));
		this.value = value;
	}

	@Override
	public String toString() {
		return this.generateToString("ModbusRecordFloat32", this.value);
	}

	/**
	 * Convert to byte array.
	 * 
	 * @param value the value
	 * @return the byte array
	 */
	public static byte[] toByteArray(float value) {
		return ByteBuffer.allocate(BYTE_LENGTH).putFloat(value).array();
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
		return toByteArray((float) TypeUtils.getAsType(OpenemsType.FLOAT, value));
	}

	@Override
	public String getValueDescription() {
		return this.value != null ? "\"" + this.value.toString() + "\"" : "";
	}

}
