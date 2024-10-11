package io.openems.edge.common.modbusslave;

import java.nio.ByteBuffer;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.type.TypeUtils;

public class ModbusRecordUint16 extends ModbusRecordConstant {

	public static final byte[] UNDEFINED_VALUE = { (byte) 0xFF, (byte) 0xFF };

	public static final int BYTE_LENGTH = 2;

	protected final Short value;

	public ModbusRecordUint16(int offset, String name, Short value) {
		super(offset, name, ModbusType.UINT16, toByteArray(value));
		this.value = value;
	}

	@Override
	public String toString() {
		return generateToString("ModbusRecordUInt16", this.value, v -> Integer.toHexString(v));
	}

	/**
	 * Convert to byte array.
	 * 
	 * @param value the value
	 * @return the byte array
	 */
	public static byte[] toByteArray(short value) {
		return ByteBuffer.allocate(BYTE_LENGTH).putShort(value).array();
	}

	/**
	 * Convert to byte array.
	 * 
	 * @param value the value
	 * @return the byte array
	 */
	public static byte[] toByteArray(Object value) {
		if (value == null || value instanceof io.openems.common.types.OptionsEnum
				&& ((io.openems.common.types.OptionsEnum) value).isUndefined()) {
			return UNDEFINED_VALUE;
		}
		return toByteArray((short) TypeUtils.getAsType(OpenemsType.SHORT, value));
	}

	@Override
	public String getValueDescription() {
		return this.value != null ? "\"" + Short.toString(this.value) + "\"" : "";
	}

}
