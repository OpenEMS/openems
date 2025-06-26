package io.openems.edge.common.startstop;

/**
 * Every OpenEMS Component that implements {@link StartStoppable} is required to
 * have a configuration property "startStop" of this type that overrides the
 * logic of the {@link StartStoppable#setStartStop(StartStop)} method:.
 *
 * <pre>
 * 	&#64;AttributeDefinition(name = "Start/stop behaviour?", description = "Should this Component be forced to start or stop?")
 *	StartStopConfig startStop() default StartStopConfig.AUTO;
 * </pre>
 *
 * <ul>
 * <li>if config is {@link StartStopConfig#START} -> always start
 * <li>if config is {@link StartStopConfig#STOP} -> always stop
 * <li>if config is {@link StartStopConfig#AUTO} -> start
 * {@link StartStop#UNDEFINED} and wait for a call to
 * {@link StartStoppable#setStartStop(StartStop)}
 * </ul>
 */

public enum StartStopConfig {
	/**
	 * Force START the Component.
	 */
	START,
	/**
	 * Force STOP the Component.
	 */
	STOP,
	/**
	 * Wait for runtime START/STOP command via
	 * {@link StartStoppable#setStartStop(StartStop)}.
	 */
	AUTO;
}