package io.openems.edge.controller.api.backend;

import static io.openems.common.utils.StringUtils.definedOrElse;

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

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
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
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.ThreadPoolUtils;
import io.openems.common.websocket.AbstractWebsocketClient;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.cycle.Cycle;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.jsonapi.EdgeKeys;
import io.openems.edge.common.jsonapi.Key;
import io.openems.edge.common.user.User;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.api.backend.api.ControllerApiBackend;
import io.openems.edge.controller.api.common.ApiWorker;
import io.openems.edge.controller.api.common.handler.ComponentConfigRequestHandler;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Api.Backend", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CONFIG_UPDATE //
})
public class ControllerApiBackendImpl extends AbstractOpenemsComponent
		implements ControllerApiBackend, Controller, OpenemsComponent, EventHandler {

	protected static final String COMPONENT_NAME = "Controller.Api.Backend";

	public static final Key<WebsocketClient> WEBSOCKET_CLIENT_KEY = new Key<>("websocketClient", WebsocketClient.class);

	protected final SendChannelValuesWorker sendChannelValuesWorker = new SendChannelValuesWorker(this);
	protected final ApiWorker apiWorker = new ApiWorker(this);

	private final Logger log = LoggerFactory.getLogger(ControllerApiBackendImpl.class);

	@Reference
	private OpenemsEdgeOem oem;
	@Reference
	protected ComponentManager componentManager;
	@Reference
	protected Cycle cycle;

	@Reference
	private ResendHistoricDataWorkerFactory resendHistoricDataWorkerFactory;
	protected ResendHistoricDataWorker resendHistoricDataWorker;

	@Reference
	private BackendOnRequest.Factory requestHandlerFactory;
	protected BackendOnRequest requestHandler;

	protected WebsocketClient websocket = null;
	protected Config config;
	/** Used for SubscribeSystemLogRequests. */
	private ScheduledExecutorService executor;

	public ControllerApiBackendImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerApiBackend.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled());

		this.apiWorker.setLogChannel(this.getApiWorkerLogChannel());
		this.resendHistoricDataWorker = this.resendHistoricDataWorkerFactory.get();
		this.requestHandler = this.requestHandlerFactory.get();

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
		this.channel(ControllerApiBackend.ChannelId.WRONG_APIKEY_CONFIGURATION).setNextValue(wrongApikeyConfiguration);

		// initialize Executor
		var name = COMPONENT_NAME + ":" + this.id();
		this.executor = Executors.newScheduledThreadPool(10,
				new ThreadFactoryBuilder().setNameFormat(name + "-%d").build());

		// initialize ApiWorker
		this.apiWorker.setTimeoutSeconds(config.apiTimeout());

		// Get URI
		URI uri = null;
		try {
			uri = new URI(definedOrElse(config.uri(), this.oem.getBackendApiUrl()));
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

		this.resendHistoricDataWorker = this.resendHistoricDataWorkerFactory.get();
		this.resendHistoricDataWorker.setConfig(new ResendHistoricDataWorker.Config(//
				this.getUnableToSendChannel().address(), //
				this.getLastSuccessFulResendChannel().address(), //
				config.resendPriority(), //
				t -> this.getLastSuccessFulResendChannel().setNextValue(t), //
				t -> this.websocket.sendMessage(t) //
		));
		this.resendHistoricDataWorker.activate(this.id(), false);

		this.requestHandler.setOnCall(call -> {
			call.put(WEBSOCKET_CLIENT_KEY, this.websocket);
			call.put(ComponentConfigRequestHandler.API_WORKER_KEY, this.apiWorker);
			call.put(EdgeKeys.IS_FROM_BACKEND_KEY, true);
		});
		this.requestHandler.setDebug(config.debugMode());
	}

	@Override
	@Deactivate
	protected synchronized void deactivate() {
		super.deactivate();
		this.resendHistoricDataWorkerFactory.unget(this.resendHistoricDataWorker);
		this.resendHistoricDataWorker = null;
		this.sendChannelValuesWorker.deactivate();
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

	@Override
	public void handleEvent(Event event) {
		try {
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
		} catch (Exception e) {
			e.printStackTrace();
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
	public CompletableFuture<? extends JsonrpcResponseSuccess> sendRequest(User user, JsonrpcRequest request)
			throws OpenemsNamedException {
		return this.websocket.sendRequest(request);
	}

}
