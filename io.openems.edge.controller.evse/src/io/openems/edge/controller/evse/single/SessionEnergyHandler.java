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
		switch (chargePoint.getStatus()) {
		case UNDEFINED /* -1 */, STARTING /* 0 */, NOT_READY_FOR_CHARGING /* 1 */, READY_FOR_CHARGING /* 2 */
			-> this.sessionStartEnergy = chargePoint.getActiveProductionEnergy().get();

		case CHARGING /* 3 */, ERROR /* 4 */, CHARGING_REJECTED /* 5 */ -> {
			if (this.sessionStartEnergy == null) {
				this.sessionStartEnergy = chargePoint.getActiveProductionEnergy().get();
			}
		}
		}
	}
}
