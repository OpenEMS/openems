package io.openems.edge.controller.api.backend;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.ops4j.pax.logging.spi.PaxAppender;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.OpenemsConstants;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.notification.EdgeConfigNotification;
import io.openems.common.jsonrpc.notification.SystemLogNotification;
import io.openems.common.types.EdgeConfig;
import io.openems.common.websocket.AbstractWebsocketClient;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.api.common.ApiWorker;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.Api.Backend", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"org.ops4j.pax.logging.appender.name=Controller.Api.Backend", //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CONFIG_UPDATE //
		} //
)
public class BackendApi extends AbstractOpenemsComponent
		implements Controller, OpenemsComponent, PaxAppender, EventHandler {

	protected static final int DEFAULT_NO_OF_CYCLES = 10;
	protected static final String COMPONENT_NAME = "Controller.Api.Backend";

	protected final BackendWorker worker = new BackendWorker(this);

	protected final ApiWorker apiWorker = new ApiWorker();

	private final Logger log = LoggerFactory.getLogger(BackendApi.class);

	protected WebsocketClient websocket = null;
	protected int noOfCycles = DEFAULT_NO_OF_CYCLES; // default, is going to be overwritten by config
	protected boolean debug = false;

	// Used for SubscribeSystemLogRequests
	private boolean isSystemLogSubscribed = false;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference
	protected ComponentManager componentManager;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		;
		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public BackendApi() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.noOfCycles = config.noOfCycles();
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
		
		String[] splitkey = config.apikey().split("MAC");
		
		httpHeaders.put("apikey", splitkey[0]);
		if(splitkey.length == 2) {
			httpHeaders.put("mac", splitkey[1]);
		}
		httpHeaders.put("pwd", "g6J:X)JE,VC?-@Y!");
		httpHeaders.put("version", OpenemsConstants.VERSION.toString());
		

		// Create Websocket instance
		this.websocket = new WebsocketClient(this, COMPONENT_NAME + ":" + this.id(), uri, httpHeaders, proxy);
		this.websocket.start();

		// Activate worker
		this.worker.activate(config.id());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.worker.deactivate();
		if (this.websocket != null) {
			this.websocket.stop();
		}
	}

	@Override
	public void run() throws OpenemsNamedException {
		this.apiWorker.run();
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

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.worker.triggerNextRun();
			break;

		case EdgeEventConstants.TOPIC_CONFIG_UPDATE:
			EdgeConfig config = (EdgeConfig) event.getProperty(EdgeEventConstants.TOPIC_CONFIG_UPDATE_KEY);
			EdgeConfigNotification message = new EdgeConfigNotification(config);
			WebsocketClient ws = this.websocket;
			if (ws == null) {
				return;
			}
			ws.sendMessage(message);
		}
	}
}
