package io.openems.edge.controller.evse.single.statemachine;

import static io.openems.edge.controller.evse.single.Utils.CHARGE_THRESHOLD_IN_WATT;

import java.time.Instant;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.timedata.DurationUnit;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.evse.single.EvseSingleState;
import io.openems.edge.controller.evse.single.Mode;

public class EvConnectedHandler extends StateHandler<EvseSingleState, Context> {

	private static final DurationUnit INITIAL_CHARGE_DELAY = DurationUnit.ofSeconds(15);
	private static final DurationUnit MAX_WAIT_FOR_CONSUMPTION_DELAY = DurationUnit.ofSeconds(60);

	private Instant initialChargeStartTime;

	@Override
	protected void onEntry(Context context) throws OpenemsError.OpenemsNamedException {
		this.initialChargeStartTime = context.clock.instant();
	}

	@Override
	protected void onExit(Context context) throws OpenemsError.OpenemsNamedException {
		this.initialChargeStartTime = null;
	}

	@Override
	public EvseSingleState runAndGetNextState(Context context) {
		if (!context.actions.abilities().isEvConnected()) {
			return EvseSingleState.UNDEFINED;
		}

		context.applyMinSetPointActions();

		final var minimumWaitDate = this.initialChargeStartTime.plus(INITIAL_CHARGE_DELAY.getDuration());
		if (context.clock.instant().isAfter(minimumWaitDate)) {
			var activePower = context.chargePoint.getActivePower();
			if (activePower.isDefined() && activePower.get() > CHARGE_THRESHOLD_IN_WATT) {
				if (context.actions.applySetPoint() != null && context.actions.applySetPoint().value() > 0) {
					return EvseSingleState.CHARGING;
				} else {
					return context.mode == Mode.ZERO //
							? EvseSingleState.CHARGE_DISABLED
							: EvseSingleState.CHARGE_PAUSED;
				}
			}
		}

		final var maximumWaitDate = this.initialChargeStartTime.plus(MAX_WAIT_FOR_CONSUMPTION_DELAY.getDuration());
		if (context.clock.instant().isAfter(maximumWaitDate)) {
			return EvseSingleState.FINISHED_EV_STOP;
		}

		return EvseSingleState.EV_CONNECTED;
	}
}
