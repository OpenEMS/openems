package io.openems.edge.controller.ess.fastfrequencyreserve.statemachine;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.fastfrequencyreserve.statemachine.StateMachine.State;
import io.openems.edge.ess.api.ManagedSymmetricEss;

public class ActivationTimeHandler extends StateHandler<State, Context> {

	protected boolean isFreqDipDetected;
	protected boolean flagDischargeStart;
	protected LocalDateTime startTime;
	private static int ZERO_POWER = 0;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.isFreqDipDetected = false;
		this.flagDischargeStart = true;
	}

	protected void onExit(Context context) throws OpenemsNamedException {
		this.isFreqDipDetected = false;
		this.flagDischargeStart = true;
	}

	@Override
	protected State runAndGetNextState(Context context) throws OpenemsNamedException {

		var meterFreq = context.meter.getFrequency().get();
		var freqLimit = context.freqLimit;
		var dischargePower = context.dischargePower;
		ManagedSymmetricEss e = context.ess;
		ComponentManager componentManager = context.componentManager;

		// System.out.println(checkForFfrCycle(context.componentManager,
		// context.startTimestamp, context.duration));

		if (checkForFfrCycle(context.componentManager, context.startTimestamp, context.duration)) {
			return State.UNDEFINED;
		}

		if (meterFreq <= freqLimit && !this.isFreqDipDetected) {
			this.isFreqDipDetected = true;
		}

		if (this.isFreqDipDetected) {
			// e.setActivePowerEquals(dischargePower);
			context.setPowerandPrint(State.ACTIVATION_TIME, dischargePower, e, componentManager);

			if (this.flagDischargeStart) {
				this.setOnEntryDischargeStart(componentManager);
			} else {
				long activationExpiration = Duration //
						.between(this.startTime, LocalDateTime.now(componentManager.getClock())) //
						.toMillis(); //

				if (activationExpiration >= context.activationRunTime.getValue()) {
					return State.SUPPORT_DURATION;

				}
			}
		} else {
			context.setPowerandPrint(State.ACTIVATION_TIME, ZERO_POWER, e, componentManager);
			return State.ACTIVATION_TIME;
		}
		return State.ACTIVATION_TIME;

	}

	private void setOnEntryDischargeStart(ComponentManager componentManager) {
		this.startTime = LocalDateTime.now(componentManager.getClock());
		this.flagDischargeStart = false;
	}

	private boolean checkForFfrCycle(ComponentManager componentManager, long startTimestamp, int duration) {

		var now = ZonedDateTime.now(componentManager.getClock()).toEpochSecond();
		return now >= startTimestamp + duration;
	}

	@SuppressWarnings("unused")
	private static void printTime(String descr, long now) {
		System.out.println(descr + Instant.ofEpochSecond(now));
	}

}
