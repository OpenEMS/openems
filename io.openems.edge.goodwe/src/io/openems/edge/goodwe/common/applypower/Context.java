package io.openems.edge.goodwe.common.applypower;

import io.openems.edge.common.statemachine.AbstractContext;
import io.openems.edge.goodwe.common.GoodWe;
import io.openems.edge.goodwe.common.enums.PowerModeEms;

public class Context extends AbstractContext<GoodWe> {

	protected final int pvProduction;
	protected final int activePowerSetPoint;

	private PowerModeEms nextPowerMode;
	private int essPowerSet;

	public Context(GoodWe parent, int pvProduction, int activePowerSetPoint) {
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
