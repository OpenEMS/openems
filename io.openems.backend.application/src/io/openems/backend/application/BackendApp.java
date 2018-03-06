package io.openems.backend.application;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.config.ConfigUtils;

@Component()
public class BackendApp {

	private final Logger log = LoggerFactory.getLogger(BackendApp.class);

	@Reference
	private ConfigurationAdmin configAdmin;

	@Activate
	void activate() {
		log.debug("Activate BackendApp");
		ConfigUtils.configureLogging(configAdmin);
	}

	@Deactivate
	void deactivate() {
		log.debug("Deactivate BackendApp");
	}

}
