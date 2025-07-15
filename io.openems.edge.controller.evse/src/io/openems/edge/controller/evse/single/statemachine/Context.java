package io.openems.edge.controller.evse.single.statemachine;

import java.util.function.Consumer;

import io.openems.edge.common.statemachine.AbstractContext;
import io.openems.edge.controller.evse.single.ControllerEvseSingleImpl;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;

public class Context extends AbstractContext<ControllerEvseSingleImpl> {

	protected final ChargePointActions actions;
	protected final EvseChargePoint chargePoint;
	protected final Consumer<ChargePointActions> callback;

	public Context(ControllerEvseSingleImpl parent, ChargePointActions actions, EvseChargePoint chargePoint,
			Consumer<ChargePointActions> callback) {
		super(parent);
		this.actions = actions;
		this.chargePoint = chargePoint;
		this.callback = callback;
	}

	protected void apply(ChargePointActions actions) {
		this.callback.accept(actions);
	}
}