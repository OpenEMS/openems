package io.openems.edge.controller.ess.cycle.statemachine;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.cycle.CycleOrder;
import io.openems.edge.controller.ess.cycle.statemachine.StateMachine.State;

public class UndefinedHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) throws IllegalArgumentException, OpenemsNamedException {
		if (this.initializeTime(context)) {
			// Get state
			switch (this.getCycleOrder(context)) {
			case AUTO:
				return State.UNDEFINED;
			case START_WITH_CHARGE:
				return State.START_CHARGE;
			case START_WITH_DISCHARGE:
				return State.START_DISCHARGE;
			}
		}
		return State.UNDEFINED;
	}

	/**
	 * Helper to activate the full cycle activated or not.
	 *
	 * @param context the {@link Context}
	 * @return true if the Controller should be executed now
	 * @throws OpenemsNamedException throws Openems Named Exception.
	 */
	private boolean initializeTime(Context context) throws OpenemsNamedException {
		var time = LocalTime.now(context.componentManager.getClock());
		// TODO: it would be better to do the parsing of `startTime` once and not every
		// Cycle/Second.
		var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
		var localTime = LocalTime.parse(context.config.startTime(), formatter);
		if (time.isAfter(localTime.minusSeconds(1))) {
			if (time.isBefore(localTime.plusSeconds(59))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Gets the configured {@link CycleOrder} or automatically derives it from the
	 * ESS State-of-Charge.
	 *
	 * @param context the {@link Context}
	 * @return the {@link CycleOrder}
	 */
	private CycleOrder getCycleOrder(Context context) {
		switch (context.config.cycleOrder()) {
		case START_WITH_CHARGE:
			return CycleOrder.START_WITH_CHARGE;
		case START_WITH_DISCHARGE:
			return CycleOrder.START_WITH_DISCHARGE;
		case AUTO:
			int soc = context.ess.getSoc().orElse(0); // defaults to START_WITH_DISCHARGE
			if (soc < 50) {
				return CycleOrder.START_WITH_DISCHARGE;
			}
			return CycleOrder.START_WITH_CHARGE;
		}
		return CycleOrder.AUTO;
	}
}
