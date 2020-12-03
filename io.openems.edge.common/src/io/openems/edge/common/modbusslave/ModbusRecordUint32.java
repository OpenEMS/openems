package io.openems.edge.common.modbusslave;

import java.nio.ByteBuffer;
import java.util.function.Supplier;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.type.TypeUtils;

public class ModbusRecordUint32 extends AbstractModbusRecordSupplier {

	public final static byte[] UNDEFINED_VALUE = new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };

	public final static int BYTE_LENGTH = 4;

	protected final Integer value;

	public ModbusRecordUint32(int offset, String name, Integer value) {
		super(offset, name, ModbusType.UINT32, toByteArray(value));
		this.value = value;
	}

	public ModbusRecordUint32(int offset, String name, Supplier<Integer> valueSupplier) {
		super(offset, name, ModbusType.UINT32, () -> {
			return toByteArray(valueSupplier.get());
		});
		this.value = null;
	}

	@Override
	public String toString() {
		if (this.value == null) {
			return "ModbusRecordUInt32 [value=UNDEFINED" + ", type=" + getType() + "]";
		} else {
			return "ModbusRecordUInt32 [value=" + value + "/0x" + Integer.toHexString(value) + ", type=" + getType()
					+ "]";
		}
	}

	/**
	 * Converts a Integer value to a byte-array.
	 * 
	 * @param value the Short value
	 * @return the byte-array
	 */
	public static byte[] toByteArray(Object value) {
		if (value == null || (value instanceof io.openems.common.types.OptionsEnum
				&& ((io.openems.common.types.OptionsEnum) value).isUndefined())) {
			return UNDEFINED_VALUE;
		} else {
			Integer intValue = (Integer) TypeUtils.getAsType(OpenemsType.INTEGER, value);
			if (intValue == null) {
				return UNDEFINED_VALUE;
			} else {
				return ByteBuffer.allocate(BYTE_LENGTH).putInt(intValue).array();
			}
		}
	}

	@Override
	public String getValueDescription() {
		return this.value != null ? Integer.toString(this.value) : "";
	}

}
