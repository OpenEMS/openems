//package io.openems.edge.ess.mr.gridcon;
//
//import java.time.LocalDateTime;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import io.openems.common.types.OptionsEnum;
//
//public class GoingOffgridHandler {
//
//	private final Logger log = LoggerFactory.getLogger(GoingOffgridHandler.class);
//	@SuppressWarnings("unused")
//	private final StateMachine parent;
//
//	private State state = State.UNDEFINED;
//
//	// WAIT
//	private LocalDateTime startedWaiting = null;
//	private final static int WAIT_SECONDS = 5;
//
//	public GoingOffgridHandler(StateMachine parent) {
//		this.parent = parent;
//	}
//
//	public void initialize() {
//		this.state = State.UNDEFINED;
//		this.startedWaiting = null;
//	}
//
//	protected StateMachine.State run() {
//		System.out.println("GoingOffgridHandler.run");
//		switch (this.state) {
//		case UNDEFINED:
//			this.state = this.doUndefined();
//			break;
//
//		case WAIT:
//			this.state = this.doWait();
//			break;
//
//		case FINISH_GOING_OFFGRID:
//			// finish GoingOffgridHandler, switch to OFFGRID-State
//			this.initialize();
//			return StateMachine.State.OFFGRID;
//		}
//
//		return StateMachine.State.GOING_OFFGRID;
//	}
//
//	/**
//	 * Handle UNDEFINED, i.e. GoingOffgridHandler just started taking over. Starts
//	 * with WAIT-State.
//	 * 
//	 * @return the next state
//	 */
//	private State doUndefined() {
//		return State.WAIT;
//	}
//
//	/**
//	 * Handle WAIT. Waits WAIT_SECONDS, then switches to FINISH_GOING_OFFGRID
//	 * 
//	 * @return the next state
//	 */
//	private State doWait() {
//		if (this.startedWaiting == null) {
//			this.startedWaiting = LocalDateTime.now();
//		}
//
//		if (this.startedWaiting.plusSeconds(WAIT_SECONDS).isAfter(LocalDateTime.now())) {
//			this.log.info("doWaitFirstSeconds() waiting the first seconds");
//			return State.WAIT;
//		}
//
//		// finished waiting
//		this.log.info("doWaitFirstSeconds() finished waiting");
//		return State.FINISH_GOING_OFFGRID;
//	}
//
//	public enum State implements OptionsEnum {
//		UNDEFINED(-1, "Undefined"), //
//		WAIT(1, "For the first seconds just wait"), //
//		FINISH_GOING_OFFGRID(3, "Finish Going Off-Grid"); //
//
//		private final int value;
//		private final String name;
//
//		private State(int value, String name) {
//			this.value = value;
//			this.name = name;
//		}
//
//		@Override
//		public int getValue() {
//			return value;
//		}
//
//		@Override
//		public String getName() {
//			return name;
//		}
//
//		@Override
//		public OptionsEnum getUndefined() {
//			return UNDEFINED;
//		}
//	}
//}
