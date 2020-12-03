package io.openems.edge.common.modbusslave;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.type.TypeUtils;

public class ModbusRecordString16 extends AbstractModbusRecordSupplier {

	public final static byte[] UNDEFINED_VALUE = new byte[32];

	public final static int BYTE_LENGTH = 32;

	private final String value;

	public ModbusRecordString16(int offset, String name, String value) {
		super(offset, name, ModbusType.STRING16, toByteArray(value));
		this.value = value;
	}

	public ModbusRecordString16(int offset, String name, Supplier<String> valueSupplier) {
		super(offset, name, ModbusType.STRING16, () -> {
			return toByteArray(valueSupplier.get());
		});
		this.value = null;
	}

	@Override
	public String toString() {
		return "ModbusRecordString16 [value=" + this.value + ", type=" + getType() + "]";
	}

	/**
	 * Converts a String value to a byte-array with length 32.
	 * 
	 * @param value the String value
	 * @return the byte-array
	 */
	public static byte[] toByteArray(Object value) {
		if (value == null) {
			return UNDEFINED_VALUE;
		} else {
			String stringValue = (String) TypeUtils.getAsType(OpenemsType.STRING, value);
			if (stringValue == null) {
				return UNDEFINED_VALUE;
			} else {
				byte[] result = new byte[BYTE_LENGTH];
				byte[] converted = stringValue.getBytes(StandardCharsets.US_ASCII);
				System.arraycopy(converted, 0, result, 0, Math.min(BYTE_LENGTH, converted.length));
				return result;
			}
		}
	}

	@Override
	public String getValueDescription() {
		return this.value != null ? this.value : "";
	}

}
