package io.openems.edge.controller.evse.single;

import io.openems.edge.evse.api.chargepoint.EvseChargePoint;

public class SessionEnergyHandler {

	private Long sessionStartEnergy = null;

	protected int onBeforeProcessImage(EvseChargePoint chargePoint) {
		var totalEnergy = chargePoint.getActiveProductionEnergy().get();
		if (this.sessionStartEnergy == null || totalEnergy == null) {
			return 0;
		}
		return (int) Math.max(0, totalEnergy - this.sessionStartEnergy);
	}

	protected void onAfterProcessImage(EvseChargePoint chargePoint) {
		if (chargePoint.getIsReadyForCharging()) {
			if (this.sessionStartEnergy == null) {
				this.sessionStartEnergy = chargePoint.getActiveProductionEnergy().get();
			}

		} else {
			this.sessionStartEnergy = chargePoint.getActiveProductionEnergy().get();
		}
	}
}
