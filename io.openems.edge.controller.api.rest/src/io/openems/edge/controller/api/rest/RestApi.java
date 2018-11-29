package io.openems.edge.controller.api.rest;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.user.UserService;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.api.core.ApiController;
import io.openems.edge.controller.api.core.ApiWorker;
import io.openems.edge.timedata.api.Timedata;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.jetty.server.Server;
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

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Api.Rest", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE)
public class RestApi extends AbstractOpenemsComponent implements Controller, ApiController, OpenemsComponent {

	final Logger log = LoggerFactory.getLogger(RestApi.class);
	private final ApiWorker apiWorker = new ApiWorker();

	private Server server = null;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE, target = "(!(service.factoryPid=Controller.Api.Websocket))")
	protected volatile List<OpenemsComponent> components = new CopyOnWriteArrayList<>();

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	protected volatile Timedata timedataService = null;

	@Reference
	protected ConfigurationAdmin configAdmin;

	@Reference
	protected UserService userService;

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		super.activate(context, config.service_pid(), config.id(), config.enabled());

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
	public void run() {
		this.apiWorker.run();
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
	public Timedata getTimedataService() {
		return this.timedataService;
	}

	@Override
	protected void logWarn(Logger log, String message) {
		super.logWarn(log, message);
	}
}
