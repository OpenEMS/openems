package io.openems.backend.browserwebsocket.impl;

import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Designate;

@Designate(ocd = BrowserWebsocketImpl.Config.class, factory = false)
@Component(name = "io.openems.backend.browserwebsocket.impl")
public class BrowserWebsocketImpl {

	@ObjectClassDefinition
	@interface Config {
		int port();
	}

	private int port;

	@Activate
	void activate(Config config) {
		this.port = config.port();
	}

	@Deactivate
	void deactivate() {
	}

}
