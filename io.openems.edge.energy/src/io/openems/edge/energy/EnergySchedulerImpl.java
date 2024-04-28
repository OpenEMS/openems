package io.openems.edge.energy;

import static io.openems.edge.energy.optimizer.Utils.handleGetScheduleRequest;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffControllerImpl;
import io.openems.edge.energy.api.EnergySchedulable;
import io.openems.edge.energy.api.EnergyScheduler;
import io.openems.edge.energy.jsonrpc.GetScheduleRequest;
import io.openems.edge.energy.optimizer.GlobalContext;
import io.openems.edge.energy.optimizer.Optimizer;
import io.openems.edge.predictor.api.manager.PredictorManager;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = EnergyScheduler.SINGLETON_SERVICE_PID, //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.OPTIONAL //
)
public class EnergySchedulerImpl extends AbstractOpenemsComponent
		implements OpenemsComponent, EnergyScheduler, ComponentJsonApi {

	/** The hard working Optimizer. */
	private final Optimizer optimizer;

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private ComponentManager componentManager;

	@Reference
	private PredictorManager predictorManager;

	@Reference
	private TimeOfUseTariff timeOfUseTariff;

	@Reference
	private Sum sum;

	@Reference(policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			policy = ReferencePolicy.DYNAMIC, //
			target = "(enabled=true)")
	private volatile List<EnergySchedulable<?, ?>> schedulables = new CopyOnWriteArrayList<>();

	@Reference(policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata;

	public EnergySchedulerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				EnergyScheduler.ChannelId.values() //
		);
		// Prepare Optimizer and Context
		this.optimizer = new Optimizer(() -> {
			var ctrl = this.schedulables.stream() //
					.filter(TimeOfUseTariffControllerImpl.class::isInstance) //
					.map(TimeOfUseTariffControllerImpl.class::cast) //
					.findFirst().orElse(null);
			if (ctrl == null) {
				throw new OpenemsException("TimeOfUseTariffController is not available");
			}
			var esh = ctrl.getEnergyScheduleHandler();
			// NOTE: This is a workaround while we refactor TimeOfUseTariffController
			// This code assumes that the `EnergySchedulable` is a
			// `TimeOfUseTariffController`
			return GlobalContext.create() //
					.setClock(this.componentManager.getClock()) //
					.setEnergyScheduleHandler(esh) //
					.setSum(this.sum) //
					.setPredictorManager(this.predictorManager) //
					.setTimeOfUseTariff(this.timeOfUseTariff) //
					.build();
		});
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		super.activate(context, SINGLETON_COMPONENT_ID, SINGLETON_SERVICE_PID, true);
		if (this.applyConfig(config)) {
			this.optimizer.activate(this.id());
		}
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		super.modified(context, SINGLETON_COMPONENT_ID, SINGLETON_SERVICE_PID, true);
		if (this.applyConfig(config)) {
			this.optimizer.modified(this.id());
		}
	}

	private synchronized boolean applyConfig(Config config) {
		if (OpenemsComponent.validateSingleton(this.cm, SINGLETON_SERVICE_PID, SINGLETON_COMPONENT_ID)) {
			return false;
		}

		if (!config.enabled()) {
			this.optimizer.deactivate();
			return false;
		}

		return true;
	}

	@Override
	@Deactivate
	protected void deactivate() {
		this.optimizer.deactivate();
		super.deactivate();
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.handleRequest(GetScheduleRequest.METHOD, call -> handleGetScheduleRequest(//
				this.optimizer, call.getRequest().getId(), this.timedata, this.timeOfUseTariff,
				"ctrlEssTimeOfUseTariff0", ZonedDateTime.now(this.componentManager.getClock())));
	}
}
