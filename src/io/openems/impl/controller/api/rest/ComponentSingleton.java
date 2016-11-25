package io.openems.impl.controller.api.rest;

import org.restlet.Component;
import org.restlet.data.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.exception.OpenemsException;

public class ComponentSingleton {

	private static Component instance = null;
	private static Integer port = null;

	private final static Logger log = LoggerFactory.getLogger(ComponentSingleton.class);

	protected static synchronized Component getComponent(ConfigChannel<Integer> port) throws OpenemsException {
		if (port.valueOptional().isPresent()) {
			return getComponent(port.valueOptional().get());
		}
		throw new OpenemsException("Unable to start REST-Api: port is not set");
	}

	protected static synchronized Component getComponent(int port) throws OpenemsException {
		if (ComponentSingleton.instance != null
				&& (ComponentSingleton.port == null || ComponentSingleton.port != port)) {
			// port changed -> restart
			ComponentSingleton.restartComponent(port);
		}
		if (ComponentSingleton.instance == null) {
			// instance not available -> start
			startComponent(port);
		}
		return ComponentSingleton.instance;
	}

	protected static synchronized void restartComponent(int port) throws OpenemsException {
		stopComponent();
		startComponent(port);
	}

	private static synchronized void startComponent(int port) throws OpenemsException {
		ComponentSingleton.instance = new Component();
		ComponentSingleton.instance.getServers().add(Protocol.HTTP, port);
		ComponentSingleton.instance.getDefaultHost().attach("/rest", new RestApiApplication());
		try {
			ComponentSingleton.instance.start();
			ComponentSingleton.port = port;
			log.info("REST-Api started on port [" + port + "].");
		} catch (Exception e) {
			throw new OpenemsException("REST-Api failed on port [" + port + "].", e);
		}
	}

	protected static void stopComponent() {
		if (ComponentSingleton.instance != null) {
			try {
				ComponentSingleton.instance.stop();
				log.error("REST-Api stopped.");
			} catch (Exception e) {
				log.error("REST-Api failed to stop.", e);
			}
			ComponentSingleton.instance = null;
			ComponentSingleton.port = null;
		}
	}
}
