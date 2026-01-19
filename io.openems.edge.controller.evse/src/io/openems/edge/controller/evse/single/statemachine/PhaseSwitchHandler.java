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

	private SubStateMachine subStateMachine;

	protected PhaseSwitchHandler(Profile.PhaseSwitch action) {
		this.subStateMachine = new SubStateMachine();
		this.action = action;
		this.state = switch (this.action) {
		case TO_SINGLE_PHASE -> State.PHASE_SWITCH_TO_SINGLE_PHASE;
		case TO_THREE_PHASE -> State.PHASE_SWITCH_TO_THREE_PHASE;
		};
	}

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.subStateMachine.setNextSubState(SubStateMachine.State.STOP_CHARGE, context);
		context.setPhaseSwitchFailed.accept(false); // Reset StateChannel
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

	private SubStateMachine.State getNextSubState(Context context) throws OpenemsNamedException {
		return switch (this.subStateMachine.activeState) {
		/*
		 * Initially stop charging.
		 */
		case STOP_CHARGE -> {
			yield switch (this.subStateMachine.getPhase(context,
					// Predicate tests if EV is charging
					() -> context.chargePoint.getActivePower().orElse(MAX_VALUE) < 100)) {
			case DEAD_TIME, PREDICATE_FALSE -> {
				// Keep stopping charging by applying zero set-point
				context.applyAdjustedActions(b -> b //
						.setApplyZeroSetPoint() //
						.setPhaseSwitch(null));
				yield SubStateMachine.State.STOP_CHARGE;
			}
			case PREDICATE_TRUE -> {
				// No Charge-Power anymore -> start PhaseSwitch
				yield SubStateMachine.State.PHASE_SWITCH;
			}
			case TIMEOUT_PASSED -> {
				yield SubStateMachine.State.FINISHED;
			}
			};
		}

		/*
		 * Apply actual phase-switch
		 */
		case PHASE_SWITCH -> {
			yield switch (this.subStateMachine.getPhase(context, () -> {
				// Predicate tests if phase-switch completed
				final var activePhase = context.actions.abilities().applySetPoint().phase();
				final var targetPhase = switch (this.action) {
				case TO_SINGLE_PHASE -> SINGLE_PHASE;
				case TO_THREE_PHASE -> THREE_PHASE;
				};
				return activePhase == targetPhase;
			})) {
			case DEAD_TIME, PREDICATE_FALSE -> {
				// Actually apply phase-switch action in ChargePoint; still with zero set-point
				if (context.actions.abilities().phaseSwitch() == this.action) {
					// While specific PhaseSwitch-Action is available
					context.applyAdjustedActions(b -> b //
							.setApplyZeroSetPoint() //
							.setPhaseSwitch(this.action));
				} else {
					context.applyAdjustedActions(b -> b //
							.setApplyZeroSetPoint());
				}
				yield SubStateMachine.State.PHASE_SWITCH;
			}
			case PREDICATE_TRUE -> {
				// PhaseSwitch completed -> re-start charging
				yield SubStateMachine.State.START_CHARGE;
			}
			case TIMEOUT_PASSED -> {
				yield SubStateMachine.State.FINISHED;
			}
			};
		}

		/*
		 * Finished phase-switch; re-start charging
		 */
		case START_CHARGE -> {
			yield switch (this.subStateMachine.getPhase(context)) {
			case DEAD_TIME, PREDICATE_FALSE -> {
				// Allow Charge with Min-Set-Point (but EV might not be connected)
				context.applyAdjustedActions(b -> b //
						.setApplyMinSetPoint() //
						.setPhaseSwitch(null));
				yield SubStateMachine.State.START_CHARGE;
			}
			case PREDICATE_TRUE, TIMEOUT_PASSED -> {
				yield SubStateMachine.State.FINISHED;
			}
			};
		}

		case FINISHED -> {
			yield SubStateMachine.State.FINISHED;
		}
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
			STOP_CHARGE, // Stop Charge
			PHASE_SWITCH, // Execute Phase-Switch
			START_CHARGE, // Start Charge
			FINISHED, // FINISHED
		}

		private enum Phase {
			DEAD_TIME, // Dead-Time is active: minimum time to wait for applied actions
			PREDICATE_FALSE, // Dead-Time has passed and Predicate is true
			PREDICATE_TRUE, // Dead-Time has passed and Predicate is false
			TIMEOUT_PASSED // Timeout reached
		}
	}

}
