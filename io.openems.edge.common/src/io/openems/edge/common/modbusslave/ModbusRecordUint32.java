package io.openems.edge.common.modbusslave;

import io.openems.common.types.OpenemsType;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.type.TypeUtils;

public class ModbusRecordUint32 extends ModbusRecordConstant {

	public static final long UNDEFINED_VALUE = 4294967295L;
	public static final byte[] UNDEFINED_BYTE_ARRAY = toByteArray(UNDEFINED_VALUE);
	public static final int BYTE_LENGTH = 4;

	protected final Long value;

	public ModbusRecordUint32(int offset, String name, Long value) {
		super(offset, name, ModbusType.UINT32, toByteArray(value));
		this.value = value;
	}

	@Override
	public String toString() {
		return generateToString("ModbusRecordUInt32", this.value, Long::toHexString);
	}

	/**
	 * Convert to byte array.
	 * 
	 * @param value the value
	 * @return the byte array
	 */
	public static byte[] toByteArray(long value) {
		return new byte[] { //
				(byte) (value >>> 24), //
				(byte) (value >>> 16), //
				(byte) (value >>> 8), //
				(byte) (value) //
		};
	}

	/**
	 * Convert to byte array.
	 * 
	 * @param value the value
	 * @return the byte array
	 */
	public static byte[] toByteArray(Object value) {
		if (value == null || (value instanceof OptionsEnum oe && oe.isUndefined())) {
			return UNDEFINED_BYTE_ARRAY;
		}
		return toByteArray((long) TypeUtils.getAsType(OpenemsType.LONG, value));
	}

	@Override
	public String getValueDescription() {
		return this.value != null ? "\"" + Long.toString(this.value) + "\"" : "";
	}

}
