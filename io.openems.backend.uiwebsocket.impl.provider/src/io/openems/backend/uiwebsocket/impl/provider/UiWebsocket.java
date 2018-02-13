package io.openems.backend.uiwebsocket.impl.provider;

import java.io.IOException;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.backend.edgewebsocket.api.EdgeWebsocketService;
import io.openems.backend.metadata.api.MetadataService;
import io.openems.backend.uiwebsocket.api.UiWebsocketService;

import org.osgi.service.metatype.annotations.Designate;

@Designate(ocd = UiWebsocket.Config.class, factory = false)
@Component(name = "UiWebsocket")
public class UiWebsocket implements UiWebsocketService {

	private final Logger log = LoggerFactory.getLogger(UiWebsocket.class);

	private UiWebsocketServer server = null;

	@Reference
	protected MetadataService metadataService;

	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC) // avoid recursive dependency
	protected volatile EdgeWebsocketService edgeWebsocketService;

	@ObjectClassDefinition
	@interface Config {
		int port();
	}

	@Activate
	void activate(Config config) {
		log.debug("Activate UiWebsocket [port=" + config.port() + "]");

		this.stopServer();
		this.startServer(config.port());
	}

	@Deactivate
	void deactivate() {
		log.debug("Deactivate UiWebsocket");
		this.stopServer();
	}

	/**
	 * Stop existing websocket server
	 */
	private void stopServer() {
		if (this.server != null) {
			try {
				this.server.stop();
			} catch (IOException | InterruptedException e) {
				log.error("Unable to stop existing UiWebsocketServer: " + e.getMessage());
			}
		}
	}

	/**
	 * Create and start new server
	 * 
	 * @param port
	 */
	private void startServer(int port) {
		this.server = new UiWebsocketServer(this, port);
		this.server.start();
	}

	@Override
	public void handleEdgeReply(int edgeId, JsonObject jMessage) {
		// TODO Auto-generated method stub
		log.info("TODO handleEdgeReply");
	}
}
