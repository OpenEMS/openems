package io.openems.edge.core.timer;

import org.osgi.service.cm.ConfigurationException;

import java.time.Instant;

/**
 * The TimerHandler Interface makes the use of Timer easier. OpenemsComponents,
 * that wants to use the {@link Timer} should use this, to avoid a bulk of
 * implementation. Components cann add an Identifier to the TimerHandler by
 * giving it the identifier id, the TimerString they want to use and the maxTime
 * Everything's usually from config, except the identifier, this can be a static
 * final string.
 */
public interface TimerHandler {
	/**
	 * This method let's you add an identifier with it's configuration to a Timer.
	 * After that you can ask the TimerHandler if a time is up or reset a
	 * identifier->Timer.
	 *
	 * @param identifier 1 of n identifier a component can have.
	 * @param timer      the {@link TimerType} of the {@link Timer} previously configured
	 * @param maxTime    the max Allowed Time. (InitTime + MaxTime in Seconds)
	 * @throws ConfigurationException             if Id not an instance of Timer
	 */
	public void addOneIdentifier(String identifier, TimerType timer, int maxTime) throws ConfigurationException;

	/**
	 * Resets the Timer determined by the Identifier. This will call the
	 * {@link Timer#reset(String, String)} indirectly
	 *
	 * @param identifier the identifier that in combination with the stored
	 *                   component id, will be used to determine if the Time for the
	 *                   identifier is up.
	 */
	public void resetTimer(String identifier);

	/**
	 * Removes all identifier/the component from the timer.
	 */
	public void removeComponent();

	/**
	 * Checks if the Time is up equivalent to the
	 * {@link Timer#checkIsTimeUp(String, String)} method.
	 *
	 * @param identifier one of the identifier of the component.
	 * @return true if Time is up.
	 */
	public boolean checkTimeIsUp(String identifier);

	/**
	 * Overrides the Initial Time. Use with caution.
	 *
	 * @param time           the new initial Time.
	 * @param identifierSwap one of the identifier of the component.
	 */
	public void setInitialTime(Instant time, String identifierSwap);

	/**
	 * Overrides the Initial Time. Use with caution.
	 *
	 * @param count          new initial SetPoint of the counter.
	 * @param identifierSwap one of the identifier of the component.
	 */
	public void setInitialTime(Integer count, String identifierSwap);
}
