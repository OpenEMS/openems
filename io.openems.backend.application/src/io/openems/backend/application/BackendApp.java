package io.openems.backend.application;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component()
public class BackendApp {

	private final Logger log = LoggerFactory.getLogger(BackendApp.class);

	@Activate
	void activate() {
		log.debug("Activate BackendApp");
	}

	@Deactivate
	void deactivate() {
		log.debug("Deactivate BackendApp");
	}

}
