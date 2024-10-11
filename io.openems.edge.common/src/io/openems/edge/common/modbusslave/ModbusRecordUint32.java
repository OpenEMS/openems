package io.openems.edge.common.modbusslave;

import java.nio.ByteBuffer;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.type.TypeUtils;

public class ModbusRecordUint32 extends ModbusRecordConstant {

	public static final byte[] UNDEFINED_VALUE = { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };

	public static final int BYTE_LENGTH = 4;

	protected final Integer value;

	public ModbusRecordUint32(int offset, String name, Integer value) {
		super(offset, name, ModbusType.UINT32, toByteArray(value));
		this.value = value;
	}

	@Override
	public String toString() {
		return generateToString("ModbusRecordUInt32", this.value, Integer::toHexString);
	}

	/**
	 * Convert to byte array.
	 * 
	 * @param value the value
	 * @return the byte array
	 */
	public static byte[] toByteArray(int value) {
		return ByteBuffer.allocate(BYTE_LENGTH).putInt(value).array();
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
		return toByteArray((int) TypeUtils.getAsType(OpenemsType.INTEGER, value));
	}

	@Override
	public String getValueDescription() {
		return this.value != null ? "\"" + Integer.toString(this.value) + "\"" : "";
	}

}
