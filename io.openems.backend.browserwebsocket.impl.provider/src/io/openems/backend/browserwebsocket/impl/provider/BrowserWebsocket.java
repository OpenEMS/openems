package io.openems.backend.browserwebsocket.impl.provider;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.browserwebsocket.api.BrowserWebsocketService;

import org.osgi.service.metatype.annotations.Designate;


@Designate( ocd=BrowserWebsocket.Config.class, factory=true)
@Component(name="BrowserWebsocket")
public class BrowserWebsocket implements BrowserWebsocketService {

	private final Logger log = LoggerFactory.getLogger(BrowserWebsocket.class);
	
	@ObjectClassDefinition
	@interface Config {
		int port();
	}
	@Activate
	void activate(Config config) {
		log.debug("Activate BrowserWebsocket");
	}

	@Deactivate
	void deactivate() {
		log.debug("Deactivate BrowserWebsocket");
	}

}
