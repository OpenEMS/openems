package io.openems.edge.ess.sinexcel.statemachine;

import com.google.common.base.CaseFormat;

import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.statemachine.AbstractStateMachine;
import io.openems.edge.common.statemachine.StateHandler;

public class StateMachine extends AbstractStateMachine<StateMachine.State, Context> {

	public enum State implements io.openems.edge.common.statemachine.State<State>, OptionsEnum {

		UNDEFINED(-1), //

		TOTAL_ONGRID(1), //
		
		ERROR(2), //
		START(3), //
		STOP(4), //
		GROUNDSET(5), //		

		TOTAL_OFFGRID(6) //
		; //

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
			return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.name());
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

	public StateMachine(State initialState) {
		super(initialState);
	}

	@Override
	public StateHandler<State, Context> getStateHandler(State state) {
		switch (state) {

		case TOTAL_ONGRID:
			return new TotalOnGridHandler();
			
		case TOTAL_OFFGRID:
			return new TotalOffGridHandler();
			
		case UNDEFINED:
			return new UndefinedHandler();
		case ERROR:
			return new ErrorHandler();
		case GROUNDSET:
			return new GroundSetHandler();
		case START:
			return new StartInverterHandler();
		case STOP:
			return new StopInverterHandler();
		}

		throw new IllegalArgumentException("Unknown State [" + state + "]");
	}

	

	/**
	 * The states which is used in the sinexcel switch from On-grid mode to off-grid
	 * mode and vice-versa. This switching needs total to 8 states, because state
	 * transition is decided with 3 digital inputs
	 * 
	 * <table border="1">
	 * 
	 * <tr>
	 * <td>DI1</td>
	 * <td>DI2</td>
	 * <td>DI3</td>
	 * <td>State</td>
	 * </tr>
	 * <tr>
	 * <td>0</td>
	 * <td>0</td>
	 * <td>0</td>
	 * <td>State</td>
	 * </tr>
	 * 
	 * <tr>
	 * <td>0</td>
	 * <td>0</td>
	 * <td>1</td>
	 * <td>State</td>
	 * </tr>
	 * 
	 * <tr>
	 * <td>0</td>
	 * <td>1</td>
	 * <td>0</td>
	 * <td>State</td>
	 * </tr>
	 * 
	 * <tr>
	 * <td>0</td>
	 * <td>1</td>
	 * <td>1</td>
	 * <td>State</td>
	 * </tr>
	 * 
	 * <tr>
	 * <td>1</td>
	 * <td>0</td>
	 * <td>0</td>
	 * <td>State</td>
	 * </tr>
	 * 
	 * <tr>
	 * <td>1</td>
	 * <td>0</td>
	 * <td>1</td>
	 * <td>State</td>
	 * </tr>
	 * 
	 * <tr>
	 * <td>1</td>
	 * <td>1</td>
	 * <td>0</td>
	 * <td>State</td>
	 * </tr>
	 * 
	 * <tr>
	 * <td>1</td>
	 * <td>1</td>
	 * <td>1</td>
	 * <td>State</td>
	 * </tr>
	 * </table>
	 */
}
