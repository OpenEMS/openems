package io.openems.edge.ess.mr.gridcon;

public class GoingOngridHandler {

	@SuppressWarnings("unused")
	private final StateMachine parent;

	public GoingOngridHandler(StateMachine parent) {
		this.parent = parent;
	}

	public void initialize() {
	}

	protected StateMachine.State run() {
		// always just go ONGRID
		return StateMachine.State.ONGRID;
	}
}
