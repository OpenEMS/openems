package io.openems.edge.common.modbusslave;

public class ModbusRecordFloat32Reserved extends ModbusRecordFloat32 {

	public ModbusRecordFloat32Reserved(int offset) {
		super(offset, "Reserved", null);
	}

	@Override
	public String toString() {
		return "ModbusRecordFloat32Reserved [type=" + this.getType() + "]";
	}

}
