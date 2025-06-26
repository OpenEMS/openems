package io.openems.edge.common.modbusslave;

public class ModbusRecordUint16Reserved extends ModbusRecordUint16 {

	public ModbusRecordUint16Reserved(int offset) {
		super(offset, "Reserved", null);
	}

	@Override
	public String toString() {
		return "ModbusRecordUint16Reserved [type=" + this.getType() + "]";
	}

}
