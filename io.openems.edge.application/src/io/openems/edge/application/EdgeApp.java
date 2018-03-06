package io.openems.edge.application;

import java.util.List;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.config.ConfigUtils;
import io.openems.edge.controller.api.Controller;

@Component()
public class EdgeApp {

	private final Logger log = LoggerFactory.getLogger(EdgeApp.class);

	@Reference
	private ConfigurationAdmin configAdmin;

	@Reference(target = "(service.factoryPid=Controller.DebugLog)")
	private volatile List<Controller> controllers;

	@Reference
	private volatile List<Controller> controllers2;

	@Activate
	void activate() {
		log.debug("Activate EdgeApp");
		ConfigUtils.configureLogging(configAdmin);
	}

	@Deactivate
	void deactivate() {
		log.debug("Deactivate EdgeApp");
	}

}
