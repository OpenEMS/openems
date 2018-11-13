package io.openems.backend.uiwebsocket.impl;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.backend.edgewebsocket.api.EdgeWebsocketService;
import io.openems.backend.metadata.api.MetadataService;
import io.openems.backend.timedata.api.TimedataService;
import io.openems.backend.uiwebsocket.api.UiWebsocketService;
import io.openems.common.exceptions.OpenemsException;

@Designate(ocd = Config.class, factory = false)
@Component(name = "UiWebsocket", configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true)
public class UiWebsocket implements UiWebsocketService {

	private final Logger log = LoggerFactory.getLogger(UiWebsocket.class);

	private UiWebsocketServer server = null;

	@Reference
	protected volatile MetadataService metadataService;

	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
	protected volatile EdgeWebsocketService edgeWebsocketService;

	@Reference
	protected volatile TimedataService timeDataService;

	@Activate
	void activate(Config config) {
		log.info("Activate UiWebsocket [port=" + config.port() + "]");

		this.stopServer();
		this.startServer(config.port());
	}

	@Deactivate
	void deactivate() {
		log.info("Deactivate UiWebsocket");
		this.stopServer();
	}

	/**
	 * Stop existing websocket server
	 */
	private void stopServer() {
		if (this.server != null) {
			int tries = 3;
			while (tries-- > 0) {
				try {
					this.server.stop(1000);
					return;
				} catch (NullPointerException | InterruptedException e) {
					log.warn("Unable to stop existing UiWebsocketServer. " + e.getClass().getSimpleName() + ": "
							+ e.getMessage());
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						/* ignore */
					}
				}
			}
			log.error("Stopping UiWebsocketServer failed too often.");
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
	public void handleEdgeReply(int edgeId, JsonObject jMessage) throws OpenemsException {
		this.server.handleEdgeReply(edgeId, jMessage);
	}
}
