package io.openems.backend.edgewebsocket.impl.provider;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.backend.edgewebsocket.api.EdgeWebsocketService;
import io.openems.backend.metadata.api.MetadataService;
import io.openems.backend.timedata.api.TimedataService;
import io.openems.backend.uiwebsocket.api.UiWebsocketService;
import io.openems.common.exceptions.OpenemsException;

@Designate(ocd = EdgeWebsocket.Config.class, factory = false)
@Component(name = "EdgeWebsocket", configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true)
public class EdgeWebsocket implements EdgeWebsocketService {

	private final Logger log = LoggerFactory.getLogger(EdgeWebsocket.class);

	private EdgeWebsocketServer server = null;

	@Reference
	protected volatile MetadataService metadataService;

	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
	protected volatile UiWebsocketService uiWebsocketService;
	
	@Reference
	protected volatile TimedataService timedataService;

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
			int tries = 3;
			while (tries-- > 0) {
				try {
					this.server.stop(1000);
					return;
				} catch (NullPointerException | InterruptedException e) {
					log.warn("Unable to stop existing EdgeWebsocketServer. " + e.getClass().getSimpleName() + ": "
							+ e.getMessage());
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						/* ignore */
					}
				}
			}
			log.error("Stopping EdgeWebsocketServer failed too often.");
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

	@Override
	public boolean isOnline(int edgeId) {
		return this.server.isOnline(edgeId);
	}

	@Override
	public void forwardMessageFromUi(int edgeId, JsonObject jMessage) throws OpenemsException {
		this.server.forwardMessageFromUi(edgeId, jMessage);
	}

}
