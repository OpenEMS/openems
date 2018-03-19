package io.openems.edge.application;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true)
public class EdgeApp {

	private final Logger log = LoggerFactory.getLogger(EdgeApp.class);

	@Activate
	void activate() {
		log.debug("Activate EdgeApp");
	}

	@Deactivate
	void deactivate() {
		log.debug("Deactivate EdgeApp");
	}

}
