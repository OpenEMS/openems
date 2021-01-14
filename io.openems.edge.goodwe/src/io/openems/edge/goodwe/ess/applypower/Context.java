package io.openems.edge.goodwe.ess.applypower;

import io.openems.edge.common.statemachine.AbstractContext;
import io.openems.edge.goodwe.ess.GoodWeEssImpl;
import io.openems.edge.goodwe.ess.enums.PowerModeEms;

public class Context extends AbstractContext<GoodWeEssImpl> {

	protected final int pvProduction;
	protected final int activePowerSetPoint;

	private PowerModeEms nextPowerMode;
	private int essPowerSet;

	public Context(GoodWeEssImpl parent, int pvProduction, int activePowerSetPoint) {
		super(parent);
		this.pvProduction = pvProduction;
		this.activePowerSetPoint = activePowerSetPoint;
	}

	protected void setMode(PowerModeEms nextPowerMode, int essPowerSet) {
		this.nextPowerMode = nextPowerMode;
		this.essPowerSet = essPowerSet;
	}

	public int getEssPowerSet() {
		return essPowerSet;
	}

	public PowerModeEms getNextPowerMode() {
		return nextPowerMode;
	}
}