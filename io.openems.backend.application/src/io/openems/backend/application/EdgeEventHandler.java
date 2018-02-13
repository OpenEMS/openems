package io.openems.backend.application;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import io.openems.backend.common.events.BackendEventConstants;

@Component(property = { //
		EventConstants.EVENT_TOPIC + "=" + BackendEventConstants.TOPIC_EDGE_ONLINE,
		EventConstants.EVENT_TOPIC + "=" + BackendEventConstants.TOPIC_EDGE_OFFLINE })
public class EdgeEventHandler implements EventHandler {

	@Override
	public void handleEvent(Event event) {
		System.out.println(event);
	}

}
