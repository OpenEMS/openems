//package io.openems.edge.controller.ess.fastfrequencyreserve.statemachine;
//
//import java.time.Duration;
//import java.time.LocalDateTime;
//
//import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
//import io.openems.edge.common.component.ComponentManager;
//import io.openems.edge.common.statemachine.StateHandler;
//import io.openems.edge.controller.ess.fastfrequencyreserve.statemachine.StateMachine.State;
//import io.openems.edge.ess.api.ManagedSymmetricEss;
//
//public class BufferedSupportHandler extends StateHandler<State, Context> {
//
//	private static int ZERO_POWER = 0;
//	private boolean flagZeroPowerHold = true;
//	protected LocalDateTime startTime;
//
//	@Override
//	protected void onEntry(Context context) throws OpenemsNamedException {
//		flagZeroPowerHold = true;
//	}
//
//	protected void onExit(Context context) throws OpenemsNamedException {
//		flagZeroPowerHold = true;
//	}
//
//	@Override
//	protected State runAndGetNextState(Context context) throws OpenemsNamedException {
//
//		ManagedSymmetricEss e = context.ess;
//		ComponentManager componentManager = context.componentManager;
//
//		e.setActivePowerEquals(ZERO_POWER);
//
//		if (this.flagZeroPowerHold) {
//			setOnZeroPowerHold(componentManager);
//		} else {
//			long x = Duration //
//					.between(startTime, LocalDateTime.now(componentManager.getClock())) //
//					.getSeconds();
//			if (x >= context.supportDuration.getValue()) {
//				return State.RECOVERY_TIME;
//			}
//
//		}
//
//		return State.BUFFERED_TIME_BEFORE_RECOVERY;
//	}
//
//	private void setOnZeroPowerHold(ComponentManager componentManager) {
//		this.startTime = LocalDateTime.now(componentManager.getClock());
//		this.flagZeroPowerHold = false;
//
//	}
//
//}
