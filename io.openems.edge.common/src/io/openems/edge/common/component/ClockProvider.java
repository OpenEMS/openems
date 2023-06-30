package io.openems.edge.common.component;

import java.time.Clock;

import io.openems.edge.common.test.TimeLeapClock;

/**
 * {@link ClockProvider} provides a Clock - real or mocked like
 * {@link TimeLeapClock}.
 */
public interface ClockProvider {

	/**
	 * Gets the Clock - real or mocked like {@link TimeLeapClock}.
	 *
	 * @return the {@link Clock}
	 */
	public Clock getClock();

}
