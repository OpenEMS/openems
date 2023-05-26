package io.openems.edge.battery.protection.force;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.statemachine.AbstractContext;
import io.openems.edge.common.statemachine.AbstractStateMachine;
import io.openems.edge.common.statemachine.StateHandler;

public abstract class AbstractForceChargeDischarge
		extends AbstractStateMachine<AbstractForceChargeDischarge.State, AbstractForceChargeDischarge.Context> {

	protected static final int WAIT_FOR_FORCE_MODE_SECONDS = 60;

	public static class Context extends AbstractContext<OpenemsComponent> {

		private final ClockProvider clockProvider;
		private final Integer minCellVoltage;
		private final Integer maxCellVoltage;

		public Context(ClockProvider clockProvider, Integer minCellVoltage, Integer maxCellVoltage) {
			this.clockProvider = clockProvider;
			this.minCellVoltage = minCellVoltage;
			this.maxCellVoltage = maxCellVoltage;
		}

		protected Instant now() {
			return Instant.now(this.clockProvider.getClock());
		}
	}

	public enum State implements io.openems.edge.common.statemachine.State<State>, OptionsEnum {
		UNDEFINED(-1), //

		WAIT_FOR_FORCE_MODE(10), //
		FORCE_MODE(11), //
		BLOCK_MODE(12), //
		;

		private final int value;

		private State(int value) {
			this.value = value;
		}

		@Override
		public int getValue() {
			return this.value;
		}

		@Override
		public String getName() {
			return this.name();
		}

		@Override
		public OptionsEnum getUndefined() {
			return UNDEFINED;
		}

		@Override
		public State[] getStates() {
			return State.values();
		}
	}

	public AbstractForceChargeDischarge() {
		super(State.UNDEFINED);
	}

	@Override
	public StateHandler<State, Context> getStateHandler(State state) {
		
		return switch (state) {
		case UNDEFINED ->
			 new StateHandler<>() {
				@Override
				protected State runAndGetNextState(Context context) throws OpenemsNamedException {
					return AbstractForceChargeDischarge.this.handleUndefinedState(context.minCellVoltage,
							context.maxCellVoltage);
				}
			};
		case WAIT_FOR_FORCE_MODE ->
			 new StateHandler<>() {
				private Instant enteredAt = Instant.MAX;
				@Override
				protected void onEntry(Context context) throws OpenemsNamedException {
					this.enteredAt = context.now();
				}
				@Override
				protected State runAndGetNextState(Context context) throws OpenemsNamedException {
					return AbstractForceChargeDischarge.this.handleWaitForForceModeState(context.minCellVoltage,
							context.maxCellVoltage, Duration.between(this.enteredAt, context.now()));
				}
			};
		case FORCE_MODE ->
			 new StateHandler<>() {
				@Override
				protected State runAndGetNextState(Context context) throws OpenemsNamedException {
					return AbstractForceChargeDischarge.this.handleForceModeState(context.minCellVoltage,
							context.maxCellVoltage);
				}
			};
		case BLOCK_MODE ->
			 new StateHandler<>() {
				@Override
				protected State runAndGetNextState(Context context) throws OpenemsNamedException {
					return AbstractForceChargeDischarge.this.handleBlockModeState(context.minCellVoltage,
							context.maxCellVoltage);
				}
			};
		default -> throw new IllegalArgumentException("Unknown State [" + state + "]");
		};
		
	}

	protected abstract State handleUndefinedState(int minCellVoltage, int maxCellVoltage);

	protected abstract State handleWaitForForceModeState(int minCellVoltage, int maxCellVoltage, Duration sinceStart);

	protected abstract State handleForceModeState(int minCellVoltage, int maxCellVoltage);

	protected abstract State handleBlockModeState(int minCellVoltage, int maxCellVoltage);

}