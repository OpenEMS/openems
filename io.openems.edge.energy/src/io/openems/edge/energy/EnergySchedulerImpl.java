package io.openems.edge.energy;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.openems.edge.energy.optimizer.Utils.sortByScheduler;
import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.List;
import java.util.Objects;
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

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.energy.api.EnergySchedulable;
import io.openems.edge.energy.api.EnergyScheduler;
import io.openems.edge.energy.api.handler.AbstractEnergyScheduleHandler;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.optimizer.Optimizer;
import io.openems.edge.predictor.api.manager.PredictorManager;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timeofusetariff.api.TariffManager;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = EnergyScheduler.SINGLETON_SERVICE_PID, //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.OPTIONAL //
)
public class EnergySchedulerImpl extends AbstractOpenemsComponent
		implements OpenemsComponent, EnergyScheduler, ComponentJsonApi {

	private final Optimizer optimizer;

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private Meta meta;

	@Reference
	private ComponentManager componentManager;

	@Reference
	private PredictorManager predictorManager;

	@Reference
	private TariffManager tariffManager;

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
			esh.setOnRescheduleCallback(//
					(reason, rescheduleMode) -> this.optimizer.restartOptimization(//
							reason, //
							rescheduleMode.optimizeCurrentPeriod()));
		}
		this.optimizer.restartOptimization("EnergySchedulerImpl::addSchedulable() " + schedulable.id(), false);
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

	private Config config;

	public EnergySchedulerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				EnergyScheduler.ChannelId.values() //
		);

		this.optimizer = new Optimizer(//
				() -> this.config.logVerbosity(), //
				() -> {
					// Sort Schedulables by the order in the Scheduler
					var schedulables = sortByScheduler(this.scheduler, this.schedulables);
					var eshs = schedulables.stream() //
							.map(EnergySchedulable::getEnergyScheduleHandler) //
							.filter(Objects::nonNull) //
							.collect(toImmutableList());
					return GlobalOptimizationContext.builder() //
							.setComponentManager(this.componentManager) //
							.setMeta(this.meta) //
							.setEnvironment(this.config.environment()) //
							.setEnergyScheduleHandlers(eshs) //
							.setSum(this.sum) //
							.setPredictorManager(this.predictorManager) //
							.setTariffManager(this.tariffManager) //
							.build();
				}, //
				this.channel(EnergyScheduler.ChannelId.SIMULATIONS_PER_QUARTER), //
				this.channel(EnergyScheduler.ChannelId.GENERATIONS_PER_QUARTER));
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, SINGLETON_COMPONENT_ID, SINGLETON_SERVICE_PID, true);
		this.applyConfig(config, "EnergySchedulerImpl::activate()");
	}

	@Modified
	private void modified(ComponentContext context, Config config) {
		super.modified(context, SINGLETON_COMPONENT_ID, SINGLETON_SERVICE_PID, true);
		this.applyConfig(config, "EnergySchedulerImpl::modified()");
	}

	@Override
	@Deactivate
	protected void deactivate() {
		this.optimizer.deactivate();
		super.deactivate();
	}

	private synchronized void applyConfig(Config config, String reason) {
		this.config = config;
		if (OpenemsComponent.validateSingleton(this.cm, SINGLETON_SERVICE_PID, SINGLETON_COMPONENT_ID)) {
			return;
		}

		if (!config.enabled()) {
			this.optimizer.deactivate();
			return;
		}

		if (this.optimizer.isActivated()) {
			this.optimizer.restartOptimization(reason, false);
		} else {
			this.optimizer.activate(); // Starts optimization immediately
		}
	}

	@Override
	public String debugLog() {
		return this.optimizer.debugLog();
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.handleRequest(new GetSchedule(), //
				endpoint -> endpoint //
						.setDescription("""
								Gets the Schedule.
								""".stripIndent()), //
				call -> GetSchedule.Response.from(call.getRequest(), //
						this.componentManager.getClock(), //
						this.timedata, this.predictorManager, this.tariffManager, //
						this.optimizer.getLatestSimulationResult()));
	}
}
