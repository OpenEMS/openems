package io.openems.edge.common.modbusslave;

public class ModbusRecordInt16Reserved extends ModbusRecordInt16 {

	public ModbusRecordInt16Reserved(int offset) {
		super(offset, "Reserved", null);
	}

	@Override
	public String toString() {
		return "ModbusRecordInt16Reserved [type=" + this.getType() + "]";
	}

}
