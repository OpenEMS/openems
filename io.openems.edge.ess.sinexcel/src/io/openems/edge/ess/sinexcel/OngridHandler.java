package io.openems.edge.ess.sinexcel;

//import io.openems.edge.ess.sinexcel.StateMachine.State;

public class OngridHandler {
//	private final Logger log = LoggerFactory.getLogger(OngridHandler.class);
//	private final StateMachine parent;
//
//	private State state = State.UNDEFINED;
//
//	public OngridHandler(StateMachine parent) {
//		this.parent = parent;
//	}
//
//	public void initialize() {
//		this.state = State.UNDEFINED;
//	}
//
//	protected StateMachine.State run() throws IllegalArgumentException, OpenemsNamedException {
//		System.out.println("OngridHandler.run");
//		// Verify that we are still On-Grid -> otherwise switch to "Going Off-Grid"
//		GridMode gridMode = this.parent.parent.getGridMode().getNextValue().asEnum();
//		switch (gridMode) {
//		case ON_GRID:
//		case UNDEFINED:
//			break;
//		case OFF_GRID:
//			return StateMachine.State.OFFGRID;
//		}
//
//		switch (this.state) {
//		case UNDEFINED:
//			this.state = this.doUndefined(this.state);
//			break;
//		case ONGRID:
//			this.state = this.doOngrid(this.state);
//			break;
//		case OFFGRID:
//			break;
//		default:
//			break;
//		}
//
//		return StateMachine.State.ONGRID;
//	}
//
//	private State doUndefined(State state) throws OpenemsNamedException {
//		return doOperations();
//	}
//
//	private State doOngrid(State state) throws OpenemsNamedException {
//		return doOperations();
//	}
//
//	// Assumptions : defined and ongrid mode is same, as there is only operations to Softstart()
//	
//	private State doOperations() throws OpenemsNamedException {
//		CurrentState currentState = this.parent.getSinexcelState();
//
//		switch (currentState) {
//		case UNDEFINED:
//		case SLEEPING:
//		case MPPT:
//		case THROTTLED:
//		case STARTED:
//			this.parent.parent.softStart(true);
//			break;
//		case SHUTTINGDOWN:
//		case FAULT:
//		case STANDBY:
//		case OFF:
//			this.parent.parent.softStart(false);
//		default:
//			break;
//
//		}
//		return state;
//	}

}
