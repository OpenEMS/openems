package io.openems.edge.common.modbusslave;

import java.nio.ByteBuffer;
import java.util.function.Supplier;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.type.TypeUtils;

public class ModbusRecordFloat32 extends AbstractModbusRecordSupplier {

	public final static byte[] UNDEFINED_VALUE = new byte[] { (byte) 0x7F, (byte) 0xC0, (byte) 0x00, (byte) 0x00 };

	public final static int BYTE_LENGTH = 4;

	private final Float value;

	public ModbusRecordFloat32(int offset, String name, Supplier<Float> valueSupplier) {
		super(offset, name, ModbusType.FLOAT32, () -> {
			return toByteArray(valueSupplier.get());
		});
		this.value = null;
	}

	public ModbusRecordFloat32(int offset, String name, Float value) {
		super(offset, name, ModbusType.FLOAT32, toByteArray(value));
		this.value = value;
	}

	@Override
	public String toString() {
		return "ModbusRecordFloat32 [value=" + this.value + ", type=" + getType() + "]";
	}

	/**
	 * Converts a Float value to a byte-array.
	 * 
	 * @param value the Float value
	 * @return the byte-array
	 */
	public static byte[] toByteArray(Object value) {
		if (value == null) {
			return UNDEFINED_VALUE;
		} else {
			Float floatValue = (Float) TypeUtils.getAsType(OpenemsType.FLOAT, value);
			if (floatValue == null) {
				return UNDEFINED_VALUE;
			} else {
				return ByteBuffer.allocate(BYTE_LENGTH).putFloat(floatValue).array();
			}
		}
	}

	@Override
	public String getValueDescription() {
		return this.value != null ? this.value.toString() : "";
	}

}
