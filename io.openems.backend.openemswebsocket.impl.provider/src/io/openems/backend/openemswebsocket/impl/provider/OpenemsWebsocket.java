package io.openems.backend.openemswebsocket.impl.provider;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.openemswebsocket.api.OpenemsWebsocketService;

import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Designate(ocd = OpenemsWebsocket.Config.class, factory = false)
@Component(name = "OpenemsWebsocket")
public class OpenemsWebsocket implements OpenemsWebsocketService {

	private final Logger log = LoggerFactory.getLogger(OpenemsWebsocket.class);

	@ObjectClassDefinition
	@interface Config {
		int port();
	}

	@Activate
	void activate(Config config) {
		log.debug("Activate OpenemsWebsocket [port=" + config.port() + "]");
	}

	@Deactivate
	void deactivate() {
		log.debug("Deactivate OpenemsWebsocket");
	}

}
