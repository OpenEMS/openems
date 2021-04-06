package io.openems.edge.goodwe.common.applypower;

import io.openems.edge.common.statemachine.AbstractContext;
import io.openems.edge.goodwe.common.GoodWe;
import io.openems.edge.goodwe.common.enums.GoodweType;
import io.openems.edge.goodwe.common.enums.PowerModeEms;

public class Context extends AbstractContext<GoodWe> {

	protected final GoodweType goodweType;
	protected final boolean readOnlyMode;
	protected final int pvProduction;
	protected final int soc;
	protected final int activePowerSetPoint;

	private PowerModeEms nextPowerMode;
	private int essPowerSet;

	public Context(GoodWe parent, GoodweType goodweType, boolean readOnlyMode, int pvProduction, int soc,
			int activePowerSetPoint) {
		super(parent);
		this.goodweType = goodweType;
		this.readOnlyMode = readOnlyMode;
		this.pvProduction = pvProduction;
		this.soc = soc;
		this.activePowerSetPoint = activePowerSetPoint;
	}

	protected void setMode(PowerModeEms nextPowerMode, int essPowerSet) {
		this.nextPowerMode = nextPowerMode;
		this.essPowerSet = essPowerSet;
	}

	/**
	 * Gets the resulting Power-Set value.
	 * 
	 * @return the value
	 */
	public int getEssPowerSet() {
		return this.essPowerSet;
	}

	/**
	 * Gets the resulting {@link PowerModeEms} value.
	 * 
	 * @return the value
	 */
	public PowerModeEms getNextPowerMode() {
		return this.nextPowerMode;
	}
}
