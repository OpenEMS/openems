package io.openems.backend.restapi;

import org.restlet.Component;
import org.restlet.data.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestApiSingleton {

	private final Logger log = LoggerFactory.getLogger(RestApiSingleton.class);
	private final Component component;

	public RestApiSingleton(int port) throws Exception {
		this.component = new Component();
		this.component.getServers().add(Protocol.HTTP, port);
		this.component.getDefaultHost().attach("/rest", new RestApiApplication());
		this.component.start();
		log.info("REST-Api started on port [" + port + "].");
	}
}
