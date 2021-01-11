package io.openems.edge.battery.soltaro;

public interface CellCharacteristic {

	/**
	 * German "Beladeschlussspannung".
	 * 
	 * @return the final cell charge voltage in [mV]
	 */
	int getFinalCellChargeVoltage_mV();

	/**
	 * German "Entladeschlussspannung".
	 * 
	 * @return the final cell discharge voltage in [mV]
	 */
	int getFinalCellDischargeVoltage_mV();

	/**
	 * German "Zwangsbeladespannung".
	 * 
	 * @return the force charge cell voltage in [mV]
	 */
	int getForceChargeCellVoltage_mV();

	/**
	 * German "Zwangsentladespannung".
	 * 
	 * @return the force discharge cell voltage in [mV]
	 */
	int getForceDischargeCellVoltage_mV();

}
