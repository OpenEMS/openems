package io.openems.edge.controller.api.websocket;

import java.io.IOException;
import java.util.List;
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

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.user.UserService;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.api.core.ApiController;
import io.openems.edge.controller.api.core.ApiWorker;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Api.Websocket", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = "org.ops4j.pax.logging.appender.name=Controller.Api.Websocket")
public class WebsocketApi extends AbstractOpenemsComponent
		implements Controller, ApiController, OpenemsComponent, PaxAppender {

	final Logger log = LoggerFactory.getLogger(WebsocketApi.class);
	private final ApiWorker apiWorker = new ApiWorker();

	private WebsocketApiServer websocketApiServer = null;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE, target = "(!(service.factoryPid=Controller.Api.Websocket))")
	protected volatile List<OpenemsComponent> components = new CopyOnWriteArrayList<>();

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	protected volatile Timedata timedataService = null;

	@Reference
	protected ConfigurationAdmin configAdmin;

	@Reference
	protected UserService userService;

	public WebsocketApi() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}
	
	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.service_pid(), config.id(), config.enabled());

		if (!this.isEnabled()) {
			// abort if disabled
			return;
		}

		this.apiWorker.setTimeoutSeconds(config.apiTimeout());

		/*
		 * Start WebsocketApiServer
		 */
		this.websocketApiServer = new WebsocketApiServer(this, config.port());
		this.websocketApiServer.start();
		this.logInfo(this.log, "Websocket-Api started on port [" + config.port() + "].");
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		if (this.websocketApiServer != null) {
			try {
				this.websocketApiServer.stop();
			} catch (IOException | InterruptedException e) {
				this.logError(this.log, "Error while closing websocket: " + e.getMessage());
			}
		}
	}

	@Override
	public void run() {
		this.apiWorker.run();
	}

	@Override
	public Timedata getTimedataService() {
		return this.timedataService;
	}

	@Override
	public List<OpenemsComponent> getComponents() {
		return this.components;
	}

	@Override
	public ConfigurationAdmin getConfigurationAdmin() {
		return this.configAdmin;
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
	public void doAppend(PaxLoggingEvent event) {
		this.websocketApiServer.sendLog(event);
	}
}
