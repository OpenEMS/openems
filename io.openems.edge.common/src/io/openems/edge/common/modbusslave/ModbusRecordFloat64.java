package io.openems.edge.common.modbusslave;

import java.nio.ByteBuffer;
import java.util.function.Supplier;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.type.TypeUtils;

public class ModbusRecordFloat64 extends AbstractModbusRecordSupplier {

	public final static byte[] UNDEFINED_VALUE = new byte[] { (byte) 0x7F, (byte) 0xF8, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

	public final static int BYTE_LENGTH = 8;

	private final Double value;

	public ModbusRecordFloat64(int offset, String name, Double value) {
		super(offset, name, ModbusType.FLOAT64, toByteArray(value));
		this.value = value;
	}

	public ModbusRecordFloat64(int offset, String name, Supplier<Double> valueSupplier) {
		super(offset, name, ModbusType.FLOAT64, () -> {
			return toByteArray(valueSupplier.get());
		});
		this.value = null;
	}

	@Override
	public String toString() {
		return "ModbusRecordFloat64 [value=" + this.value + ", type=" + getType() + "]";
	}

	/**
	 * Converts a Double value to a byte-array.
	 * 
	 * @param value the Double value
	 * @return the byte-array
	 */
	public static byte[] toByteArray(Object value) {
		if (value == null) {
			return UNDEFINED_VALUE;
		} else {
			Double doubleValue = (Double) TypeUtils.getAsType(OpenemsType.DOUBLE, value);
			if (doubleValue == null) {
				return UNDEFINED_VALUE;
			} else {
				return ByteBuffer.allocate(BYTE_LENGTH).putDouble(doubleValue).array();
			}
		}
	}

	@Override
	public String getValueDescription() {
		return this.value != null ? this.value.toString() : "";
	}

}
