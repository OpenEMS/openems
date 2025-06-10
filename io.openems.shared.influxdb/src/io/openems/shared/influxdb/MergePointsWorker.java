package io.openems.shared.influxdb;

import com.influxdb.client.write.Point;

public interface MergePointsWorker {

	/**
	 * Activates the worker.
	 */
	public void activate();

	/**
	 * Deactivates the worker.
	 */
	public void deactivate();

	/**
	 * Inserts the specified element into this queue if it is possible to do so
	 * immediately without violating capacity restrictions, returning true upon
	 * success and false if no space is currently available.
	 * 
	 * @param point the {@link Point} to add
	 * @return true if the point was added to this queue, else false
	 */
	public boolean offer(Point point);

	/**
	 * Simple debug log string.
	 * 
	 * @return the debug string
	 */
	public String debugLog();

}
