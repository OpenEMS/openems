package io.openems.edge.common.modbusslave;

public class ModbusRecordUint16BlockLength extends ModbusRecordUint16 {

	private final String blockName;

	public ModbusRecordUint16BlockLength(int offset, String blockName, short length) {
		super(offset, "Length of block \"" + blockName + "\"", length);
		this.blockName = blockName;
	}

	@Override
	public String toString() {
		return "ModbusRecordUint16BlockLength [blockName=" + this.blockName + ", value=" + this.value + "/0x"
				+ Integer.toHexString(this.value) + ", type=" + this.getType() + "]";
	}

}
