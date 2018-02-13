package io.openems.backend.edgewebsocket.impl.provider;

import java.io.IOException;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.edgewebsocket.api.EdgeWebsocketService;
import io.openems.backend.metadata.api.MetadataService;
import io.openems.backend.uiwebsocket.api.UiWebsocketService;

import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Designate(ocd = EdgeWebsocket.Config.class, factory = false)
@Component(name = "EdgeWebsocket", immediate = true)
public class EdgeWebsocket implements EdgeWebsocketService {

	private final Logger log = LoggerFactory.getLogger(EdgeWebsocket.class);

	private EdgeWebsocketServer server = null;

	@Reference
	protected MetadataService metadataService;

	@Reference
	protected UiWebsocketService uiWebsocketService;

	@Reference
	protected EventAdmin eventAdmin;

	@ObjectClassDefinition
	@interface Config {
		int port();
	}

	@Activate
	void activate(Config config) {
		log.debug("Activate EdgeWebsocket [port=" + config.port() + "]");

		this.stopServer();
		this.startServer(config.port());
	}

	@Deactivate
	void deactivate() {
		log.debug("Deactivate EdgeWebsocket");
		this.stopServer();
	}

	/**
	 * Stop existing websocket server
	 */
	private synchronized void stopServer() {
		if (this.server != null) {
			try {
				this.server.stop();
			} catch (IOException | InterruptedException e) {
				log.error("Unable to stop existing EdgeWebsocketServer: " + e.getMessage());
			}
		}
	}

	/**
	 * Create and start new server
	 * 
	 * @param port
	 */
	private synchronized void startServer(int port) {
		this.server = new EdgeWebsocketServer(this, port);
		this.server.start();
	}

}
