package io.openems.edge.controller.api.backend;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.notification.EdgeConfigNotification;
import io.openems.common.jsonrpc.notification.SystemLogNotification;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.ThreadPoolUtils;
import io.openems.common.websocket.AbstractWebsocketClient;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.cycle.Cycle;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.user.User;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.api.common.ApiWorker;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.Api.Backend", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"org.ops4j.pax.logging.appender.name=Controller.Api.Backend", //
		} //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CONFIG_UPDATE //
})
public class BackendApiImpl extends AbstractOpenemsComponent
		implements BackendApi, Controller, JsonApi, OpenemsComponent, PaxAppender, EventHandler {

	protected static final String COMPONENT_NAME = "Controller.Api.Backend";

	protected final SendChannelValuesWorker sendChannelValuesWorker = new SendChannelValuesWorker(this);

	protected final SendAggregatedChannelValuesWorker sendAggregatedChannelValuesWorker //
			= new SendAggregatedChannelValuesWorker(this);

	protected final ApiWorker apiWorker = new ApiWorker(this);

	private final Logger log = LoggerFactory.getLogger(BackendApiImpl.class);

	protected WebsocketClient websocket = null;
	protected Config config;

	// Used for SubscribeSystemLogRequests
	private boolean isSystemLogSubscribed = false;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference
	protected ComponentManager componentManager;

	@Reference
	protected Cycle cycle;

	private ScheduledExecutorService executor;

	public BackendApiImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				BackendApi.ChannelId.values() //
		);
		this.apiWorker.setLogChannel(this.getApiWorkerLogChannel());
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled());

		if (!this.isEnabled()) {
			return;
		}

		/*
		 * FEMS specific check for Apikey
		 */
		boolean wrongApikeyConfiguration = false;
		if (!System.getProperty("os.name").startsWith("Windows") /* real FEMS device only */) {
			try (Scanner scanner = new Scanner(Paths.get("/etc/fems"))) {
				wrongApikeyConfiguration = !scanner.findAll("apikey=\"?(.*)\"?")//
						.map(mr -> mr.group(1)) //
						.anyMatch(fileApikey -> Objects.equals(fileApikey, config.apikey()));//
			} catch (IOException e) {
				wrongApikeyConfiguration = true;
				this.log.warn("Unable to read apikey from /etc/fems: " + e.getMessage());
			}
		}
		this.channel(BackendApi.ChannelId.WRONG_APIKEY_CONFIGURATION).setNextValue(wrongApikeyConfiguration);

		// initialize Executor
		var name = COMPONENT_NAME + ":" + this.id();
		this.executor = Executors.newScheduledThreadPool(10,
				new ThreadFactoryBuilder().setNameFormat(name + "-%d").build());

		// initialize ApiWorker
		this.apiWorker.setTimeoutSeconds(config.apiTimeout());

		// Get URI
		URI uri = null;
		try {
			uri = new URI(config.uri());
		} catch (URISyntaxException e) {
			this.log.error("URI [" + config.uri() + "] is invalid: " + e.getMessage());
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
		this.websocket = new WebsocketClient(this, name, uri, httpHeaders, proxy);
		this.websocket.start();

		this.sendAggregatedChannelValuesWorker.activate(this.id());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.sendChannelValuesWorker.deactivate();
		this.sendAggregatedChannelValuesWorker.deactivate();
		if (this.websocket != null) {
			this.websocket.stop();
		}
		ThreadPoolUtils.shutdownAndAwaitTermination(this.executor, 5);
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
		super.logWarn(log, message);
	}

	@Override
	protected void logError(Logger log, String message) {
		super.logError(log, message);
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
		var ws = this.websocket;
		if (ws == null) {
			return;
		}
		var notification = SystemLogNotification.fromPaxLoggingEvent(event);
		ws.sendMessage(notification);
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.sendChannelValuesWorker.collectData();
			break;

		case EdgeEventConstants.TOPIC_CONFIG_UPDATE:
			// Send new EdgeConfig
			var config = (EdgeConfig) event.getProperty(EdgeEventConstants.TOPIC_CONFIG_UPDATE_KEY);
			var message = new EdgeConfigNotification(config);
			var ws = this.websocket;
			if (ws == null) {
				return;
			}
			ws.sendMessage(message);

			// Trigger sending of all channel values, because a Component might have
			// disappeared
			this.sendChannelValuesWorker.sendValuesOfAllChannelsOnce();
		}
	}

	@Override
	public boolean isConnected() {
		return Optional.ofNullable(this.websocket) //
				.map(WebsocketClient::isConnected) //
				.orElse(false);
	}

	/**
	 * Execute a command using the {@link ScheduledExecutorService}.
	 *
	 * @param command a {@link Runnable}
	 */
	protected void execute(Runnable command) {
		if (!this.executor.isShutdown()) {
			this.executor.execute(command);
		}
	}

	/**
	 * Schedules a command using the {@link ScheduledExecutorService}.
	 *
	 * @param command      a {@link Runnable}
	 * @param initialDelay the initial delay
	 * @param delay        the delay
	 * @param unit         the {@link TimeUnit}
	 * @return a {@link ScheduledFuture}, or null if Executor is shutting down
	 */
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
		if (this.executor.isShutdown()) {
			return null;
		}
		return this.executor.scheduleWithFixedDelay(command, initialDelay, delay, unit);
	}

	@Override
	public CompletableFuture<? extends JsonrpcResponseSuccess> handleJsonrpcRequest(User user, JsonrpcRequest request)
			throws OpenemsNamedException {
		// delegates request to actual backend
		return this.websocket.sendRequest(request);
	}

}
