package io.openems.edge.evcs.ocpp.common;

import java.time.Instant;

public class ChargeSession {

	private Instant time;
	private Integer energy;
	
	public ChargeSession(Instant time, Integer energy) {
		this.time = time;
		this.energy = energy;
	}
	
	public ChargeSession(int energy) {
		this(Instant.now(), energy);
	}
	
	public ChargeSession(Instant time) {
		this(time, null);
	}
	
	public ChargeSession() {
		this(null, null);
	}

	public Instant getTime() {
		return time;
	}

	public void setTime(Instant time) {
		this.time = time;
	}

	public Integer getEnergy() {
		return energy;
	}

	public void setEnergy(Integer energy) {
		this.energy = energy;
	}
}
