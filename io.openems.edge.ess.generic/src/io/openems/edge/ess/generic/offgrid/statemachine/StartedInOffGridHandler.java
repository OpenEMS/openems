package io.openems.edge.ess.generic.offgrid.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.OffGridBatteryInverter;
import io.openems.edge.batteryinverter.api.OffGridBatteryInverter.TargetGridMode;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.ess.generic.common.GenericManagedEss;
import io.openems.edge.ess.generic.offgrid.statemachine.StateMachine.OffGridState;
import io.openems.edge.ess.offgrid.api.OffGridSwitch;
import io.openems.edge.ess.offgrid.api.OffGridSwitch.Contactor;

public class StartedInOffGridHandler extends StateHandler<OffGridState, Context> {

	private Instant lastAttempt = Instant.MIN;

	private static final int TARGET_GRID_FREQUENCY = 52; // Hz

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.lastAttempt = Instant.now();
		System.out.println(this.lastAttempt);
	}

	@Override
	public OffGridState runAndGetNextState(Context context) throws OpenemsNamedException {
		final GenericManagedEss ess = context.getParent();
		final Battery battery = context.battery;
		final OffGridBatteryInverter inverter = context.batteryInverter;
		final OffGridSwitch offGridSwitch = context.offGridSwitch;

		if (ess.hasFaults()) {
			return OffGridState.UNDEFINED;
		}

		if (!battery.isStarted()) {
			return OffGridState.UNDEFINED;
		}

		if (!inverter.isStarted()) {
			return OffGridState.UNDEFINED;
		}

		// TODO move this logic to GridSwitch
		// Grid is On?
		if (offGridSwitch.getGridMode() == GridMode.ON_GRID) {
			Instant now = Instant.now();
			// Just hard coded 65 sec waiting, i.e. more than 1 minute of the external timer
			long waitingSeconds = 65;
			boolean isWaitingTimePassed = Duration.between(this.lastAttempt, now).getSeconds() > waitingSeconds;
			if (isWaitingTimePassed) {
				// Crucial point is: Inverter grid mode should set before Grid Switch sets the
				// contactors in desired position
				inverter.setTargetGridMode(TargetGridMode.GO_ON_GRID);
				if (inverter.getGridMode() == GridMode.ON_GRID) {
					return OffGridState.UNDEFINED;
				}
			} else {
				ess._setGridMode(GridMode.UNDEFINED);
				return OffGridState.STARTED_IN_OFF_GRID;
			}
		}

		// Allowed discharge reduces to 0, becuase target gridmode changes the inverter
		// from RUNNING to GO_RUNNING state
		if (ess.getAllowedDischargePower().orElse(0) == 0 && ess.getGridMode() == GridMode.OFF_GRID) {
			Instant now = Instant.now();
			long waitingSeconds = 5;
			boolean isWaitingTimePassed = Duration.between(this.lastAttempt, now).getSeconds() > waitingSeconds;
			if (isWaitingTimePassed) {
				offGridSwitch.setGroundingContactor(Contactor.OPEN);
				offGridSwitch.setMainContactor(Contactor.OPEN);
				return OffGridState.STOP_BATTERY_INVERTER;
			}
		}

		inverter.setTargetOffGridFrequency(TARGET_GRID_FREQUENCY);
		ess._setStartStop(StartStop.START);
		ess._setGridMode(GridMode.OFF_GRID);
		return OffGridState.STARTED_IN_OFF_GRID;
	}
}
