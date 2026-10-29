package io.openems.edge.controller.evse.single.statemachine;

import static io.openems.edge.controller.evse.single.Utils.CHARGE_THRESHOLD_IN_WATT;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.evse.single.EvseSingleState;
import io.openems.edge.controller.evse.single.Mode;

public class ChargeResumingHandler extends StateHandler<EvseSingleState, Context> {

	private static final Duration CHARGE_RESUMING_TIMEOUT = Duration.ofMinutes(2);

	private final Logger log = LoggerFactory.getLogger(ChargeResumingHandler.class);

	private Instant entryTime;

	@Override
	protected void onEntry(Context context) throws OpenemsError.OpenemsNamedException {
		super.onEntry(context);
		this.entryTime = context.clock.instant();
	}

	@Override
	public EvseSingleState runAndGetNextState(Context context) {
		if (!context.actions.abilities().isReadyForCharging() || !context.actions.abilities().isEvConnected()) {
			return EvseSingleState.UNDEFINED;
		}

		if (context.actions.applySetPoint() == null) {
			return EvseSingleState.CHARGE_RESUMING;
		}

		if (context.actions.applySetPoint().value() == 0) {
			return context.mode == Mode.ZERO ? EvseSingleState.CHARGE_DISABLED : EvseSingleState.CHARGE_PAUSED;
		}

		context.applyActions();

		final var activePower = context.chargePoint.getActivePower();
		if (activePower.isDefined() && activePower.get() > CHARGE_THRESHOLD_IN_WATT) {
			return EvseSingleState.CHARGING;
		} else if (this.hasReachedTimeout(context)) {
			this.log.info("Charge resuming timeout reached. Transitioning to FINISHED_EV_STOP state.");
			return EvseSingleState.FINISHED_EV_STOP;
		} else {
			return EvseSingleState.CHARGE_RESUMING;
		}
	}

	private boolean hasReachedTimeout(Context context) {
		return Duration.between(this.entryTime, context.clock.instant()).compareTo(CHARGE_RESUMING_TIMEOUT) > 0;
	}
}
