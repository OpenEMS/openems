package io.openems.edge.energy;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.openems.edge.energy.optimizer.Utils.sortByScheduler;
import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.Call;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffController;
import io.openems.edge.energy.api.EnergySchedulable;
import io.openems.edge.energy.api.EnergyScheduler;
import io.openems.edge.energy.api.Version;
import io.openems.edge.energy.api.handler.AbstractEnergyScheduleHandler;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.optimizer.Optimizer;
import io.openems.edge.energy.v1.jsonrpc.GetScheduleResponse;
import io.openems.edge.energy.v1.optimizer.GlobalContextV1;
import io.openems.edge.energy.v1.optimizer.OptimizerV1;
import io.openems.edge.energy.v1.optimizer.UtilsV1;
import io.openems.edge.predictor.api.manager.PredictorManager;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = EnergyScheduler.SINGLETON_SERVICE_PID, //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.OPTIONAL //
)
@SuppressWarnings("deprecation")
public class EnergySchedulerImpl extends AbstractOpenemsComponent implements OpenemsComponent, EnergyScheduler {

	/** The hard working Optimizer. */
	private final OptimizerV1 optimizerV1;
	private final Optimizer optimizer;

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private ComponentManager componentManager;

	@Reference
	private PredictorManager predictorManager;

	@Reference(policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile TimeOfUseTariff timeOfUseTariff;

	@Reference
	private io.openems.edge.scheduler.api.Scheduler scheduler;

	@Reference
	private Sum sum;

	private final List<EnergySchedulable> schedulables = new CopyOnWriteArrayList<>();

	@Reference(policyOption = GREEDY, cardinality = MULTIPLE, policy = DYNAMIC, target = "(enabled=true)")
	private void addSchedulable(EnergySchedulable schedulable) {
		this.schedulables.add(schedulable);
		var esh = (AbstractEnergyScheduleHandler<?, ?>) schedulable.getEnergyScheduleHandler(); // this is safe
		if (esh != null) {
			esh.setOnRescheduleCallback(reason -> this.triggerReschedule(reason));
		}
		this.triggerReschedule("EnergySchedulerImpl::addSchedulable() " + schedulable.id());
	}

	@SuppressWarnings("unused")
	private void removeSchedulable(EnergySchedulable schedulable) {
		this.schedulables.remove(schedulable);
		var esh = (AbstractEnergyScheduleHandler<?, ?>) schedulable.getEnergyScheduleHandler(); // this is safe
		if (esh != null) {
			esh.removeOnRescheduleCallback();
		}
	}

	@Reference(policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata;

	@Deprecated
	@Reference(policyOption = GREEDY, cardinality = OPTIONAL, target = "(enabled=true)")
	private volatile TimeOfUseTariffController timeOfUseTariffController;

	private Config config;

	public EnergySchedulerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				EnergyScheduler.ChannelId.values() //
		);

		this.optimizerV1 = new OptimizerV1(//
				() -> this.config.logVerbosity(), //
				() -> {
					if (this.timeOfUseTariff == null) {
						throw new OpenemsException("TimeOfUseTariff is not available");
					}
					if (this.timeOfUseTariffController == null) {
						throw new OpenemsException("TimeOfUseTariffController is not available");
					}
					return GlobalContextV1.create() //
							.setClock(this.componentManager.getClock()) //
							.setEnergyScheduleHandler(this.timeOfUseTariffController.getEnergyScheduleHandlerV1()) //
							.setSum(this.sum) //
							.setPredictorManager(this.predictorManager) //
							.setTimeOfUseTariff(this.timeOfUseTariff) //
							.build();
				});

		this.optimizer = new Optimizer(//
				() -> this.config.logVerbosity(), //
				() -> {
					// Sort Schedulables by the order in the Scheduler
					var schedulables = sortByScheduler(this.scheduler, this.schedulables);
					var eshs = schedulables.stream() //
							.map(EnergySchedulable::getEnergyScheduleHandler) //
							.filter(Objects::nonNull) //
							.collect(toImmutableList());
					return GlobalOptimizationContext.create() //
							.setComponentManager(this.componentManager) //
							.setRiskLevel(this.config.riskLevel()) //
							.setEnergyScheduleHandlers(eshs) //
							.setSum(this.sum) //
							.setPredictorManager(this.predictorManager) //
							.setTimeOfUseTariff(this.timeOfUseTariff) //
							.build();
				}, //
				this.channel(EnergyScheduler.ChannelId.SIMULATIONS_PER_QUARTER));
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		super.activate(context, SINGLETON_COMPONENT_ID, SINGLETON_SERVICE_PID, true);
		this.applyConfig(config, "activate");
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		super.modified(context, SINGLETON_COMPONENT_ID, SINGLETON_SERVICE_PID, true);
		this.applyConfig(config, "modified");
	}

	@Override
	@Deactivate
	protected void deactivate() {
		this.optimizerV1.deactivate();
		this.optimizer.deactivate();
		super.deactivate();
	}

	private synchronized void applyConfig(Config config, String reason) {
		this.config = config;
		if (OpenemsComponent.validateSingleton(this.cm, SINGLETON_SERVICE_PID, SINGLETON_COMPONENT_ID)) {
			return;
		}

		if (!config.enabled()) {
			this.optimizerV1.deactivate();
			this.optimizer.interruptTask();
			return;
		}

		this.triggerReschedule("EnergySchedulerImpl::applyConfig() " + reason);
	}

	private void triggerReschedule(String reason) {
		if (this.config == null) {
			return; // Wait for @Activate
		}
		switch (this.config.version()) {
		case V1_ESS_ONLY -> {
			this.optimizer.interruptTask();
			this.optimizerV1.activate(this.id());
		}
		case V2_ENERGY_SCHEDULABLE -> {
			this.optimizerV1.deactivate();
			this.optimizer.triggerReschedule(reason);
		}
		}
	}

	@Override
	public String debugLog() {
		if (this.config != null && this.config.version() == Version.V2_ENERGY_SCHEDULABLE) {
			return this.optimizer.debugLog();
		}
		return null;
	}

	@Override
	public GetScheduleResponse handleGetScheduleRequestV1(Call<JsonrpcRequest, JsonrpcResponse> call, String id) {
		if (this.optimizerV1 != null) {
			return UtilsV1.handleGetScheduleRequest(this.optimizerV1, call.getRequest().getId(), this.timedata,
					this.timeOfUseTariff, id, ZonedDateTime.now(this.componentManager.getClock()));
		}
		throw new IllegalArgumentException("This should have been Version V1");
	}

	@Override
	public Version getImplementationVersion() {
		return Optional.ofNullable(this.config) //
				.map(c -> c.version()) //
				.orElse(null);
	}
}
