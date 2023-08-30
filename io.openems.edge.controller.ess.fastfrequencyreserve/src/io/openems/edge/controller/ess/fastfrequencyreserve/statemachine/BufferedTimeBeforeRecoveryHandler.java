package io.openems.edge.controller.ess.fastfrequencyreserve.statemachine;

import java.time.Duration;
import java.time.LocalDateTime;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.fastfrequencyreserve.statemachine.StateMachine.State;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;

public class BufferedTimeBeforeRecoveryHandler extends StateHandler<State, Context> {

	public static final int FIFTEEN_MINUTES = 15;
	private static int ZERO_POWER = 0;
	// private static int TEN_SECONDS_IN_MILLIS = 10000;

	protected LocalDateTime startTime;
	protected boolean flagBufferedRecoveryStart;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.flagBufferedRecoveryStart = true;
	}

	@Override
	protected void onExit(Context context) throws OpenemsNamedException {
		this.flagBufferedRecoveryStart = true;
	}

	@Override
	protected State runAndGetNextState(Context context) throws OpenemsNamedException {

		ManagedSymmetricEss e = context.ess;
		ComponentManager componentManager = context.componentManager;
		var minPowerEss = e.getPower().getMinPower(context.ess, Phase.ALL, Pwr.ACTIVE);

		// Charge with 18 % of min Power of ess
		// e.setActivePowerEquals((int) (-minPowerEss * 0.18));
		context.setPowerandPrint(State.BUFFERED_TIME_BEFORE_RECOVERY, (int) (minPowerEss * 0.18), e, componentManager);

		if (this.flagBufferedRecoveryStart) {
			this.setOnEntryBufferedRecoveryStart(componentManager);

		} else {
			long x = Duration //
					.between(context.getCycleStart(), LocalDateTime.now(componentManager.getClock())) //
					.getSeconds();
			if (x <= 15) {
				context.setPowerandPrint(State.BUFFERED_TIME_BEFORE_RECOVERY, ZERO_POWER, e, componentManager);
			} else {
				return State.RECOVERY_TIME;
			}

		}
		return State.BUFFERED_TIME_BEFORE_RECOVERY;
	}

	private void setOnEntryBufferedRecoveryStart(ComponentManager componentManager) {
		this.startTime = LocalDateTime.now(componentManager.getClock());
		this.flagBufferedRecoveryStart = false;

	}
}
