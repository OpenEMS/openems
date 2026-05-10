package io.openems.common.bridge.http.time.periodic;

import java.util.concurrent.TimeUnit;

import io.openems.common.function.Disposable;

public interface PeriodicExecutor extends Disposable {
	/**
	 * Returns the remaining timespan in the given time unit before the action is
	 * called the next time.
	 *
	 * @param unit Unit for the return value
	 * @return Delay until next run (in the given unit)
	 */
	public long getRemainingDelayUntilNextRun(TimeUnit unit);
}
