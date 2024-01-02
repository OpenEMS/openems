package io.openems.backend.b2bwebsocket;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.backend.common.debugcycle.DebugLoggable;
import io.openems.backend.common.edgewebsocket.EdgeWebsocket;
import io.openems.backend.common.jsonrpc.JsonRpcRequestHandler;
import io.openems.backend.common.metadata.Metadata;
import io.openems.backend.common.timedata.TimedataManager;
import io.openems.common.utils.ThreadPoolUtils;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Backend2Backend.Websocket", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		Metadata.Events.AFTER_IS_INITIALIZED //
})
public class Backend2BackendWebsocket extends AbstractOpenemsBackendComponent implements EventHandler, DebugLoggable {

	private static final String COMPONENT_ID = "b2bwebsocket0";

	public static final int DEFAULT_PORT = 8076;

	protected final ScheduledExecutorService executor = Executors.newScheduledThreadPool(10,
			new ThreadFactoryBuilder().setNameFormat("B2bWebsocket-%d").build());

	@Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.DYNAMIC)
	protected volatile JsonRpcRequestHandler jsonRpcRequestHandler;

	@Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.DYNAMIC)
	protected volatile Metadata metadata;

	@Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.DYNAMIC)
	protected volatile TimedataManager timedataManager;

	@Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.DYNAMIC)
	protected volatile EdgeWebsocket edgeWebsocket;

	private WebsocketServer server = null;
	private Config config;

	public Backend2BackendWebsocket() {
		super("Backend2Backend.Websocket");
	}

	@Activate
	private void activate(Config config) {
		this.config = config;

		if (this.metadata.isInitialized()) {
			this.startServer();
		}
	}

	@Deactivate
	private void deactivate() {
		ThreadPoolUtils.shutdownAndAwaitTermination(this.executor, 5);
		this.stopServer();
	}

	/**
	 * Create and start new server.
	 */
	private synchronized void startServer() {
		if (this.server == null) {
			this.server = new WebsocketServer(this, this.getName(), this.config.port(), this.config.poolSize(),
					this.config.debugMode());
			this.server.start();
		}
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

	@Override
	protected void logError(Logger log, String message) {
		super.logError(log, message);
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case Metadata.Events.AFTER_IS_INITIALIZED:
			this.startServer();
			break;
		}
	}

	public String getId() {
		return COMPONENT_ID;
	}

	@Override
	public String debugLog() {
		return "[" + this.getName() + "] " + this.server.debugLog();
	}

	@Override
	public Map<String, JsonElement> debugMetrics() {
		final var metrics = new HashMap<String, JsonElement>();

		this.server.debugMetrics().forEach((key, value) -> {
			metrics.put(this.getId() + "/" + key, new JsonPrimitive(value));
		});

		return metrics;
	}

}
