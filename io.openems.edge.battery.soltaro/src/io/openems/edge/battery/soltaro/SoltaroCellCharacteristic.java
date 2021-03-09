package io.openems.edge.battery.soltaro;

import io.openems.edge.battery.api.CellCharacteristic;

public class SoltaroCellCharacteristic implements CellCharacteristic {

	@Override
	public int getFinalCellDischargeVoltage_mV() {
		return 2_900; //  
	}

	@Override
	public int getForceChargeCellVoltage_mV() {
		return 2_850; // 0x2046 Cell under voltage Protection + 50 mV (i.e. x2046 ==> 2800)
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