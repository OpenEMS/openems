package io.openems.edge.controller.evse.single.statemachine;

import static io.openems.edge.common.type.Phase.SingleOrThreePhase.SINGLE_PHASE;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;
import static java.lang.Integer.MAX_VALUE;

import java.time.Duration;
import java.time.Instant;
import java.util.function.BooleanSupplier;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.EnumUtils;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.evse.single.statemachine.StateMachine.State;
import io.openems.edge.evse.api.chargepoint.Profile;
import io.openems.edge.evse.api.common.ApplyPhaseSwitch;
import io.openems.edge.evse.api.common.ApplyPhaseSwitch.PhaseSwitchAbility;

public abstract sealed class PhaseSwitchHandler extends StateHandler<State, Context> {

	public static final class ToSinglePhase extends PhaseSwitchHandler {
		public ToSinglePhase() {
			super();
		}
	}

	public static final class ToThreePhase extends PhaseSwitchHandler {
		public ToThreePhase() {
			super();
		}
	}

	private ApplyPhaseSwitch action;
	private State state;

	private SubStateMachine subStateMachine;

	protected PhaseSwitchHandler() {
		this.subStateMachine = new SubStateMachine();
	}

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.action = context.actions.phaseSwitch();
		this.state = this.mapPhaseSwitchDirection();
		this.subStateMachine.setNextSubState(SubStateMachine.State.ENTRY, context);
		context.setPhaseSwitchFailed.accept(false);
	}

	private State mapPhaseSwitchDirection() {
		return switch (this.action.direction()) {
		case TO_SINGLE_PHASE -> State.PHASE_SWITCH_TO_SINGLE_PHASE;
		case TO_THREE_PHASE -> State.PHASE_SWITCH_TO_THREE_PHASE;
		};
	}

	@Override
	protected State runAndGetNextState(Context context) throws OpenemsNamedException {
		final var nextSubState = this.getNextSubState(context);
		if (nextSubState == SubStateMachine.State.FINISHED) {
			return State.CHARGING;
		}
		this.subStateMachine.setNextSubState(nextSubState, context);
		return this.state;
	}

	@Override
	protected String debugLog() {
		return this.state.asCamelCase() + this.subStateMachine.debugLog;
	}

	private SubStateMachine.State getNextSubState(Context context) {
		return switch (this.subStateMachine.activeState) {
		case ENTRY -> this.handleEntry();
		case STOP_CHARGE -> this.handleStopCharge(context);
		case PHASE_SWITCH_INTERNAL -> this.handlePhaseSwitchInternal(context);
		case PHASE_SWITCH_MANUAL -> this.handlePhaseSwitchManual(context);
		case START_CHARGE -> this.handleStartCharge(context);
		case FINISHED -> SubStateMachine.State.FINISHED;
		};
	}

	private SubStateMachine.State handleEntry() {
		return switch (this.action.ability()) {
		case PhaseSwitchAbility.Internal() -> SubStateMachine.State.PHASE_SWITCH_INTERNAL;
		case PhaseSwitchAbility.Manual() -> SubStateMachine.State.STOP_CHARGE;
		};
	}

	private SubStateMachine.State handleStopCharge(Context context) {
		return switch (this.subStateMachine.getPhase(context,
				() -> context.chargePoint.getActivePower().orElse(MAX_VALUE) < 100)) {
		case DEAD_TIME, PREDICATE_FALSE -> {
			context.applyAdjustedActions(b -> b //
					.setApplyZeroSetPoint() //
					.setPhaseSwitch(null));
			yield SubStateMachine.State.STOP_CHARGE;
		}
		case PREDICATE_TRUE -> SubStateMachine.State.PHASE_SWITCH_MANUAL;
		case TIMEOUT_PASSED -> SubStateMachine.State.FINISHED;
		};
	}

	private io.openems.edge.common.type.Phase.SingleOrThreePhase getTargetPhase() {
		return switch (this.action.direction()) {
		case TO_SINGLE_PHASE -> SINGLE_PHASE;
		case TO_THREE_PHASE -> THREE_PHASE;
		};
	}

	private boolean isPhaseSwitchCompleted(Context context) {
		return context.actions.abilities().applySetPoint().phase() == this.getTargetPhase();
	}

	private SubStateMachine.State handlePhaseSwitchInternal(Context context) {
		return switch (this.subStateMachine.getPhase(context, () -> this.isPhaseSwitchCompleted(context))) {
		case DEAD_TIME, PREDICATE_FALSE -> {
			final var targetPhase = this.getTargetPhase();
			final var phaseSwitch = context.actions.phaseSwitch() != null
					&& context.actions.phaseSwitch().direction() == this.action.direction() //
							? this.action //
							: null;
			context.applyAdjustedActions(b -> b //
					.setPhaseSwitch(phaseSwitch) //
					.setApplyInternalPhaseSwitchPower(targetPhase.count));
			yield SubStateMachine.State.PHASE_SWITCH_INTERNAL;
		}
		case PREDICATE_TRUE, TIMEOUT_PASSED -> SubStateMachine.State.FINISHED;
		};
	}

	private SubStateMachine.State handlePhaseSwitchManual(Context context) {
		return switch (this.subStateMachine.getPhase(context, () -> this.isPhaseSwitchCompleted(context))) {
		case DEAD_TIME, PREDICATE_FALSE -> {
			if (context.actions.abilities().phaseSwitch() != null
					&& context.actions.abilities().phaseSwitch().direction() == this.action.direction()) {
				context.applyAdjustedActions(b -> b //
						.setApplyZeroSetPoint() //
						.setPhaseSwitch(this.action));
			} else {
				context.applyAdjustedActions(Profile.ChargePointActions.Builder::setApplyZeroSetPoint);
			}
			yield SubStateMachine.State.PHASE_SWITCH_MANUAL;
		}
		case PREDICATE_TRUE -> SubStateMachine.State.START_CHARGE;
		case TIMEOUT_PASSED -> SubStateMachine.State.FINISHED;
		};
	}

	private SubStateMachine.State handleStartCharge(Context context) {
		return switch (this.subStateMachine.getPhase(context)) {
		case DEAD_TIME, PREDICATE_FALSE -> {
			context.applyAdjustedActions(b -> b //
					.setApplyMinSetPoint() //
					.setPhaseSwitch(null));
			yield SubStateMachine.State.START_CHARGE;
		}
		case PREDICATE_TRUE, TIMEOUT_PASSED -> SubStateMachine.State.FINISHED;

		};
	}

	private class SubStateMachine {
		private static final int DEAD_TIME_SECONDS = 30;
		private static final int TIMEOUT_SECONDS = 600;

		private State activeState = State.STOP_CHARGE;
		private Instant lastChange;

		// Additional info for debugLog
		protected String debugLog = "";

		public void setNextSubState(State state, Context context) {
			if (this.activeState == state) {
				return;
			}
			this.activeState = state;
			this.lastChange = Instant.now(context.clock);
		}

		public Phase getPhase(Context context) {
			return this.getPhase(context, () -> true);
		}

		public Phase getPhase(Context context, BooleanSupplier predicate) {
			if (this.lastChange == null) { // handle race condition
				this.lastChange = Instant.now(context.clock);
			}

			final var duration = Duration.between(this.lastChange, Instant.now(context.clock)).toSeconds();
			final Phase result;
			if (duration >= TIMEOUT_SECONDS) {
				context.setPhaseSwitchFailed.accept(true); // Phase-Switch failed
				result = Phase.TIMEOUT_PASSED;
			} else if (duration >= DEAD_TIME_SECONDS) {
				if (predicate.getAsBoolean()) {
					result = Phase.PREDICATE_TRUE;
				} else {
					result = Phase.PREDICATE_FALSE;
				}
			} else {
				result = Phase.DEAD_TIME;
			}
			this.debugLog = "-" + EnumUtils.nameAsCamelCase(this.activeState) //
					+ "-" + EnumUtils.nameAsCamelCase(result) //
					+ "-" + duration + "s";
			return result;
		}

		private enum State {
			ENTRY, //
			STOP_CHARGE, //
			PHASE_SWITCH_MANUAL, //
			PHASE_SWITCH_INTERNAL, //
			START_CHARGE, //
			FINISHED, //
		}

		private enum Phase {
			DEAD_TIME, //
			PREDICATE_FALSE, //
			PREDICATE_TRUE, //
			TIMEOUT_PASSED //
		}
	}

}
