package io.openems.edge.common.modbusslave;

import java.nio.ByteBuffer;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.type.TypeUtils;

public class ModbusRecordUint64 extends ModbusRecordConstant {

	public static final byte[] UNDEFINED_VALUE = { //
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, //
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };

	public static final int BYTE_LENGTH = 8;

	protected final Long value;

	public ModbusRecordUint64(int offset, String name, Long value) {
		super(offset, name, ModbusType.UINT64, toByteArray(value));
		this.value = value;
	}

	@Override
	public String toString() {
		return this.generateToString("ModbusRecordUInt64", this.value, Long::toHexString);
	}

	/**
	 * Convert to byte array.
	 * 
	 * @param value the value
	 * @return the byte array
	 */
	public static byte[] toByteArray(long value) {
		return ByteBuffer.allocate(BYTE_LENGTH).putLong(value).array();
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
		return toByteArray((long) TypeUtils.getAsType(OpenemsType.LONG, value));
	}

	@Override
	public String getValueDescription() {
		return this.value != null //
				? "\"" + Long.toString(this.value) + "\"" //
				: "";
	}

}
