package io.openems.edge.common.test;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

public class DummyEventAdmin implements EventAdmin {

	@Override
	public void postEvent(Event event) {
	}

	@Override
	public void sendEvent(Event event) {
	}

}
