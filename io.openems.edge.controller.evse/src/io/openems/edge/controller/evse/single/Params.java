package io.openems.edge.controller.evse.single;

import java.util.function.Consumer;

import io.openems.edge.evse.api.Limit;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint.ApplyCharge;
import io.openems.edge.evse.api.chargepoint.Mode;

public record Params(boolean readyForCharging, Mode.Actual actualMode, Limit limit,
		Consumer<ApplyCharge> applyChargeCallback) {

	/**
	 * Applies charging.
	 * 
	 * @param applyCharge the {@link ApplyCharge} record
	 */
	public void applyCharge(ApplyCharge applyCharge) {
		if (this.applyChargeCallback != null) {
			this.applyChargeCallback.accept(applyCharge);
		}
	}
}