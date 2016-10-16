package io.openems.impl.controller.balancing;

import io.openems.api.controller.Controller;
import io.openems.api.thing.IsConfigParameter;
import io.openems.api.thing.Thing;

public class BalancingController extends Controller {

	private boolean chargeFromAc = false;
	private Thing gridMeter = null;

	@IsConfigParameter("chargeFromAc")
	public void setChargeFromAc(boolean chargeFromAc) {
		this.chargeFromAc = chargeFromAc;
	}

	@IsConfigParameter("gridMeter")
	public void setGridMeter(Thing gridMeter) {
		this.gridMeter = gridMeter;
	}

	@Override
	public String toString() {
		return "BalancingController [chargeFromAc=" + chargeFromAc + ", getPriority()=" + getPriority() + "]";
	}
}
