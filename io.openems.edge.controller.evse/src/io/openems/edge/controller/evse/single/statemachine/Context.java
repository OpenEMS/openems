package io.openems.edge.controller.evse.single.statemachine;

import java.util.function.Consumer;

import io.openems.edge.common.statemachine.AbstractContext;
import io.openems.edge.controller.evse.single.ControllerEvseSingleImpl;
import io.openems.edge.controller.evse.single.Types.History;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Profile;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;

public class Context extends AbstractContext<ControllerEvseSingleImpl> {

	protected final ChargePointActions actions;
	protected final EvseChargePoint chargePoint;
	protected final History history;
	protected final Consumer<ChargePointActions> callback;

	public Context(ControllerEvseSingleImpl parent, ChargePointActions actions, EvseChargePoint chargePoint,
			History history, Consumer<ChargePointActions> callback) {
		super(parent);
		this.actions = actions;
		this.chargePoint = chargePoint;
		this.history = history;
		this.callback = callback;
	}

	private void applyActions(ChargePointActions actions) {
		this.callback.accept(actions);
	}

	/**
	 * Apply the actions provided by EVSE Cluster.
	 */
	protected void applyActions() {
		this.applyActions(this.actions);
	}

	/**
	 * Apply adjusted actions.
	 * 
	 * @param adjustedActions Callback for adjusted {@link ChargePointActions}.
	 */
	protected void applyAdjustedActions(Consumer<ChargePointActions.Builder> adjustedActions) {
		var builder = Profile.ChargePointActions.copy(this.actions);
		adjustedActions.accept(builder);
		this.applyActions(builder.build());
	}

	/**
	 * Apply minimum set-points.
	 */
	protected void applyMinSetPointActions() {
		this.applyAdjustedActions(b -> b //
				.setApplyMinSetPoint());
	}
}