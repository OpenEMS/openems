package io.openems.edge.common.modbusslave;

public class ModbusRecordUint32Reserved extends ModbusRecordUint32 {

	public ModbusRecordUint32Reserved(int offset) {
		super(offset, "Reserved", (Integer) null);
	}

	@Override
	public String toString() {
		return "ModbusRecordUint32Reserved [type=" + getType() + "]";
	}

}
