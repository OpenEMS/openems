package io.openems.edge.controller.ess.fastfrequencyreserve.statemachine;

import java.time.Duration;
import java.time.LocalDateTime;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.fastfrequencyreserve.statemachine.StateMachine.State;
import io.openems.edge.ess.api.ManagedSymmetricEss;

public class DeactivationTimeHandler extends StateHandler<State, Context> {

	private static int ZERO_POWER = 0;
	protected boolean flagZeroPowerStart;
	protected LocalDateTime startTime;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.flagZeroPowerStart = true;
	}

	protected void onExit(Context context) throws OpenemsNamedException {
		this.flagZeroPowerStart = true;
	}

	@Override
	protected State runAndGetNextState(Context context) throws OpenemsNamedException {

		ManagedSymmetricEss e = context.ess;
		ComponentManager componentManager = context.componentManager;

		e.setActivePowerEquals(ZERO_POWER);

		if (this.flagZeroPowerStart) {
			this.setOnZeroPowerStart(componentManager);
		} else {
			long x = Duration //
					.between(this.startTime, LocalDateTime.now(componentManager.getClock())) //
					.toMillis();

			if (x >= context.activationRunTime.getValue()) {
				return State.BUFFERED_TIME_BEFORE_RECOVERY;
			}
		}

		return State.DEACTIVATION_TIME;
	}

	private void setOnZeroPowerStart(ComponentManager componentManager) {
		this.startTime = LocalDateTime.now(componentManager.getClock());
		this.flagZeroPowerStart = false;

	}

}
