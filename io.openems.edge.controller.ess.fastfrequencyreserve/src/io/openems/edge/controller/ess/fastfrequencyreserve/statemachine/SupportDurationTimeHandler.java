package io.openems.edge.controller.ess.fastfrequencyreserve.statemachine;

import java.time.Duration;
import java.time.LocalDateTime;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.fastfrequencyreserve.statemachine.StateMachine.State;
import io.openems.edge.ess.api.ManagedSymmetricEss;

public class SupportDurationTimeHandler extends StateHandler<State, Context> {

	protected boolean flagDischargeHold;
	protected LocalDateTime startTime;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.flagDischargeHold = true;
	}

	protected void onExit(Context context) throws OpenemsNamedException {
		this.flagDischargeHold = true;
	}

	@Override
	protected State runAndGetNextState(Context context) throws OpenemsNamedException {

		var dischargePower = context.dischargePower;
		ManagedSymmetricEss e = context.ess;
		ComponentManager componentManager = context.componentManager;

		e.setActivePowerEquals(dischargePower);

		if (this.flagDischargeHold) {
			this.setOnEntryDischargeHold(componentManager);
		} else {
			long supportDurationExpiration = Duration //
					.between(this.startTime, LocalDateTime.now(componentManager.getClock())) //
					.getSeconds();

			if (supportDurationExpiration >= context.supportDuration.getValue()) {
				return State.DEACTIVATION_TIME;
			}
		}

		return State.SUPPORT_DURATION;
	}

	private void setOnEntryDischargeHold(ComponentManager componentManager) {
		this.startTime = LocalDateTime.now(componentManager.getClock());
		this.flagDischargeHold = false;

	}

}
