package io.openems.edge.bridge.http.time;

import io.openems.edge.bridge.http.api.BridgeHttp.Endpoint;

public interface DelayTimeProvider {

	/**
	 * Gives the {@link Delay} till the next run should be triggered.
	 * 
	 * @param firstRun          true if this method gets executed for the first time
	 * @param lastRunSuccessful true if the last fetch of an {@link Endpoint} was
	 *                          successful
	 * @return the {@link Delay} till the next run
	 */
	public Delay nextRun(boolean firstRun, boolean lastRunSuccessful);

}
