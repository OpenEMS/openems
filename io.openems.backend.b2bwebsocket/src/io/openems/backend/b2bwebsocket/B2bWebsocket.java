package io.openems.backend.b2bwebsocket;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;

import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.backend.common.jsonrpc.JsonRpcRequestHandler;
import io.openems.backend.metadata.api.Metadata;
import io.openems.backend.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Backend2Backend.Websocket", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class B2bWebsocket extends AbstractOpenemsBackendComponent {

	public static final int DEFAULT_PORT = 8076;

	private WebsocketServer server = null;

	@Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.DYNAMIC)
	protected volatile JsonRpcRequestHandler jsonRpcRequestHandler;

	@Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.DYNAMIC)
	protected volatile Metadata metadata;

	@Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.DYNAMIC)
	protected volatile Timedata timeData;

	public B2bWebsocket() {
		super("Backend2Backend.Websocket");
	}

	@Activate
	void activate(Config config) {
		this.startServer(config.port());
	}

	@Deactivate
	void deactivate() {
		this.stopServer();
	}

	/**
	 * Create and start new server.
	 * 
	 * @param port the port
	 */
	private synchronized void startServer(int port) {
		this.server = new WebsocketServer(this, this.getName(), port);
		this.server.start();
	}

	/**
	 * Stop existing websocket server.
	 */
	private synchronized void stopServer() {
		if (this.server != null) {
			this.server.stop();
		}
	}

	@Override
	protected void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}

	@Override
	protected void logWarn(Logger log, String message) {
		super.logWarn(log, message);
	}
}
