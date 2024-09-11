package io.openems.edge.energy;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.openems.common.utils.FunctionUtils.doNothing;
import static io.openems.edge.energy.optimizer.Utils.sortByScheduler;

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
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;
import io.openems.common.worker.AbstractImmediateWorker;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.Call;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffController;
import io.openems.edge.energy.api.EnergySchedulable;
import io.openems.edge.energy.api.EnergyScheduler;
import io.openems.edge.energy.api.simulation.GlobalSimulationsContext;
import io.openems.edge.energy.optimizer.Optimizer;
import io.openems.edge.energy.v1.jsonrpc.GetScheduleResponse;
import io.openems.edge.energy.v1.optimizer.GlobalContext;
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
public class EnergySchedulerImpl extends AbstractOpenemsComponent implements OpenemsComponent, EnergyScheduler {

	/** The hard working Optimizer. */
	// TODO should be final after v1
	private AbstractImmediateWorker optimizer;

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private ComponentManager componentManager;

	@Reference
	private PredictorManager predictorManager;

	@Reference(policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile TimeOfUseTariff timeOfUseTariff;

	@Reference
	private io.openems.edge.scheduler.api.Scheduler scheduler;

	@Reference
	private Sum sum;

	private final List<EnergySchedulable> schedulables = new CopyOnWriteArrayList<>();

	@Reference(policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			policy = ReferencePolicy.DYNAMIC, //
			target = "(enabled=true)")
	private void addSchedulable(EnergySchedulable schedulable) {
		this.schedulables.add(schedulable);
		this.resetOptimizer();
	}

	@SuppressWarnings("unused")
	private void removeSchedulable(EnergySchedulable schedulable) {
		this.schedulables.remove(schedulable);
		this.resetOptimizer();
	}

	@Reference(policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata;

	@Deprecated
	@Reference(policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL, target = "(enabled=true)")
	private volatile TimeOfUseTariffController timeOfUseTariffController;

	private Config config;

	public EnergySchedulerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				EnergyScheduler.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		super.activate(context, SINGLETON_COMPONENT_ID, SINGLETON_SERVICE_PID, true);

		// Prepare Optimizer and Context
		this.optimizer = switch (config.version()) {

		case V1_ESS_ONLY -> new OptimizerV1(() -> {
			if (this.timeOfUseTariff == null) {
				throw new OpenemsException("TimeOfUseTariff is not available");
			}
			if (this.timeOfUseTariffController == null) {
				throw new OpenemsException("TimeOfUseTariffController is not available");
			}
			return GlobalContext.create() //
					.setClock(this.componentManager.getClock()) //
					.setEnergyScheduleHandler(this.timeOfUseTariffController.getEnergyScheduleHandlerV1()) //
					.setSum(this.sum) //
					.setPredictorManager(this.predictorManager) //
					.setTimeOfUseTariff(this.timeOfUseTariff) //
					.build();
		});

		case V2_ENERGY_SCHEDULABLE -> new Optimizer(//
				(log, message) -> { // Log info
					switch (this.config.logVerbosity()) {
					case NONE, DEBUG_LOG -> doNothing();
					case TRACE -> this.logInfo(log, message);
					}
				}, //
				() -> {
					// Sort Schedulables by the order in the Scheduler
					var schedulables = sortByScheduler(this.scheduler, this.schedulables);
					var eshs = schedulables.stream() //
							.map(EnergySchedulable::getEnergyScheduleHandler) //
							.collect(toImmutableList());

					return GlobalSimulationsContext.create() //
							.setClock(this.componentManager.getClock()) //
							.setEnergyScheduleHandlers(eshs) //
							.setSum(this.sum) //
							.setPredictorManager(this.predictorManager) //
							.setTimeOfUseTariff(this.timeOfUseTariff) //
							.build();
				}, //
				this.channel(EnergyScheduler.ChannelId.SIMULATIONS_PER_QUARTER));
		};

		if (this.applyConfig(config)) {
			this.resetOptimizer();
		}
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		super.modified(context, SINGLETON_COMPONENT_ID, SINGLETON_SERVICE_PID, true);
		if (this.applyConfig(config)) {
			this.resetOptimizer();
		}
	}

	private void resetOptimizer() {
		if (this.config == null) {
			return; // Catch early race condition
		}
		this.optimizer.modified(this.id());
	}

	@Override
	public String debugLog() {
		if (this.optimizer instanceof Optimizer o) {
			return switch (this.config.logVerbosity()) {
			case NONE -> null;
			case DEBUG_LOG, TRACE -> o.debugLog();
			};
		}
		return null;
	}

	private synchronized boolean applyConfig(Config config) {
		this.config = config;
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
	public GetScheduleResponse handleGetScheduleRequestV1(Call<JsonrpcRequest, JsonrpcResponse> call, String id) {
		return UtilsV1.handleGetScheduleRequest((OptimizerV1) this.optimizer, call.getRequest().getId(), this.timedata,
				this.timeOfUseTariff, id, ZonedDateTime.now(this.componentManager.getClock()));
	}
}
