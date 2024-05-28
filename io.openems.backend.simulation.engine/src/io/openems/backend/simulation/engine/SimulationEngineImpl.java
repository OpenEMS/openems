package io.openems.backend.simulation.engine;

import java.util.concurrent.CompletableFuture;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.backend.common.edgewebsocket.EdgeWebsocket;
import io.openems.backend.common.jsonrpc.SimulationEngine;
import io.openems.backend.common.jsonrpc.request.SimulationRequest;
import io.openems.backend.common.metadata.Metadata;
import io.openems.backend.common.metadata.User;
import io.openems.backend.common.timedata.TimedataManager;
import io.openems.backend.simulation.engine.possiblebatterycapacityextension.PossibleBatteryCapacityExtensionRequest;
import io.openems.backend.simulation.engine.possiblebatterycapacityextension.PossibleBatteryCapacityExtensionUtil;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Simulation.Engine", //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		immediate = true //
)
public class SimulationEngineImpl extends AbstractOpenemsBackendComponent implements SimulationEngine {

	private final Logger log = LoggerFactory.getLogger(SimulationEngineImpl.class);

	@Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.DYNAMIC)
	private volatile TimedataManager timedatamanager;

	@Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.DYNAMIC)
	private volatile EdgeWebsocket edgeWebsocket;

	@Reference
	protected volatile Metadata metadata;

	public SimulationEngineImpl() {
		super("SimulationEngine");
	}

	@Activate
	private void activate(Config config) {
		super.logInfo(this.log, "Activate");
	}

	@Deactivate
	private void deactivate() {
		this.logInfo(this.log, "Deactivate");
	}

	@Override
	public CompletableFuture<JsonrpcResponseSuccess> handleRequest(String edgeId, User user, SimulationRequest request)
			throws OpenemsNamedException {

		return switch (request.getPayload().getMethod()) {
		case PossibleBatteryCapacityExtensionRequest.METHOD ->
			PossibleBatteryCapacityExtensionUtil.handleRequest(this.timedatamanager, user, edgeId, this.metadata,
					this.edgeWebsocket, PossibleBatteryCapacityExtensionRequest.from(request));
		default -> {
			this.logWarn(this.log, "Unhandled Request: " + request);
			throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
		}
		};
	}
}