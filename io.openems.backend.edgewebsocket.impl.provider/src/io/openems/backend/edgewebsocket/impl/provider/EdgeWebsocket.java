package io.openems.backend.edgewebsocket.impl.provider;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.edgewebsocket.api.EdgeWebsocketService;

import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Designate(ocd = EdgeWebsocket.Config.class, factory = false)
@Component(name = "EdgeWebsocket")
public class EdgeWebsocket implements EdgeWebsocketService {

	private final Logger log = LoggerFactory.getLogger(EdgeWebsocket.class);

	@ObjectClassDefinition
	@interface Config {
		int port();
	}

	@Activate
	void activate(Config config) {
		log.debug("Activate EdgeWebsocket [port=" + config.port() + "]");
	}

	@Deactivate
	void deactivate() {
		log.debug("Deactivate EdgeWebsocket");
	}

}
