package io.openems.edge.common.modbusslave;

import java.nio.ByteBuffer;
import java.util.function.Supplier;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.type.TypeUtils;

public class ModbusRecordUint16 extends AbstractModbusRecordSupplier {

	public final static byte[] UNDEFINED_VALUE = new byte[] { (byte) 0xFF, (byte) 0xFF };

	public final static int BYTE_LENGTH = 2;

	protected final Short value;

	public ModbusRecordUint16(int offset, String name, Short value) {
		super(offset, name, ModbusType.UINT16, toByteArray(value));
		this.value = value;
	}

	public ModbusRecordUint16(int offset, String name, Supplier<Short> valueSupplier) {
		super(offset, name, ModbusType.UINT16, () -> {
			return toByteArray(valueSupplier.get());
		});
		this.value = null;
	}

	@Override
	public String toString() {
		if (this.value == null) {
			return "ModbusRecordUInt16 [value=" + this.value + ", type=" + getType() + "]";
		} else {
			return "ModbusRecordUInt16 [value=" + this.value + "/0x" + Integer.toHexString(this.value) + ", type="
					+ getType() + "]";
		}
	}

	/**
	 * Converts a Short value to a byte-array.
	 * 
	 * @param value the Short value
	 * @return the byte-array
	 */
	public static byte[] toByteArray(Object value) {
		if (value == null || (value instanceof io.openems.common.types.OptionsEnum
				&& ((io.openems.common.types.OptionsEnum) value).isUndefined())) {
			return UNDEFINED_VALUE;
		} else {
			Short shortValue = (Short) TypeUtils.getAsType(OpenemsType.SHORT, value);
			if (shortValue == null) {
				return UNDEFINED_VALUE;
			} else {
				return ByteBuffer.allocate(BYTE_LENGTH).putShort(shortValue).array();
			}
		}
	}

	@Override
	public String getValueDescription() {
		return this.value != null ? Short.toString(this.value) : "";
	}

}
