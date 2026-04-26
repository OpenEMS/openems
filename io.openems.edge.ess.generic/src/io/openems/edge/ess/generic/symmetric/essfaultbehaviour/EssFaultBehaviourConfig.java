package io.openems.edge.ess.generic.symmetric.essfaultbehaviour;

import io.openems.edge.ess.generic.symmetric.statemachine.Context;

public enum EssFaultBehaviourConfig {
	CHECK_ALL(new CheckAllEssFaultBehaviour()), //
	IGNORE_BATTERY_INVERTER_ERRORS(new IgnoreBatteryInverterEssFaultBehaviour()), //
	;

	private final EssFaultBehaviour hasEssFaultBehaviour;

	EssFaultBehaviourConfig(EssFaultBehaviour hasEssFaultBehaviour) {
		this.hasEssFaultBehaviour = hasEssFaultBehaviour;
	}

	/**
	 * Checks if the current behaviour validates the ess to be in error.
	 * 
	 * @param context the current context
	 * @return true if the ess has an error; else false
	 */
	public boolean hasEssError(Context context) {
		return this.hasEssFaultBehaviour.hasEssFault(context);
	}

	/**
	 * Checks if the ess is started.
	 *
	 * @param context the current context
	 * @return true if the ess is started; else false
	 */
	public boolean isEssStarted(Context context) {
		return this.hasEssFaultBehaviour.isEssStarted(context);
	}

}
