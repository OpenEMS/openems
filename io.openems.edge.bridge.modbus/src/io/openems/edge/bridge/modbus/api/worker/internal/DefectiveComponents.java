package io.openems.edge.bridge.modbus.api.worker.internal;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import io.openems.edge.common.type.TypeUtils;

// TODO add method to inform about repeated read/write error for a Component.
// Use this information to dynamically increase delay.

public class DefectiveComponents {
	/**
	 * Do not retry reading from/writing to a defective Component for PAUSE_SECONDS.
	 */
	public static final int PAUSE_SECONDS = 30;

	private final Clock clock;
	private final Map<String, Instant> nextTries = new HashMap<>();

	public DefectiveComponents() {
		this(Clock.systemDefaultZone());
	}

	protected DefectiveComponents(Clock clock) {
		this.clock = clock;
	}

	/**
	 * Adds a defective Component and sets retry time to now() +
	 * {@value #PAUSE_SECONDS}.
	 * 
	 * @param componentId the Component-ID; not null
	 */
	public synchronized void add(String componentId) {
		TypeUtils.assertNull("DefectiveComponents add() takes no null values", componentId);
		this.nextTries.put(componentId, Instant.now(this.clock).plusSeconds(PAUSE_SECONDS));
	}

	/**
	 * Removes a defective Component.
	 * 
	 * @param componentId the Component-ID; not null
	 */
	public synchronized void remove(String componentId) {
		TypeUtils.assertNull("DefectiveComponents remove() takes no null values", componentId);
		this.nextTries.remove(componentId);
	}

	/**
	 * Is the given Component known to be defective?.
	 * 
	 * @param componentId the Component-ID
	 * @return true if listed as defective, false if not
	 */
	public synchronized boolean isKnown(String componentId) {
		return this.nextTries.containsKey(componentId);
	}

	/**
	 * Is the given Component due for next try?.
	 * 
	 * @param componentId the Component-ID
	 * @return true if yes, false if no, null if component is not in list of
	 *         defective components
	 */
	public synchronized Boolean isDueForNextTry(String componentId) {
		var dueTime = this.nextTries.get(componentId);
		if (dueTime == null) {
			return null;
		}
		var now = Instant.now(this.clock);
		return now.isAfter(dueTime);
	}
}