package io.openems.edge.controller.api.websocket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.java_websocket.WebSocket;
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

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.notification.EdgeConfigNotification;
import io.openems.common.jsonrpc.notification.EdgeRpcNotification;
import io.openems.common.jsonrpc.request.SubscribeSystemLogRequest;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.ThreadPoolUtils;
import io.openems.common.websocket.AbstractWebsocketServer.DebugMode;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.user.User;
import io.openems.edge.common.user.UserService;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.api.common.ApiWorker;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Api.Websocket", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"org.ops4j.pax.logging.appender.name=Controller.Api.Websocket" //
		})
@EventTopics({ //
		EdgeEventConstants.TOPIC_CONFIG_UPDATE, //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class ControllerApiWebsocketImpl extends AbstractOpenemsComponent
		implements ControllerApiWebsocket, Controller, OpenemsComponent, PaxAppender, EventHandler {

	private static final int POOL_SIZE = 10;

	/** Stores valid session tokens for authentication via Cookie. */
	protected final Map<String, User> sessionTokens = new ConcurrentHashMap<>();
	protected final ApiWorker apiWorker = new ApiWorker(this);

	private final SystemLogHandler systemLogHandler;

	@Reference
	protected ComponentManager componentManager;

	@Reference
	protected UserService userService;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	protected WebsocketServer server = null;

	private ScheduledExecutorService executor;

	public ControllerApiWebsocketImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerApiWebsocket.ChannelId.values() //
		);
		this.systemLogHandler = new SystemLogHandler(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		if (!this.isEnabled()) {
			// abort if disabled
			return;
		}

		// initialize Executor
		var name = "Controller.Api.Websocket" + ":" + this.id();
		this.executor = Executors.newScheduledThreadPool(10,
				new ThreadFactoryBuilder().setNameFormat(name + "-%d").build());

		this.apiWorker.setTimeoutSeconds(config.apiTimeout());
		this.startServer(config.port(), POOL_SIZE, DebugMode.OFF);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.stopServer();
		ThreadPoolUtils.shutdownAndAwaitTermination(this.executor, 5);
	}

	/**
	 * Create and start new server.
	 *
	 * @param port      the port
	 * @param poolSize  number of threads dedicated to handle the tasks
	 * @param debugMode activate a regular debug log about the state of the tasks
	 */
	private synchronized void startServer(int port, int poolSize, DebugMode debugMode) {
		this.server = new WebsocketServer(this, "Websocket Api", port, poolSize, debugMode);
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
	public void run() throws OpenemsNamedException {
		this.apiWorker.run();
	}

	@Override
	protected final void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}

	@Override
	protected final void logWarn(Logger log, String message) {
		super.logWarn(log, message);
	}

	@Override
	protected void logError(Logger log, String message) {
		super.logError(log, message);
	}

	@Override
	public void doAppend(PaxLoggingEvent event) {
		this.systemLogHandler.handlePaxLoggingEvent(event);
	}

	/**
	 * Gets the WebSocket connection attachment for a UI token.
	 *
	 * @param token the UI token
	 * @return the WsData
	 * @throws OpenemsNamedException if there is no connection with this token
	 */
	protected WsData getWsDataForTokenOrError(String token) throws OpenemsNamedException {
		var connections = this.server.getConnections();
		for (WebSocket websocket : connections) {
			WsData wsData = websocket.getAttachment();
			var thisToken = wsData.getSessionToken();
			if (thisToken != null && thisToken.equals(token)) {
				return wsData;
			}
		}
		throw OpenemsError.BACKEND_NO_UI_WITH_TOKEN.exception(token);
	}

	/**
	 * Handles a SubscribeSystemLogRequest by forwarding it to the
	 * 'SystemLogHandler'.
	 *
	 * @param token   the UI token
	 * @param request the SubscribeSystemLogRequest
	 * @throws OpenemsNamedException on error
	 */
	protected void handleSubscribeSystemLogRequest(String token, SubscribeSystemLogRequest request)
			throws OpenemsNamedException {
		this.systemLogHandler.handleSubscribeSystemLogRequest(token, request);
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CONFIG_UPDATE:
			if (this.server.getConnections().isEmpty()) {
				// No Connections? It's not required to build the EdgeConfig.
				return;
			}
			var config = (EdgeConfig) event.getProperty(EdgeEventConstants.TOPIC_CONFIG_UPDATE_KEY);
			var message = new EdgeConfigNotification(config);
			this.server.broadcastMessage(new EdgeRpcNotification(ControllerApiWebsocketImpl.EDGE_ID, message));
			break;

		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			for (var ws : this.server.getConnections()) {
				WsData wsData = ws.getAttachment();
				wsData.sendSubscribedChannels();
			}
			break;
		}
	}

	/**
	 * Gets the Timedata service.
	 *
	 * @return the service
	 * @throws OpenemsException if the timeservice is not available
	 */
	public Timedata getTimedata() throws OpenemsException {
		if (this.timedata != null) {
			return this.timedata;
		}
		throw new OpenemsException("There is no Timedata-Service available!");
	}
}
