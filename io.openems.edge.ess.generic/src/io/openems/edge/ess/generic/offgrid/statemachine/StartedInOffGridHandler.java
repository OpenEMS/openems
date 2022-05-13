package io.openems.edge.ess.generic.offgrid.statemachine;

import java.time.Duration;
import java.time.LocalTime;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.batteryinverter.api.OffGridBatteryInverter.TargetGridMode;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.ess.generic.offgrid.statemachine.StateMachine.OffGridState;
import io.openems.edge.ess.offgrid.api.OffGridSwitch.Contactor;

public class StartedInOffGridHandler extends StateHandler<OffGridState, Context> {

	public static LocalTime lastStateChange;
	private boolean isLastChangeChecked = false;
	private static final int TARGET_GRID_FREQUENCY = 52; // Hz

	@Override
	public OffGridState runAndGetNextState(Context context) throws OpenemsNamedException {
		final var ess = context.getParent();
		final var battery = context.battery;
		final var inverter = context.batteryInverter;
		final var offGridSwitch = context.offGridSwitch;

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
		if (offGridSwitch.getGridMode() == GridMode.ON_GRID && !this.isLastChangeChecked) {
			lastStateChange = LocalTime.now(context.componentManager.getClock());
			this.isLastChangeChecked = true;
		} else if (offGridSwitch.getGridMode() == GridMode.ON_GRID) {
			if (LocalTime.now(context.componentManager.getClock()).minus(Duration.ofSeconds(60))
					.isAfter(lastStateChange)) {
				lastStateChange = LocalTime.now(context.componentManager.getClock());
				this.isLastChangeChecked = false;
				// Crucial point is: Inverter grid mode should set before Grid Switch sets the
				// contactors in desired position
				inverter.setTargetGridMode(TargetGridMode.GO_ON_GRID);
				return OffGridState.STOP_BATTERY_INVERTER_BEFORE_SWITCH;
			}
			ess._setGridMode(GridMode.UNDEFINED);
			return OffGridState.STARTED_IN_OFF_GRID;
		} else if (offGridSwitch.getGridMode() == GridMode.OFF_GRID && this.isLastChangeChecked) {
			this.isLastChangeChecked = false;
		}

		// Allowed discharge reduces to 0, because target gridmode changes the inverter
		// from RUNNING to GO_RUNNING state
		if (ess.getAllowedDischargePower().orElse(0) == 0 && ess.getGridMode() == GridMode.OFF_GRID
				&& ess.getSoc().get() < 5) {
			offGridSwitch.setGroundingContactor(Contactor.OPEN);
			offGridSwitch.setMainContactor(Contactor.OPEN);
			return OffGridState.STOP_BATTERY_INVERTER;
		}

		inverter.setTargetOffGridFrequency(TARGET_GRID_FREQUENCY);
		ess._setStartStop(StartStop.START);
		ess._setGridMode(GridMode.OFF_GRID);
		return OffGridState.STARTED_IN_OFF_GRID;
	}
}
