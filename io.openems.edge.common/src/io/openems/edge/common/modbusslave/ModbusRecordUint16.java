package io.openems.edge.common.modbusslave;

import io.openems.common.types.OpenemsType;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.type.TypeUtils;

public class ModbusRecordUint16 extends ModbusRecordConstant {

	public static final int UNDEFINED_VALUE = 65535;
	public static final byte[] UNDEFINED_BYTE_ARRAY = toByteArray(UNDEFINED_VALUE);
	public static final int BYTE_LENGTH = 2;

	protected final Integer value;

	public ModbusRecordUint16(int offset, String name, Integer value) {
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
	public static byte[] toByteArray(int value) {
		return new byte[] { //
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
		return toByteArray((int) TypeUtils.getAsType(OpenemsType.INTEGER, value));
	}

	@Override
	public String getValueDescription() {
		return this.value != null ? "\"" + Integer.toString(this.value) + "\"" : "";
	}

}
