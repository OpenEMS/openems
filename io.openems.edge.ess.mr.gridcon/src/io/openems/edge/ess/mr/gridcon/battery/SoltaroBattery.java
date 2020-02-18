package io.openems.edge.ess.mr.gridcon.battery;

public interface SoltaroBattery {
	
	void start();
	void stop();
	
	boolean isRunning();
	boolean isStopped();
	boolean isError();
	
	float getMinimalCellVoltage();
	float getMaximalCellVoltage();
	float getSoCX(); //%
	float getCapacityX(); //Wh
	float getCurrentX();
	float getVoltageX();
	float getMaxChargeCurrentX();
	float getMaxDischargeCurrentX();
	float getMaxChargeVoltageX();
	float getMinDischargeVoltageX();
	
	//TODO Voltage, Current, min max temperature, minmax (dis)charge voltage and current
	
}
