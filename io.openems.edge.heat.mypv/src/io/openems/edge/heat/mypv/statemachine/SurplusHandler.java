package io.openems.edge.heat.mypv.statemachine;

import static io.openems.edge.heat.mypv.statemachine.MyPvConstants.SURPLUS_UPDATE_INTERVAL;

import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.heat.mypv.statemachine.StateMachine.State;

public class SurplusHandler extends StateHandler<State, Context> {
	private Integer stableTargetActivePower = null;
	private Instant lastStableTargetUpdateAt = null;

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {

		var now = context.clock.instant();
		if (this.shouldUpdateStableTarget(now)) {
			this.stableTargetActivePower = context.determineSurplusTargetActivePower();
			this.lastStableTargetUpdateAt = now;
		}

		context.setTargetActivePowerForHeatElement(this.stableTargetActivePower);
		return State.SURPLUS;
	}

	private boolean shouldUpdateStableTarget(Instant now) {
		return this.stableTargetActivePower == null //
				|| this.lastStableTargetUpdateAt == null //
				|| !now.isBefore(this.lastStableTargetUpdateAt.plus(SURPLUS_UPDATE_INTERVAL));
	}

	@Override
	protected void onExit(Context context) {
		this.stableTargetActivePower = null;
		this.lastStableTargetUpdateAt = null;
	}
}
