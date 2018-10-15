package io.openems.edge.controller.api.modbus;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.api.core.ApiController;
import io.openems.edge.controller.api.core.ApiWorker;
import io.openems.edge.timedata.api.Timedata;

import org.osgi.service.metatype.annotations.Designate;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Api.Rest", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ModbusTcpApi extends AbstractOpenemsComponent implements Controller, ApiController, OpenemsComponent {

//	private final Logger log = LoggerFactory.getLogger(ModbusTcpApi.class);
	private final ApiWorker apiWorker = new ApiWorker();

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE, target = "(!(service.factoryPid=Controller.Api.Websocket))")
	protected volatile List<OpenemsComponent> components = new CopyOnWriteArrayList<>();

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	protected volatile Timedata timedataService = null;

	@Reference
	protected ConfigurationAdmin configAdmin;

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
//		this.component = new org.restlet.Component();
//		this.component.getServers().add(Protocol.HTTP, config.port());
//		this.component.getDefaultHost().attach("/rest", new RestApiApplication(this));
//		try {
//			this.component.start();
//			this.logInfo(this.log, "REST-Api started on port [" + config.port() + "].");
//		} catch (Exception e) {
//			throw new OpenemsException("REST-Api failed on port [" + config.port() + "].", e);
//		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
//		if (this.component != null) {
//			try {
//				this.component.stop();
//			} catch (Exception e) {
//				this.logWarn(this.log, "REST-Api failed to stop: " + e.getMessage());
//			}
//		}
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
}
