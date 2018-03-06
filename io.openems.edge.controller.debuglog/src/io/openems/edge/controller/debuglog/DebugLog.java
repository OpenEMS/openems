package io.openems.edge.controller.debuglog;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.controller.api.Controller;

import org.osgi.service.metatype.annotations.Designate;

@Designate(ocd = DebugLog.Config.class, factory = true)
@Component(name = "Controller.DebugLog", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class DebugLog implements Controller {

	private final Logger log = LoggerFactory.getLogger(DebugLog.class);

	@ObjectClassDefinition
	@interface Config {
		String name();
	}

	@Activate
	void activate(Config config) {
		log.debug("Activate DebugLog: " + config.name());
	}

	@Deactivate
	void deactivate() {
		log.debug("Dectivate DebugLog");
	}

}
