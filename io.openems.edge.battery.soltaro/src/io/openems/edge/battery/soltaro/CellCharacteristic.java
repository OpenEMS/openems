package io.openems.edge.battery.soltaro;

public interface CellCharacteristic {

	int getFinalCellChargeVoltage_mV();
	int getFinalCellDischargeVoltage_mV();
	int getForceChargeCellVoltage_mV();
	int getForceDischargeCellVoltage_mV();
	
}
