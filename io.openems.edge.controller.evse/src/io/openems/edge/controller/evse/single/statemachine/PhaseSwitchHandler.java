package io.openems.edge.controller.evse.single.statemachine;

import static io.openems.edge.common.type.Phase.SingleOrThreePhase.SINGLE_PHASE;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.EnumUtils;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.evse.single.statemachine.StateMachine.State;
import io.openems.edge.evse.api.chargepoint.Profile;

public abstract sealed class PhaseSwitchHandler extends StateHandler<State, Context> {

	public static final class ToSinglePhase extends PhaseSwitchHandler {
		public ToSinglePhase() {
			super(Profile.PhaseSwitch.TO_SINGLE_PHASE);
		}
	}

	public static final class ToThreePhase extends PhaseSwitchHandler {
		public ToThreePhase() {
			super(Profile.PhaseSwitch.TO_THREE_PHASE);
		}
	}

	private final Profile.PhaseSwitch action;
	private final State state;

	private PhaseSwitchSubState subState;

	protected PhaseSwitchHandler(Profile.PhaseSwitch action) {
		this.action = action;
		this.state = switch (this.action) {
		case TO_SINGLE_PHASE -> State.PHASE_SWITCH_TO_SINGLE_PHASE;
		case TO_THREE_PHASE -> State.PHASE_SWITCH_TO_THREE_PHASE;
		};
	}

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.subState = PhaseSwitchSubState.STOP_CHARGE;
	}

	@Override
	protected State runAndGetNextState(Context context) throws OpenemsNamedException {
		var nextSubState = this.getNextSubState(context);
		this.subState = nextSubState;

		if (this.subState == PhaseSwitchSubState.FINISHED) {
			return State.CHARGING;
		}

		return this.state;
	}

	@Override
	protected String debugLog() {
		return this.state.asCamelCase() + "-" + EnumUtils.nameAsCamelCase(this.subState);
	}

	private PhaseSwitchSubState getNextSubState(Context context) throws OpenemsNamedException {
		return switch (this.subState) {
		case STOP_CHARGE -> {
			if (context.chargePoint.getActivePower().orElse(0) < 100) {
				yield PhaseSwitchSubState.PHASE_SWITCH;
			}

			context.applyAdjustedActions(b -> b //
					.setApplyZeroSetPoint() //
					.setPhaseSwitch(null));
			yield PhaseSwitchSubState.STOP_CHARGE;
		}

		case PHASE_SWITCH -> {
			final var activePhase = context.actions.abilities().applySetPoint().phase();
			final var targetPhase = switch (this.action) {
			case TO_SINGLE_PHASE -> SINGLE_PHASE;
			case TO_THREE_PHASE -> THREE_PHASE;
			};
			if (activePhase == targetPhase) {
				yield PhaseSwitchSubState.START_CHARGE;
			}

			context.applyAdjustedActions(b -> b //
					.setApplyZeroSetPoint() //
					.setPhaseSwitch(this.action));
			yield PhaseSwitchSubState.PHASE_SWITCH;
		}

		case START_CHARGE -> {
			context.applyAdjustedActions(b -> b //
					.setPhaseSwitch(null));
			yield PhaseSwitchSubState.FINISHED;
		}

		case FINISHED -> {
			yield PhaseSwitchSubState.FINISHED;
		}
		};
	}

	public enum PhaseSwitchSubState {
		STOP_CHARGE, // Stop Charge
		PHASE_SWITCH, // Execute Phase-Switch
		START_CHARGE, // Start Charge
		FINISHED;
	}
}
