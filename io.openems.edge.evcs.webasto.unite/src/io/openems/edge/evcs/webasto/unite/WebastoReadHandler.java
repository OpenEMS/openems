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
			case (0) :
				this.parent._setStatus(Status.NOT_READY_FOR_CHARGING);
				break;
			case (1) :
				this.parent._setStatus(Status.READY_FOR_CHARGING);
				break;
			case (2) :
				this.parent._setStatus(Status.CHARGING);
				break;
			case (3) :
			case (4) :
				this.parent._setStatus(Status.CHARGING_REJECTED);
				break;
			case (5) :
				// TODO Check if this state is also reached while paused
				this.parent._setStatus(Status.CHARGING_FINISHED);
				break;
			case (7) :
			case (8) :
				this.parent._setStatus(Status.ERROR);
		}
	}

	/**
	 * Writes the Amount of Phases in the Phase channel.
	 */
	private void setPhaseCount() {
		int phases = 0;

		if (this.parent.getActivePowerL1() >= 100) {
			phases += 1;
		}
		if (this.parent.getActivePowerL2() >= 100) {
			phases += 1;
		}
		if (this.parent.getActivePowerL3() >= 100) {
			phases += 1;
		}
		this.parent._setPhases(phases);

	}

}
