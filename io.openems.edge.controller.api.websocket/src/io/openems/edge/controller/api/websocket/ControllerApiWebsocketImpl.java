package io.openems.edge.controller.api.websocket;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

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

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.notification.EdgeConfigNotification;
import io.openems.common.jsonrpc.notification.EdgeRpcNotification;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.ThreadPoolUtils;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.user.UserService;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.api.common.ApiWorker;
import io.openems.edge.controller.api.common.handler.ComponentConfigRequestHandler;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Api.Websocket", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CONFIG_UPDATE, //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class ControllerApiWebsocketImpl extends AbstractOpenemsComponent
		implements ControllerApiWebsocket, Controller, OpenemsComponent, EventHandler {

	private static final int POOL_SIZE = 10;

	protected final ApiWorker apiWorker = new ApiWorker(this);

	@Reference
	protected ComponentManager componentManager;

	@Reference
	protected UserService userService;

	protected WebsocketServer server = null;

	private ScheduledExecutorService executor;

	@Reference
	protected OnRequest.Factory onRequestFactory;
	protected OnRequest onRequest;

	public ControllerApiWebsocketImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerApiWebsocket.ChannelId.values() //
		);
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

		this.onRequest = this.onRequestFactory.get();
		this.onRequest.setOnCall(call -> {
			call.put(ComponentConfigRequestHandler.API_WORKER_KEY, this.apiWorker);
		});
		this.onRequest.setDebug(config.debugMode());
		this.startServer(config.port(), POOL_SIZE);

	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.stopServer();
		this.onRequestFactory.unget(this.onRequest);
		ThreadPoolUtils.shutdownAndAwaitTermination(this.executor, 5);
	}

	/**
	 * Create and start new server.
	 *
	 * @param port     the port
	 * @param poolSize number of threads dedicated to handle the tasks
	 */
	private synchronized void startServer(int port, int poolSize) {
		this.server = new WebsocketServer(this, "Websocket Api", port, poolSize);
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

}
