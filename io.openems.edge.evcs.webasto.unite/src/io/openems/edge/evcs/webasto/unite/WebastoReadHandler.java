package io.openems.edge.evcs.webasto.unite;

import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.Status;

public class WebastoReadHandler {

	private final WebastoImpl parent;

	protected WebastoReadHandler(WebastoImpl parent) {
		this.parent = parent;
	}

	protected void run() {
		this.setPhaseCount();
		this.setStatus();
		this.parent._setChargePower((int) this.parent.getActivePower());
		this.parent._setChargingType(ChargingType.AC);
		this.parent._setEnergySession((int) this.parent.getSessionEnergy());
	}

	private void setStatus() {
		switch (this.parent.getChargePointState()) {
		case (0):
			this.parent._setStatus(Status.STARTING);
			break;
		case (1):
		case (5):
			this.parent._setStatus(Status.READY_FOR_CHARGING);
			break;
		case (2):
		case (3):
		case (4):
			this.parent._setStatus(Status.CHARGING);
			break;
		case (7):
		case (8):
			this.parent._setStatus(Status.ERROR);
		}
	}

	/**
	 * Writes the Amount of Phases in the Phase channel.
	 */
	private void setPhaseCount() {
		int phases = 0;

		if (this.parent.getActivePowerL1() >= 1) {
			phases += 1;
		}
		if (this.parent.getActivePowerL2() >= 1) {
			phases += 1;
		}
		if (this.parent.getActivePowerL3() >= 1) {
			phases += 1;
		}
		this.parent._setPhases(phases);

	}

}
