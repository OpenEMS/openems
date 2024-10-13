package io.openems.edge.common.modbusslave;

import java.nio.charset.StandardCharsets;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.type.TypeUtils;

public class ModbusRecordString16 extends ModbusRecordConstant {

	public static final byte[] UNDEFINED_VALUE = new byte[32];

	public static final int BYTE_LENGTH = 32;

	private final String value;

	public ModbusRecordString16(int offset, String name, String value) {
		super(offset, name, ModbusType.STRING16, toByteArray(value));
		this.value = value;
	}

	@Override
	public String toString() {
		return this.generateToString("ModbusRecordString16", this.value);
	}

	/**
	 * Convert to byte array.
	 * 
	 * @param value the value
	 * @return the byte array
	 */
	public static byte[] toByteArray(String value) {
		if (value == null) {
			return UNDEFINED_VALUE;
		}
		var result = new byte[BYTE_LENGTH];
		var converted = value.getBytes(StandardCharsets.US_ASCII);
		System.arraycopy(converted, 0, result, 0, Math.min(BYTE_LENGTH, converted.length));
		return result;
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
		return toByteArray((String) TypeUtils.getAsType(OpenemsType.STRING, value));
	}

	@Override
	public String getValueDescription() {
		return this.value != null ? "\"" + this.value.toString() + "\"" : "";
	}

}
