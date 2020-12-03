package io.openems.edge.common.modbusslave;

import java.util.function.Supplier;

import io.openems.common.channel.AccessMode;

public abstract class AbstractModbusRecordSupplier extends AbstractModbusRecord {

	private final String name;
	private final Supplier<byte[]> valueSupplier;

	public AbstractModbusRecordSupplier(int offset, String name, ModbusType type, Supplier<byte[]> valueSupplier) {
		super(offset, type);
		this.name = name;
		this.valueSupplier = valueSupplier;
	}

	public AbstractModbusRecordSupplier(int offset, String name, ModbusType type, byte[] value) {
		this(offset, name, type, () -> {
			return value;
		});
	}

	@Override
	public String toString() {
		return "ModbusRecordSupplier [offset=" + this.getOffset() + ", type=" + this.getType() + "]";
	}

	/**
	 * Returns the actual value as a byte-array.
	 * 
	 * @return the value
	 */
	public byte[] getValue() {
		return this.valueSupplier.get();
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public AccessMode getAccessMode() {
		return AccessMode.READ_ONLY;
	}

}
