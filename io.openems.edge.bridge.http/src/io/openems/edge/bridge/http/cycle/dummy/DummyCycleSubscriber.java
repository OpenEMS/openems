package io.openems.edge.bridge.http.cycle.dummy;

import static java.util.Collections.emptyMap;

import org.osgi.service.event.Event;

import io.openems.edge.bridge.http.cycle.CycleSubscriber;
import io.openems.edge.common.event.EdgeEventConstants;

public class DummyCycleSubscriber extends CycleSubscriber {

	/**
	 * Passes a dummy event to the {@link CycleSubscriber} to trigger the next cycle
	 * event.
	 */
	public void triggerNextCycle() {
		this.handleEvent(new Event(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, emptyMap()));
	}

}
