package io.openems.edge.ruhfass.battery.rbti.statemachine;

import java.time.Clock;

import io.openems.edge.common.statemachine.AbstractContext;
import io.openems.edge.ruhfass.battery.rbti.RuhfassBatteryRbtiImpl;

public class Context extends AbstractContext<RuhfassBatteryRbtiImpl> {

	protected final Clock clock;

	public Context(RuhfassBatteryRbtiImpl parent, Clock clock) {
		super(parent);
		this.clock = clock;
	}

	/**
	 * Check is running.
	 * 
	 * @return true if battery is in started
	 */
	public boolean isStarted() {
		// TODO add logic
		return true;
	}

	/**
	 * Check is shutdown.
	 * 
	 * @return true if battery is stopped
	 */
	public boolean isStopped() {
		// TODO add logic
		return true;
	}

	/**
	 * Check if battery has any fault or in error state.
	 * 
	 * @return true if battery has at an error.
	 */
	public boolean hasFaults() {
		// TODO add logic
		return true;
	}
}
