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
import io.openems.edge.controller.evse.single.EvseSingleState;
import io.openems.edge.evse.api.common.ApplyPhaseSwitch;
import io.openems.edge.evse.api.common.ApplyPhaseSwitch.PhaseSwitchAbility;
import io.openems.edge.evse.api.common.ApplyPhaseSwitch.PhaseSwitchAbility.Internal;
import io.openems.edge.evse.api.common.ApplyPhaseSwitch.PhaseSwitchAbility.ManualWithoutZeroSetPoint;

public abstract sealed class PhaseSwitchHandler extends StateHandler<EvseSingleState, Context> {

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
	private EvseSingleState state;

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

	private EvseSingleState mapPhaseSwitchDirection() {
		return switch (this.action.direction()) {
		case TO_SINGLE_PHASE -> EvseSingleState.PHASE_SWITCH_TO_SINGLE_PHASE;
		case TO_THREE_PHASE -> EvseSingleState.PHASE_SWITCH_TO_THREE_PHASE;
		};
	}

	@Override
	protected EvseSingleState runAndGetNextState(Context context) throws OpenemsNamedException {
		final var nextSubState = this.getNextSubState(context);
		if (nextSubState == SubStateMachine.State.FINISHED) {
			return EvseSingleState.CHARGING;
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
		case ENTRY -> this.handleEntry(context);
		case ENSURE_CHARGE -> this.handleEnsureCharge(context);
		case STOP_CHARGE -> this.handleStopCharge(context);
		case PHASE_SWITCH_INTERNAL -> this.handlePhaseSwitchInternal(context);
		case PHASE_SWITCH_MANUAL -> this.handlePhaseSwitchManual(context);
		case PHASE_SWITCH_MANUAL_WITHOUT_ZERO -> this.handlePhaseSwitchManualWithoutZero(context);
		case START_CHARGE -> this.handleStartCharge(context);
		case FINISHED -> SubStateMachine.State.FINISHED;
		};
	}

	private SubStateMachine.State handleEntry(Context context) {
		return switch (this.action.ability()) {
		case Internal ignored -> {
			final var targetPhase = this.getTargetPhase();
			final var phaseSwitch = context.actions.abilities().phaseSwitch() != null
					&& context.actions.abilities().phaseSwitch().direction() == this.action.direction() //
							? this.action //
							: null;
			context.applyAdjustedActions(b -> b //
					.setPhaseSwitch(phaseSwitch) //
					.setApplyInternalPhaseSwitchPower(targetPhase.count));
			yield SubStateMachine.State.PHASE_SWITCH_INTERNAL;
		}
		case ManualWithoutZeroSetPoint ignored -> SubStateMachine.State.ENSURE_CHARGE;
		case null, default -> SubStateMachine.State.STOP_CHARGE;
		};
	}

	private void applyEnsureChargeActions(Context context) {
		context.applyAdjustedActions(b -> b //
				.setApplyMinSetPoint() //
				.setPhaseSwitch(null));
	}

	private void applyManualPhaseSwitchActions(Context context, ApplyPhaseSwitch phaseSwitch) {
		context.applyAdjustedActions(b -> {
			if (this.action.ability() instanceof PhaseSwitchAbility.Manual) {
				b.setApplyZeroSetPoint();
			}
			b.setPhaseSwitch(phaseSwitch);
		});
	}

	private SubStateMachine.State handleEnsureCharge(Context context) {
		return switch (this.subStateMachine.getPhase(context,
				() -> context.chargePoint.getActivePower().orElse(0) > 100)) {
		case DEAD_TIME, PREDICATE_FALSE -> {
			this.applyEnsureChargeActions(context);
			yield SubStateMachine.State.ENSURE_CHARGE;
		}
		case PREDICATE_TRUE -> SubStateMachine.State.PHASE_SWITCH_MANUAL_WITHOUT_ZERO;
		case TIMEOUT_PASSED -> SubStateMachine.State.FINISHED;
		};
	}

	private SubStateMachine.State handleStopCharge(Context context) {
		return switch (this.subStateMachine.getPhase(context,
				() -> context.chargePoint.getActivePower().orElse(MAX_VALUE) < 100)) {
		case DEAD_TIME, PREDICATE_FALSE -> {
			this.applyManualPhaseSwitchActions(context, null);
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

	private boolean isInternalPhaseSwitchCompleted(Context context) {
		return this.isPhaseSwitchCompleted(context);
	}

	private SubStateMachine.State handlePhaseSwitchInternal(Context context) {
		return switch (this.subStateMachine.getPhase(context, () -> this.isInternalPhaseSwitchCompleted(context))) {
		case DEAD_TIME, PREDICATE_FALSE -> {
			final var targetPhase = this.getTargetPhase();
			final var phaseSwitch = context.actions.abilities().phaseSwitch() != null
					&& context.actions.abilities().phaseSwitch().direction() == this.action.direction() //
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
				this.applyManualPhaseSwitchActions(context, this.action);
			} else {
				this.applyManualPhaseSwitchActions(context, null);
			}
			yield SubStateMachine.State.PHASE_SWITCH_MANUAL;
		}
		case PREDICATE_TRUE -> SubStateMachine.State.START_CHARGE;
		case TIMEOUT_PASSED -> SubStateMachine.State.FINISHED;
		};
	}

	private SubStateMachine.State handlePhaseSwitchManualWithoutZero(Context context) {
		return switch (this.subStateMachine.getPhaseWithoutDeadTime(context,
				() -> this.isPhaseSwitchCompleted(context))) {
		case PREDICATE_FALSE -> {
			if (context.actions.abilities().phaseSwitch() != null
					&& context.actions.abilities().phaseSwitch().direction() == this.action.direction()) {
				this.applyManualPhaseSwitchActions(context, this.action);
			} else {
				this.applyManualPhaseSwitchActions(context, null);
			}
			yield SubStateMachine.State.PHASE_SWITCH_MANUAL_WITHOUT_ZERO;
		}
		case PREDICATE_TRUE, TIMEOUT_PASSED -> {
			context.applyAdjustedActions(b -> b.setPhaseSwitch(null));
			yield SubStateMachine.State.FINISHED;
		}
		case DEAD_TIME -> throw new IllegalStateException("Dead-time is not used for ManualWithoutZeroSetPoint.");
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

		public Phase getPhaseWithoutDeadTime(Context context, BooleanSupplier predicate) {
			if (this.lastChange == null) { // handle race condition
				this.lastChange = Instant.now(context.clock);
			}

			final var duration = Duration.between(this.lastChange, Instant.now(context.clock)).toSeconds();
			final Phase result;
			if (duration >= TIMEOUT_SECONDS) {
				context.setPhaseSwitchFailed.accept(true); // Phase-Switch failed
				result = Phase.TIMEOUT_PASSED;
			} else if (predicate.getAsBoolean()) {
				result = Phase.PREDICATE_TRUE;
			} else {
				result = Phase.PREDICATE_FALSE;
			}
			this.debugLog = "-" + EnumUtils.nameAsCamelCase(this.activeState) //
					+ "-" + EnumUtils.nameAsCamelCase(result) //
					+ "-" + duration + "s";
			return result;
		}

		private enum State {
			ENTRY, //
			ENSURE_CHARGE, //
			STOP_CHARGE, //
			PHASE_SWITCH_MANUAL, //
			PHASE_SWITCH_MANUAL_WITHOUT_ZERO, //
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
