package io.openems.edge.ess.generic.symmetric.essfaultbehaviour;

import io.openems.edge.ess.generic.symmetric.statemachine.Context;

public interface EssFaultBehaviour {

	/**
	 * Checks if the ess has a fault.
	 * 
	 * @param context the current context.
	 * @return true if there is a fault; else false
	 */
	boolean hasEssFault(Context context);

	/**
	 * Checks if the ess is started.
	 *
	 * @param context the current context.
	 * @return true if the ess is started; else false
	 */
	boolean isEssStarted(Context context);

}
