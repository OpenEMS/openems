package io.openems.backend.metadata.api;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.common.events.BackendEventConstants;
import io.openems.common.exceptions.OpenemsException;

@Component(property = { //
		EventConstants.EVENT_TOPIC + "=" + BackendEventConstants.TOPIC_EDGE_ONLINE,
		EventConstants.EVENT_TOPIC + "=" + BackendEventConstants.TOPIC_EDGE_OFFLINE })
public class EdgeOnlineHandler implements EventHandler {

	private final Logger log = LoggerFactory.getLogger(EdgeOnlineHandler.class);

	@Reference
	private MetadataService metadataService;

	@Override
	public void handleEvent(Event event) {
		int edgeId = (int) event.getProperty("edgeId");
		Edge edge;
		try {
			edge = this.metadataService.getEdge(edgeId);
		} catch (OpenemsException e) {
			log.warn(e.getMessage());
			return;
		}
		String topic = (String) event.getProperty("event.topics");
		if (topic.equals(BackendEventConstants.TOPIC_EDGE_ONLINE)) {
			edge.setOnline(true);
			log.debug("Marked Edge [" + edge.getName() + "] as Online");
		} else if (topic.equals(BackendEventConstants.TOPIC_EDGE_OFFLINE)) {
			edge.setOnline(false);
			log.debug("Marked Edge [" + edge.getName() + "] as Offline");
		} else {
			log.warn("Unknown Topic: " + topic);
		}
	}
}
