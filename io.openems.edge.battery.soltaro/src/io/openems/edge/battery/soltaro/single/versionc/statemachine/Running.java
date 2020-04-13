package io.openems.edge.battery.soltaro.single.versionc.statemachine;

public class Running extends StateMachine.Handler {

//	if (this.isError()) {
//	this.setStateMachineState(State.ERROR);
//} else if (!this.isSystemRunning()) {
//	this.setStateMachineState(State.UNDEFINED);
//} else {
//	this.setStateMachineState(State.RUNNING);
//	this.checkAllowedCurrent();
//	readyForWorking = true;
//}
//break;
	
	@Override
	public StateMachine getNextState(StateMachine.Context context) {
		return StateMachine.UNDEFINED;
	}

}
