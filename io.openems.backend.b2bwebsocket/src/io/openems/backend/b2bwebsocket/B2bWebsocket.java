package io.openems.backend.b2bwebsocket;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.backend.common.jsonrpc.JsonRpcRequestHandler;
import io.openems.backend.common.metadata.Metadata;
import io.openems.backend.common.timedata.Timedata;
import io.openems.common.utils.ThreadPoolUtils;

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

	protected final ScheduledExecutorService executor = Executors.newScheduledThreadPool(10,
			new ThreadFactoryBuilder().setNameFormat("B2bWebsocket-%d").build());

	public B2bWebsocket() {
		super("Backend2Backend.Websocket");
	}

	private Config config;

	private final Runnable startServerWhenMetadataIsInitialized = () -> {
		this.startServer(this.config.port(), this.config.poolSize(), this.config.debugMode());
	};

	@Activate
	private void activate(Config config) {
		this.config = config;
		this.metadata.addOnIsInitializedListener(this.startServerWhenMetadataIsInitialized);
	}

	@Deactivate
	private void deactivate() {
		ThreadPoolUtils.shutdownAndAwaitTermination(this.executor, 5);
		this.metadata.removeOnIsInitializedListener(this.startServerWhenMetadataIsInitialized);
		this.stopServer();
	}

	/**
	 * Create and start new server.
	 *
	 * @param port      the port
	 * @param poolSize  number of threads dedicated to handle the tasks
	 * @param debugMode activate a regular debug log about the state of the tasks
	 */
	private synchronized void startServer(int port, int poolSize, boolean debugMode) {
		this.server = new WebsocketServer(this, this.getName(), port, poolSize, debugMode);
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
