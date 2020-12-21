package io.openems.edge.battery.soltaro;

public class SoltaroCellCharacteristic implements CellCharacteristic {

	@Override
	public int getFinalCellDischargeVoltage_mV() {
		return 2_900; //  
	}

	@Override
	public int getForceChargeCellVoltage_mV() {
		return 2_800; // 0x2046 Cell under voltage Protection
	}
	
	@Override
	public int getFinalCellChargeVoltage_mV() {
		return 3_650; // 0x0041  Cell Over Voltage Recover / 0x0080 Cell over Voltage Alarm
	}

	@Override
	public int getForceDischargeCellVoltage_mV() {
		return 3_680; // 
	}

}
