package io.openems.edge.common.modbusslave;

public class ModbusRecordFloat64Reserved extends ModbusRecordFloat64 {

	public ModbusRecordFloat64Reserved(int offset) {
		super(offset, "Reserved", (Double) null);
	}

	@Override
	public String toString() {
		return "ModbusRecordFloat64Reserved [type=" + getType() + "]";
	}

}
