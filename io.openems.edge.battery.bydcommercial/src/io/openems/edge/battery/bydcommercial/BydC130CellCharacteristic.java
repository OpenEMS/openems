package io.openems.edge.battery.bydcommercial;

import io.openems.edge.battery.api.CellCharacteristic;

public class BydC130CellCharacteristic implements CellCharacteristic {

	@Override
	public int getFinalCellDischargeVoltage_mV() {
		return 2_900;   
	}

	@Override
	public int getForceChargeCellVoltage_mV() {
		return 2_850; 
	}
	
	@Override
	public int getFinalCellChargeVoltage_mV() {
		return 3_650; 
	}

	@Override
	public int getForceDischargeCellVoltage_mV() {
		return 3_680;  
	}

}