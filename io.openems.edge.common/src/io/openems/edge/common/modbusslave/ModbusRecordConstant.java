package io.openems.edge.common.modbusslave;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.edge.common.component.OpenemsComponent;

public abstract class ModbusRecordConstant extends ModbusRecord {

	private final Logger log = LoggerFactory.getLogger(ModbusRecordConstant.class);

	private final String name;
	private final byte[] value;

	public ModbusRecordConstant(int offset, String name, ModbusType type, byte[] value) {
		super(offset, type);
		this.name = name;
		this.value = value;
	}

	@Override
	public String toString() {
		return "ModbusRecordConstant [offset=" + this.getOffset() + ", type=" + this.getType() + "]";
	}

	public byte[] getValue() {
		return this.value;
	}

	@Override
	public byte[] getValue(OpenemsComponent component) {
		return this.getValue();
	}

	@Override
	public void writeValue(int index, byte byte1, byte byte2) {
		this.log.warn("Writing to Read-Only Modbus Record is not allowed! [" + this.getOffset() + ", " + this.getType()
				+ "]");
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
