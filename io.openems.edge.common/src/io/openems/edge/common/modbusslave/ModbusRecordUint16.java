package io.openems.edge.common.modbusslave;

import java.nio.ByteBuffer;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.type.TypeUtils;

public class ModbusRecordUint16 extends ModbusRecordConstant {

	public final static byte[] UNDEFINED_VALUE = { (byte) 0xFF, (byte) 0xFF };

	public final static int BYTE_LENGTH = 2;

	protected final Short value;

	public ModbusRecordUint16(int offset, String name, Short value) {
		super(offset, name, ModbusType.UINT16, toByteArray(value));
		this.value = value;
	}

	@Override
	public String toString() {
		return "ModbusRecordUInt16 [value=" + this.value + "/0x" + Integer.toHexString(this.value) + ", type="
				+ this.getType() + "]";
	}

	public static byte[] toByteArray(short value) {
		return ByteBuffer.allocate(BYTE_LENGTH).putShort(value).array();
	}

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
