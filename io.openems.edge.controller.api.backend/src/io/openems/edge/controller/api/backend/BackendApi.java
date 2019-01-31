package io.openems.edge.controller.api.backend;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.ops4j.pax.logging.spi.PaxAppender;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.notification.SystemLogNotification;
import io.openems.common.websocket.AbstractWebsocketClient;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.api.core.ApiWorker;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.Api.Backend", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = "org.ops4j.pax.logging.appender.name=Controller.Api.Backend")
public class BackendApi extends AbstractOpenemsComponent implements Controller, OpenemsComponent, PaxAppender {

	protected static final int DEFAULT_CYCLE_TIME = 10000;
	protected static final String COMPONENT_NAME = "Controller.Api.Backend";

	protected final BackendWorker backendWorker = new BackendWorker(this);

	private final Logger log = LoggerFactory.getLogger(BackendApi.class);
	private final ApiWorker apiWorker = new ApiWorker();

	protected WebsocketClient websocket = null;
	protected int cycleTime = DEFAULT_CYCLE_TIME; // default, is going to be overwritten by config
	protected boolean debug = false;

	// Used for SubscribeSystemLogRequests
	private boolean isSystemLogSubscribed = false;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	protected volatile Timedata timedata = null;

	@Reference
	protected ComponentManager componentManager;

	@Reference
	private ConfigurationAdmin configAdmin;

	@Reference(policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			target = "(&(enabled=true)(!(service.factoryPid=" + COMPONENT_NAME + ")))")
	private volatile List<OpenemsComponent> components = new CopyOnWriteArrayList<>();

	public BackendApi() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.enabled());
		this.cycleTime = config.cycleTime();
		this.debug = config.debug();

		if (!this.isEnabled()) {
			return;
		}

		// initialize ApiWorker
		this.apiWorker.setTimeoutSeconds(config.apiTimeout());

		// Get URI
		URI uri = null;
		try {
			uri = new URI(config.uri());
		} catch (URISyntaxException e) {
			log.error("URI [" + config.uri() + "] is invalid: " + e.getMessage());
			return;
		}

		// Get Proxy configuration
		Proxy proxy;
		if (config.proxyAddress().trim().equals("") || config.proxyPort() == 0) {
			proxy = AbstractWebsocketClient.NO_PROXY;
		} else {
			proxy = new Proxy(config.proxyType(), new InetSocketAddress(config.proxyAddress(), config.proxyPort()));
		}

		// create http headers
		Map<String, String> httpHeaders = new HashMap<>();
		httpHeaders.put("apikey", config.apikey());

		// Create Websocket instance
		this.websocket = new WebsocketClient(this, COMPONENT_NAME, uri, httpHeaders, proxy);
		this.websocket.start();

		// Activate worker
		this.backendWorker.activate(config.id());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.backendWorker.deactivate();
		this.websocket.stop();
	}

	@Override
	public void run() throws OpenemsNamedException {
		this.apiWorker.run();
	}

	public ComponentManager getComponentManager() {
		return this.componentManager;
	}

	@Override
	protected void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}

	@Override
	protected void logWarn(Logger log, String message) {
		super.logInfo(log, message);
	}

	/**
	 * Activates/deactivates subscription to System-Log.
	 * 
	 * <p>
	 * If activated, all System-Log events are sent via
	 * {@link SystemLogNotification}s.
	 * 
	 * @param isSystemLogSubscribed true to activate
	 */
	protected void setSystemLogSubscribed(boolean isSystemLogSubscribed) {
		this.isSystemLogSubscribed = isSystemLogSubscribed;
	}

	@Override
	public void doAppend(PaxLoggingEvent event) {
		if (!this.isSystemLogSubscribed) {
			return;
		}
		WebsocketClient ws = this.websocket;
		if (ws == null) {
			return;
		}
		SystemLogNotification notification = SystemLogNotification.fromPaxLoggingEvent(event);
		ws.sendMessage(notification);
	}
}
