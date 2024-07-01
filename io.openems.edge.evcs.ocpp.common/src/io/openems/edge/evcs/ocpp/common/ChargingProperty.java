package io.openems.edge.evcs.ocpp.common;

import java.time.ZonedDateTime;

/**
 * Charging Property.
 *
 * <p>
 * This class provides the charge power and the total energy of the meter of a
 * certain time stamp.
 */
public class ChargingProperty {

	private int chargePower;
	private double totalMeterEnergy;
	private ZonedDateTime timestamp;

	public ChargingProperty(int chargePower, double totalMeterEnergy, ZonedDateTime timestamp) {
		this.chargePower = chargePower;
		this.totalMeterEnergy = totalMeterEnergy;
		this.timestamp = timestamp;
	}

	public int getChargePower() {
		return this.chargePower;
	}

	public void setChargePower(int chargePower) {
		this.chargePower = chargePower;
	}

	public double getTotalMeterEnergy() {
		return this.totalMeterEnergy;
	}

	public void setTotalMeterEnergy(double totalMeterEnergy) {
		this.totalMeterEnergy = totalMeterEnergy;
	}

	public ZonedDateTime getTimestamp() {
		return this.timestamp;
	}

	public void setTimestamp(ZonedDateTime timestamp) {
		this.timestamp = timestamp;
	}
}
