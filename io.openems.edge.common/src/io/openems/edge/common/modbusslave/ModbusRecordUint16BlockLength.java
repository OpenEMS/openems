package io.openems.edge.common.modbusslave;

public class ModbusRecordUint16BlockLength extends ModbusRecordUint16 {

	private final String blockName;

	public ModbusRecordUint16BlockLength(int offset, String blockName, short length) {
		super(offset, "Length of block \"" + blockName + "\"", length);
		this.blockName = blockName;
	}

	@Override
	public String toString() {
		return generateToString("ModbusRecordUint16BlockLength",
				b -> b.append("blockName=").append(this.blockName).append(", "), this.value,
				v -> Integer.toHexString(v));
	}

}
