//package io.openems.edge.battery.soltaro.single.versionb.statemachine;
//
//import com.google.common.base.CaseFormat;
//
//import io.openems.common.types.OptionsEnum;
//import io.openems.edge.common.statemachine.StateHandler;
//
//public enum State implements io.openems.edge.common.statemachine.State<State, Context>, OptionsEnum {
//	UNDEFINED(-1, new UndefinedHandler()), //
//
//	GO_RUNNING(10, new GoRunningHandler()), //
//	RUNNING(11, new RunningHandler()), //
//
//	GO_STOPPED(20, new GoStoppedHandler()), //
//	STOPPED(21, new StoppedHandler()), //
//
//	ERROR(30, new ErrorHandler()), //
//
//	;
//
//	private final int value;
//	protected final StateHandler<State, Context> handler;
//
//	private State(int value, StateHandler<State, Context> handler) {
//		this.value = value;
//		this.handler = handler;
//	}
//
//	@Override
//	public int getValue() {
//		return this.value;
//	}
//
//	@Override
//	public String getName() {
//		return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.name());
//	}
//
//	@Override
//	public OptionsEnum getUndefined() {
//		return UNDEFINED;
//	}
//
//	@Override
//	public StateHandler<State, Context> getHandler() {
//		return this.handler;
//	}
//}
