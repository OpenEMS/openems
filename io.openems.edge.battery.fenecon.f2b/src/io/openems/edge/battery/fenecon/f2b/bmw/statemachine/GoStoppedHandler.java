package io.openems.edge.battery.fenecon.f2b.bmw.statemachine;

import static io.openems.edge.common.channel.ChannelUtils.getValues;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.EnumUtils;
import io.openems.edge.battery.fenecon.f2b.bmw.BatteryFeneconF2bBmw;
import io.openems.edge.battery.fenecon.f2b.bmw.BatteryFeneconF2bBmwImpl.BatteryValues;
import io.openems.edge.battery.fenecon.f2b.bmw.enums.GoStoppedSubState;
import io.openems.edge.battery.fenecon.f2b.bmw.enums.HvContactorStatus;
import io.openems.edge.battery.fenecon.f2b.bmw.enums.InsulationMeasurement;
import io.openems.edge.battery.fenecon.f2b.bmw.statemachine.StateMachine.State;
import io.openems.edge.battery.fenecon.f2b.common.enums.F2bCanCommunication;
import io.openems.edge.battery.fenecon.f2b.common.enums.F2bTerminal15Sw;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.common.type.TypeUtils;

public class GoStoppedHandler extends StateHandler<State, Context> {

	private static final int TIMEOUT = 120; // [s], per SubState
	private static final int WAIT_UNTIL_BATTERY_IS_IN_STANDBY_STATE = 40;// [s]
	private static final int MAXIMUM_ALLOWED_CURRENT_TO_STOP_THE_BATTERY = 1;// [A]

	private final Logger log = LoggerFactory.getLogger(GoStoppedHandler.class);

	protected static record GoStoppedState(GoStoppedSubState subState, Instant lastChange) {
	}

	protected GoStoppedState goStoppedState;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.goStoppedState = new GoStoppedState(GoStoppedSubState.WAIT_FOR_CURRENT_REDUCTION,
				Instant.now(context.clock));
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		final var battery = context.getParent();

		// Handle the Sub-StateMachine
		var nextSubState = this.getNextSubState(context);
		battery.channel(BatteryFeneconF2bBmw.ChannelId.GO_RUNNING_STATE_MACHINE).setNextValue(nextSubState);

		final var now = Instant.now(context.clock);
		if (nextSubState != this.goStoppedState.subState) {
			// Record State changes
			this.goStoppedState = new GoStoppedState(nextSubState, now);
		} else if (this.goStoppedState.lastChange.isBefore(now.minusSeconds(TIMEOUT))) {
			// Handle GoStoppedHandler State-timeout
			throw new OpenemsException("Timeout [" + TIMEOUT + "s] in GoStoppped-" + this.goStoppedState.subState);
		}

		if (nextSubState == GoStoppedSubState.ERROR) {
			return State.ERROR;
		}

		if (nextSubState == GoStoppedSubState.FINISHED) {
			return State.STOPPED;
		}

		return State.GO_STOPPED;
	}

	private GoStoppedSubState getNextSubState(Context context) throws OpenemsNamedException {
		final var battery = context.getParent();
		return switch (this.goStoppedState.subState) {
		case WAIT_FOR_CURRENT_REDUCTION -> {
			if (TypeUtils.abs(battery.getCurrent().orElse(0)) >= MAXIMUM_ALLOWED_CURRENT_TO_STOP_THE_BATTERY) {
				context.logInfo(this.log, "Waiting for the battery current to be reduced...");
				yield GoStoppedSubState.WAIT_FOR_CURRENT_REDUCTION;
			}
			yield GoStoppedSubState.OPEN_HV_CONTACTORS;
		}
		case OPEN_HV_CONTACTORS -> {
			if (!context.isTerminal30cClosed()) {
				yield GoStoppedSubState.FINISHED;
			}

			if (battery.getHvContactorStatus().asEnum() == HvContactorStatus.CONTACTORS_OPENED) {
				yield GoStoppedSubState.POWER_OFF_F2B_TERMINAL_15_SW_AND_HW;
			} else {
				switch (battery.getContactorsDiagnosticStatus()) {
				case ONE_CONTACTOR_STUCK:// Negative StuckDown (at the same time):
				case TWO_CONTACTOR_STUCK:// Positive and Negative StuckDown (at the same time):
					battery._setHvContactorsStuckFailed(true);
					yield GoStoppedSubState.ERROR;
				case NO_CONTACTOR_STUCK:
					battery.setHvContactor(false);
					context.logWarn(this.log, "Waiting for Hv Contactors");
					yield GoStoppedSubState.OPEN_HV_CONTACTORS;
				case UNDEFINED:
					yield GoStoppedSubState.POWER_OFF_F2B_TERMINAL_15_SW_AND_HW;
				}
			}
			yield GoStoppedSubState.OPEN_HV_CONTACTORS;
		}
		case POWER_OFF_F2B_TERMINAL_15_SW_AND_HW -> {
			// TODO check if closed
			battery.setF2bTerminal15Sw(F2bTerminal15Sw.KL_30F_AND_KL_30C_ON);
			battery.setF2bTerminal15Hw(false);
			battery.setInsulationMeasurement(InsulationMeasurement.DO_NOT_PERFORM_MEASUREMENT);
			yield GoStoppedSubState.WAIT_FOURTY_SECONDS;
		}
		case WAIT_FOURTY_SECONDS -> {
			final var now = Instant.now(context.clock);
			if (now.minusSeconds(WAIT_UNTIL_BATTERY_IS_IN_STANDBY_STATE).isAfter(this.goStoppedState.lastChange)) {
				battery.setF2bCanCommunication(F2bCanCommunication.CAN_OFF);
				var anyValueDefined = getValues(battery, BatteryValues.class).isPresent();
				if (!anyValueDefined) {
					yield GoStoppedSubState.F2B_TERMINAL_30C_SWITCH_OFF;
				}
			}
			yield GoStoppedSubState.WAIT_FOURTY_SECONDS;
		}
		case F2B_TERMINAL_30C_SWITCH_OFF -> {
			// TODO watch-dog 10 second
			battery.setF2bTerminal30c(false);
			if (!context.isTerminal30cClosed()) {
				yield GoStoppedSubState.FINISHED;
			}
			yield GoStoppedSubState.F2B_TERMINAL_30C_SWITCH_OFF;

		}
		case FINISHED -> GoStoppedSubState.FINISHED;
		case ERROR, UNDEFINED -> GoStoppedSubState.ERROR;
		};
	}

	@Override
	protected String debugLog() {
		return State.GO_STOPPED.asCamelCase() + "-" + EnumUtils.nameAsCamelCase(this.goStoppedState.subState);
	}
}
