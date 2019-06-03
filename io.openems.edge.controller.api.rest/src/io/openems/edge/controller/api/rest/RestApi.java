package io.openems.edge.controller.api.rest;

import org.eclipse.jetty.server.Server;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.user.UserService;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.api.core.ApiWorker;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Api.Rest", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE)
public class RestApi extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	public final static boolean DEFAULT_DEBUG_MODE = true;

	private final Logger log = LoggerFactory.getLogger(RestApi.class);

	protected final ApiWorker apiWorker = new ApiWorker();

	private Server server = null;
	private boolean isDebugModeEnabled = DEFAULT_DEBUG_MODE;

	@Reference
	protected ComponentManager componentManager;

	@Reference
	protected UserService userService;

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

	public RestApi() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.isDebugModeEnabled = config.debugMode();

		if (!this.isEnabled()) {
			// abort if disabled
			return;
		}

		this.apiWorker.setTimeoutSeconds(config.apiTimeout());

		/*
		 * Start RestApi-Server
		 */
		try {
			this.server = new Server(config.port());
			this.server.setHandler(new RestHandler(this));
			this.server.start();
			this.logInfo(this.log, "REST-Api started on port [" + config.port() + "].");
		} catch (Exception e) {
			throw new OpenemsException("REST-Api failed on port [" + config.port() + "].", e);
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		if (this.server != null) {
			try {
				this.server.stop();
			} catch (Exception e) {
				this.logWarn(this.log, "REST-Api failed to stop: " + e.getMessage());
			}
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
		super.logWarn(log, message);
	}

	@Override
	protected void logError(Logger log, String message) {
		super.logError(log, message);
	}

	protected boolean isDebugModeEnabled() {
		return isDebugModeEnabled;
	}
}
