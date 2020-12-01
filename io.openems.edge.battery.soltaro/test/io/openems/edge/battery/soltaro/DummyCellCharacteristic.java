package io.openems.edge.battery.soltaro;

public class DummyCellCharacteristic implements CellCharacteristic {
	
	public static final int FINAL_CELL_CHARGE_VOLTAGE_MV = 3_650;
	public static final int FINAL_CELL_DISCHARGE_VOLTAGE_MV = 2_900;
	public static final int FORCE_CHARGE_CELL_VOLTAGE_MV = 2_800;
	public static final int FORCE_DISCHARGE_CELL_VOLTAGE_MV = 3_680;

	@Override
	public int getFinalCellChargeVoltage_mV() {
		return FINAL_CELL_CHARGE_VOLTAGE_MV;
	}

	@Override
	public int getFinalCellDischargeVoltage_mV() {
		return FINAL_CELL_DISCHARGE_VOLTAGE_MV;
	}

	@Override
	public int getForceChargeCellVoltage_mV() {
		return FORCE_CHARGE_CELL_VOLTAGE_MV;
	}

	@Override
	public int getForceDischargeCellVoltage_mV() {
		return FORCE_DISCHARGE_CELL_VOLTAGE_MV;
	}

}
