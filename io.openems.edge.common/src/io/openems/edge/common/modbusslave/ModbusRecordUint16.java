package io.openems.edge.common.modbusslave;

import java.nio.ByteBuffer;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.type.TypeUtils;

public class ModbusRecordUint16 extends ModbusRecordConstant {

	public final static byte[] UNDEFINED_VALUE = new byte[] { (byte) 0xFF, (byte) 0xFF };

	public final static int BYTE_LENGTH = 2;

	private final Short value;

	public ModbusRecordUint16(int offset, Short value) {
		super(offset, ModbusType.UINT16, toByteArray(value));
		this.value = value;
	}

	@Override
	public String toString() {
		return "ModbusRecordUInt16 [value=" + value + "/0x" + Integer.toHexString(value) + ", type=" + getType() + "]";
	}

	public static byte[] toByteArray(short value) {
		return ByteBuffer.allocate(BYTE_LENGTH).putShort(value).array();
	}

	public static byte[] toByteArray(Object value) {
		if (value == null) {
			return UNDEFINED_VALUE;
		} else {
			return toByteArray((short) TypeUtils.getAsType(OpenemsType.SHORT, value));
		}
	}

}
