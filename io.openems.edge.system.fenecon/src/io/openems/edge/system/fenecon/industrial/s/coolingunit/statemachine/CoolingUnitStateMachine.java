package io.openems.edge.system.fenecon.industrial.s.coolingunit.statemachine;

import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.statemachine.AbstractStateMachine;
import io.openems.edge.common.statemachine.StateHandler;

public class CoolingUnitStateMachine
		extends AbstractStateMachine<CoolingUnitStateMachine.CoolingUnitState, CoolingUnitContext> {

	public enum CoolingUnitState implements io.openems.edge.common.statemachine.State<CoolingUnitState>, OptionsEnum {
		UNDEFINED(-1), //
		START_COOLING(0), //
		STOP_COOLING(1), //
		WAIT_FOR_START_REQUEST(2), //
		WAIT_FOR_STOP_REQUEST(3),//
		;

		private final int value;

		private CoolingUnitState(int value) {
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
		public CoolingUnitState[] getStates() {
			return CoolingUnitState.values();
		}
	}

	public CoolingUnitStateMachine(CoolingUnitState initialState) {
		super(initialState);
	}

	@Override
	public StateHandler<CoolingUnitState, CoolingUnitContext> getStateHandler(CoolingUnitState state) {
		return switch (state) {
		case UNDEFINED -> new UndefinedHandler();
		case START_COOLING -> new StartCoolingHandler();
		case STOP_COOLING -> new StopCoolingHandler();
		case WAIT_FOR_START_REQUEST -> new WaitForStartRequestHandler();
		case WAIT_FOR_STOP_REQUEST -> new WaitForStopRequestHandler();
		};
	}
}