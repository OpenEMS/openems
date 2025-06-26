package io.openems.edge.common.modbusslave;

public class ModbusRecordUint64Reserved extends ModbusRecordUint64 {

	public ModbusRecordUint64Reserved(int offset) {
		super(offset, "Reserved", null);
	}

	@Override
	public String toString() {
		return "ModbusRecordUint64Reserved [type=" + this.getType() + "]";
	}

}
