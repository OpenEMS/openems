package io.openems.edge.evcs.ocpp.core;

import java.util.Calendar;

/**
 * Charging Property.
 * 
 * <p>
 * This class provides the charge power and the total energy of the meter of a certain time stamp. 
 */
public class ChargingProperty {

	private int chargePower;
	private long totalMeterEnergy;
	private Calendar timestamp;
	
	public ChargingProperty(int chargePower, long totalMeterEnergy, Calendar timestamp) {
		this.chargePower = chargePower;
		this.totalMeterEnergy = totalMeterEnergy;
		this.timestamp = timestamp;
	}

	public int getChargePower() {
		return chargePower;
	}

	public void setChargePower(int chargePower) {
		this.chargePower = chargePower;
	}

	public long getTotalMeterEnergy() {
		return totalMeterEnergy;
	}

	public void setTotalMeterEnergy(long totalMeterEnergy) {
		this.totalMeterEnergy = totalMeterEnergy;
	}

	public Calendar getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Calendar timestamp) {
		this.timestamp = timestamp;
	}
	
}
