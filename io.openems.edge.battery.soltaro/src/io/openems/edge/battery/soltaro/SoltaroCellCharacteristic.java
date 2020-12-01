package io.openems.edge.battery.soltaro;

public class SoltaroCellCharacteristic implements CellCharacteristic {

	@Override
	public int getFinalCellDischargeVoltage_mV() {
		return 2_800; // x2086 Cell under voltage Alarm 
	}

	@Override
	public int getForceChargeCellVoltage_mV() {
		return 2_750; // x2047 Cell under voltage Protection  recover
	}
	
	@Override
	public int getFinalCellChargeVoltage_mV() {
		return 3_650; // 0x0080  Cell Over Voltage Alarm
	}

	@Override
	public int getForceDischargeCellVoltage_mV() {
		return 3_680; // 0x0041  Cell Over Voltage Recover
	}

}
