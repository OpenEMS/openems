package io.openems.backend.restapi;

import org.restlet.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestApiSingleton {

	private final Logger log = LoggerFactory.getLogger(RestApiSingleton.class);
	private final Component component;

	public RestApiSingleton(int port) {
		this.component = new Component();
		// this.component.getServers().add(Protocol.HTTP, port);
		// this.component.getDefaultHost().attach("/rest", new RestApiApplication());
		// try {
		// this.component.start();
		// } catch (Exception e) {
		// throw new OpenemsException("Starting REST-Api failed: " + e.getMessage());
		// }
		// log.info("REST-Api started on port [" + port + "].");
	}
}
