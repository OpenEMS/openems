package io.openems.edge.batteryinverter.kaco.blueplanetgridsave.errorrestart;

public enum ErrorRestartBehaviourConfig {
	NO_RESTART(new DefaultErrorRestartBehaviour()), //
	ALWAYS_RESTART(new AlwaysRestartErrorRestartBehaviour()), //
	;

	private final ErrorRestartBehaviour errorRestartBehaviour;

	ErrorRestartBehaviourConfig(ErrorRestartBehaviour errorRestartBehaviour) {
		this.errorRestartBehaviour = errorRestartBehaviour;
	}

	public ErrorRestartBehaviour getErrorRestartBehaviour() {
		return this.errorRestartBehaviour;
	}

}
