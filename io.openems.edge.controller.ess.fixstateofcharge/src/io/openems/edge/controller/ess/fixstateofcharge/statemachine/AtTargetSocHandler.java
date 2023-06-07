package io.openems.edge.controller.ess.fixstateofcharge.statemachine;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.fixstateofcharge.ControllerEssFixStateOfChargeImpl;
import io.openems.edge.controller.ess.fixstateofcharge.api.AbstractFixStateOfCharge;
import io.openems.edge.controller.ess.fixstateofcharge.statemachine.StateMachine.State;

public class AtTargetSocHandler extends StateHandler<State, Context> {

	@Override
	protected State runAndGetNextState(Context context) throws OpenemsNamedException {

		this.setAtTargetEpochSeconds(context.clock, context.getParent());
		context.setTargetPower(0);
		context.setRampPower(context.maxApparentPower * 0.1);

		/*
		 * Check if SoC is no longer "AtTargetSoc".
		 * 
		 * Keep staying in AT_TARGET_SOC State even if the SoC drops or rises by one.
		 */
		var socDiff = Math.abs(context.soc - context.targetSoc);
		if (socDiff > AbstractFixStateOfCharge.DEFAULT_DEAD_BAND_SOC_DIFFERENCE) {
			return Context.getSocState(context.soc, context.targetSoc);
		}

		// Check time termination
		if (context.config.isSelfTermination()) {

			final var atTargetEpochSeconds = this.getLatestTargetEpochSecondsOrZero(context.getParent());

			// AtTargetEpochSeconds not set
			if (atTargetEpochSeconds == 0) {
				return State.AT_TARGET_SOC;
			}

			var targetTimeBuffer = 0;
			if (context.considerTargetTime()) {
				targetTimeBuffer = context.config.getTargetTimeBuffer();
			}

			var teminationTime = Instant.ofEpochSecond(atTargetEpochSeconds) //
					.plus(targetTimeBuffer, ChronoUnit.MINUTES) //
					.plus(context.config.getTerminationBuffer(), ChronoUnit.MINUTES);

			// Fallback termination time is reached
			if (Instant.now(context.clock).isAfter(teminationTime)) {
				context.getParent().resetController();
			}
		}

		return State.AT_TARGET_SOC;
	}

	private void setAtTargetEpochSeconds(Clock clock, AbstractFixStateOfCharge parent) {

		final var atTargetEpochSeconds = this.getLatestTargetEpochSecondsOrZero(parent);

		// At target epoch seconds already set
		if (atTargetEpochSeconds > 0) {
			return;
		}

		var epochSeconds = Instant.now(clock).getEpochSecond();
		parent._setAtTargetEpochSeconds(epochSeconds);
	}

	private long getLatestTargetEpochSecondsOrZero(AbstractFixStateOfCharge parent) {

		if (parent.getAtTargetEpochSeconds().isDefined()) {
			return parent.getAtTargetEpochSeconds().orElse(0L);
		}

		// Use latest valid value if not defined
		return ControllerEssFixStateOfChargeImpl //
				.getLastValidValue(parent.getAtTargetEpochSecondsChannel()) //
				.orElse(0L);
	}
}
