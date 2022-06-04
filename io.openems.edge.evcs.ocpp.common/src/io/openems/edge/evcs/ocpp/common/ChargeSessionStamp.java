package io.openems.edge.evcs.ocpp.common;

import java.time.Instant;

public class ChargeSessionStamp {

	private Instant time;
	private long energy;

	/**
	 * Constructor of a ChargeSession with the given time and energy.
	 * 
	 * @param time   the {@link Instant} time
	 * @param energy the Energy
	 */
	public ChargeSessionStamp(Instant time, long energy) {
		this.time = time;
		this.energy = energy;
	}

	/**
	 * Constructor of a ChargeSession with the initial Energy.
	 * 
	 * <p>
	 * The time will be initialized by Instant.now().
	 * 
	 * @param energy the Energy
	 */
	public ChargeSessionStamp(long energy) {
		this(Instant.now(), energy);
	}

	/**
	 * Constructor of a ChargeSession with the initial Time.
	 * 
	 * <p>
	 * The energy will be initialized by 0.
	 * 
	 * @param time the {@link Instant} time
	 */
	public ChargeSessionStamp(Instant time) {
		this(time, 0);
	}

	/**
	 * Constructor of a ChargeSession with no initial values.
	 */
	public ChargeSessionStamp() {
		this(null, 0);
	}

	public Instant getTime() {
		return this.time;
	}

	public void setTime(Instant time) {
		this.time = time;
	}

	public long getEnergy() {
		return this.energy;
	}

	public void setEnergy(long energy) {
		this.energy = energy;
	}

	public boolean isChargeSessionStampPresent() {
		return this.time != null;
	}

	public void setChargeSessionStamp(Instant time, long energy) {
		this.time = time;
		this.energy = energy;
	}

	protected void resetChargeSessionStamp() {
		this.time = null;
		this.energy = 0;
	}

	public void setChargeSessionStampIfNotPresent(Instant time, long energy) {
		this.setChargeSessionStamp(time, energy);
	}

	/**
	 * Reset the Charge Session Timestamp.
	 */
	public void resetChargeSessionStampIfPresent() {
		if (this.isChargeSessionStampPresent()) {
			this.resetChargeSessionStamp();
		}
	}
}
