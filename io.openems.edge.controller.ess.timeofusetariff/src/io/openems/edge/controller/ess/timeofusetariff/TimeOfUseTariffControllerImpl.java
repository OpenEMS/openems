package io.openems.edge.controller.ess.timeofusetariff;

import static io.openems.common.utils.FunctionUtils.doNothing;
import static io.openems.edge.controller.ess.timeofusetariff.EnergyScheduler.buildEnergyScheduleHandler;
import static io.openems.edge.controller.ess.timeofusetariff.Mode.OFF;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.CHARGE_GRID;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DELAY_DISCHARGE;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.calculateApplyMode;
import static io.openems.edge.energy.api.Version.V2_ENERGY_SCHEDULABLE;
import static io.openems.edge.energy.api.handler.RescheduleMode.OPTIMIZE_CURRENT_PERIOD;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jscalendar.JSCalendar.Tasks.OneTask;
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.common.meta.GridBuySoftLimit;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.ess.emergencycapacityreserve.ControllerEssEmergencyCapacityReserve;
import io.openems.edge.controller.ess.gridoptimizedcharge.ControllerEssGridOptimizedCharge;
import io.openems.edge.controller.ess.limiter14a.ControllerEssLimiter14a;
import io.openems.edge.controller.ess.limittotaldischarge.ControllerEssLimitTotalDischarge;
import io.openems.edge.controller.ess.timeofusetariff.EnergyScheduler.OptimizationContext;
import io.openems.edge.controller.ess.timeofusetariff.Utils.ApplyMode;
import io.openems.edge.controller.ess.timeofusetariff.jsonrpc.GetScheduleRequest;
import io.openems.edge.controller.ess.timeofusetariff.jsonrpc.GetScheduleResponse;
import io.openems.edge.controller.ess.timeofusetariff.v1.EnergyScheduleHandlerV1;
import io.openems.edge.controller.ess.timeofusetariff.v1.EnergyScheduleHandlerV1.ContextV1;
import io.openems.edge.controller.ess.timeofusetariff.v1.UtilsV1;
import io.openems.edge.energy.api.EnergySchedulable;
import io.openems.edge.energy.api.handler.EshWithDifferentModes;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateActiveTime;
import io.openems.edge.timeofusetariff.api.TariffManager;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ess.Time-Of-Use-Tariff", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE)
@GenerateTargetsFromReferences({ "ess", "CtrlGridOptimizedCharge" })
@SuppressWarnings("deprecation")
public class TimeOfUseTariffControllerImpl extends AbstractOpenemsComponent implements TimeOfUseTariffController,
		EnergySchedulable, Controller, OpenemsComponent, TimedataProvider, ComponentJsonApi {

	private final Logger log = LoggerFactory.getLogger(TimeOfUseTariffControllerImpl.class);

	@Deprecated
	private final EnergyScheduleHandlerV1 energyScheduleHandlerV1;
	private final CalculateActiveTime calculateDelayedTime = new CalculateActiveTime(this,
			TimeOfUseTariffController.ChannelId.DELAYED_TIME);
	private final CalculateActiveTime calculateChargedTime = new CalculateActiveTime(this,
			TimeOfUseTariffController.ChannelId.CHARGED_TIME);

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private ComponentManager componentManager;

	@Reference
	private Meta meta;

	@Reference
	private Sum sum;

	@Reference
	private TariffManager tariffManager;

	@Reference(policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata;

	@Deprecated
	@Reference(policyOption = GREEDY, cardinality = MULTIPLE, target = "(&(enabled=true)(isReserveSocEnabled=true))")
	private volatile List<ControllerEssEmergencyCapacityReserve> ctrlEmergencyCapacityReserves = new CopyOnWriteArrayList<>();

	@Deprecated
	@Reference(policyOption = GREEDY, cardinality = MULTIPLE, target = "(enabled=true)")
	private volatile List<ControllerEssLimitTotalDischarge> ctrlLimitTotalDischarges = new CopyOnWriteArrayList<>();

	@Deprecated
	@Reference(policyOption = GREEDY, cardinality = MULTIPLE, target = "(enabled=true)")
	private volatile List<ControllerEssLimiter14a> ctrlLimiter14as = new CopyOnWriteArrayList<>();

	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY, target = "(&(enabled=true)(id=${config.ess_id}))")
	private ManagedSymmetricEss ess;

	@Deprecated
	@Reference(policy = DYNAMIC, cardinality = OPTIONAL)
	private volatile io.openems.edge.energy.api.EnergyScheduler energyScheduler;

	private final List<ControllerEssGridOptimizedCharge> ctrlGridOptimizedCharges = new CopyOnWriteArrayList<>();

	@Reference(policy = DYNAMIC, //
			policyOption = GREEDY, //
			cardinality = MULTIPLE, //
			target = "(&(ess.id=${config.ess_id})(enabled=true))")
	@SuppressWarnings("unused")
	private void bindCtrlGridOptimizedCharge(ControllerEssGridOptimizedCharge ctrl) {
		this.ctrlGridOptimizedCharges.add(ctrl);
		this.energyScheduleHandler.triggerReschedule(//
				"TimeOfUseTariffControllerImpl::bindCtrlGridOptimizedCharge", //
				OPTIMIZE_CURRENT_PERIOD);
	}

	@SuppressWarnings("unused")
	private void updatedCtrlGridOptimizedCharge(ControllerEssGridOptimizedCharge ctrl) {
		this.energyScheduleHandler.triggerReschedule(//
				"TimeOfUseTariffControllerImpl::updatedCtrlGridOptimizedCharge", //
				OPTIMIZE_CURRENT_PERIOD);
	}

	@SuppressWarnings("unused")
	private void unbindCtrlGridOptimizedCharge(ControllerEssGridOptimizedCharge ctrl) {
		this.ctrlGridOptimizedCharges.remove(ctrl);
		ctrl.enableRun();
		this.energyScheduleHandler.triggerReschedule(//
				"TimeOfUseTariffControllerImpl::unbindCtrlGridOptimizedCharge", //
				OPTIMIZE_CURRENT_PERIOD);
	}

	private EshWithDifferentModes<StateMachine, OptimizationContext, Void> energyScheduleHandler;
	private Config config = null;

	public TimeOfUseTariffControllerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				TimeOfUseTariffController.ChannelId.values() //
		);

		this.energyScheduleHandlerV1 = new EnergyScheduleHandlerV1(//
				() -> this.config.controlMode().modesArray, //
				() -> new ContextV1(this.ctrlEmergencyCapacityReserves, this.ctrlLimitTotalDischarges,
						this.ctrlLimiter14as, this.ess, this.config.controlMode(),
						this.config.maxChargePowerFromGrid()));
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.energyScheduleHandler = buildEnergyScheduleHandler(this, //
				() -> this.config.enabled() && this.config.mode() == Mode.AUTOMATIC //
						? this.buildEnergySchedulerConfig() //
						: null);
		this.config = config;

		if (!config.enabled()) {
			this.enableRunOnGridOptimizedChargeControllers();
		}
	}

	@Modified
	private void modified(ComponentContext context, Config config) {
		super.modified(context, config.id(), config.alias(), config.enabled());
		this.config = config;

		if (!config.enabled()) {
			this.enableRunOnGridOptimizedChargeControllers();
		}

		this.energyScheduleHandler.triggerReschedule("TimeOfUseTariffControllerImpl::modified()",
				OPTIMIZE_CURRENT_PERIOD);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.enableRunOnGridOptimizedChargeControllers();
	}

	@Override
	public void run() throws OpenemsNamedException {
		if (this.energyScheduler == null) {
			this.logWarn(this.log, "EnergyScheduler reference is not available");
			return;
		}

		var version = this.energyScheduler.getImplementationVersion();
		if (version == null) {
			return;
		}

		if (version != V2_ENERGY_SCHEDULABLE || this.config.mode() == OFF) {
			this.enableRunOnGridOptimizedChargeControllers();
		} else {
			this.disableRunOnGridOptimizedChargeControllers();
		}

		// NOTE gridBuySoftLimit is nullable to handle deprecation of Config
		// maxChargePowerFromGrid
		final var gridBuySoftLimit = Optional.ofNullable(this.meta.getGridBuySoftLimit().getActiveOneTask()) //
				.map(OneTask::payload) //
				.map(GridBuySoftLimit::power) //
				.orElse(this.config.maxChargePowerFromGrid());
		final int gridSellHardLimit = this.meta.getGridSellHardLimitWithBuffer();

		final var clock = this.componentManager.getClock();
		final var currentPeriod = this.energyScheduleHandler.getCurrentPeriod();

		// Version and Mode given from the configuration.
		final var am = switch (this.energyScheduler.getImplementationVersion()) {

		case V1_ESS_ONLY //
			-> switch (this.config.mode()) {
			case AUTOMATIC //
				-> UtilsV1.calculateAutomaticMode(this.energyScheduleHandlerV1, //
						this.sum, this.ess, this.ctrlLimiter14as, //
						this.config.maxChargePowerFromGrid(), //
						null /* forceState */);
			case MANUAL //
				-> UtilsV1.calculateAutomaticMode(this.energyScheduleHandlerV1, //
						this.sum, this.ess, this.ctrlLimiter14as, //
						this.config.maxChargePowerFromGrid(), //
						validateManualModeV1(this.config.manualMode()) /* forceState */);
			case OFF //
				-> new ApplyMode(StateMachine.BALANCING, null);
			};

		case V2_ENERGY_SCHEDULABLE //
			-> switch (this.config.mode()) {
			case AUTOMATIC //
				->
				calculateApplyMode(this.sum, this.ess, gridBuySoftLimit, gridSellHardLimit, currentPeriod, null, clock);
			case MANUAL -> //
				calculateApplyMode(this.sum, this.ess, gridBuySoftLimit, gridSellHardLimit, currentPeriod,
						this.config.manualMode(), clock);
			case OFF //
				-> new ApplyMode(StateMachine.BALANCING, null);
			};
		};

		// Update Channels
		this._setStateMachine(am.actualMode());
		this.calculateChargedTime.update(am.actualMode() == CHARGE_GRID);
		this.calculateDelayedTime.update(am.actualMode() == DELAY_DISCHARGE);

		final var gridBuyPrice = this.tariffManager.getGridBuyDayAheadPrices()//
				.getAt(this.componentManager.getClock().instant());
		this._setQuarterlyPrices(gridBuyPrice);

		// Apply ActivePower set-point
		this.ess.setActivePowerEqualsWithFilter(am.setPoint());
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.handleRequest(GetScheduleRequest.METHOD, call -> {
			if (this.energyScheduler == null) {
				this.logWarn(this.log, "EnergyScheduler reference is not available");
				throw new IllegalStateException("No EnergyScheduler reference available");
			}

			var version = this.energyScheduler.getImplementationVersion();
			if (version == null) {
				throw new IllegalStateException("No EnergyScheduler version available");
			}

			return switch (version) {
			case V1_ESS_ONLY //
				-> this.energyScheduler.handleGetScheduleRequestV1(call, this.id());
			case V2_ENERGY_SCHEDULABLE //
				-> GetScheduleResponse.from(call.getRequest().getId(), //
						this.id(), this.componentManager.getClock(), this.ess, this.timedata,
						this.energyScheduleHandler);
			};
		});
	}

	@Override
	public String debugLog() {
		if (this.energyScheduler == null) {
			return null;
		}

		var b = new StringBuilder() //
				.append(this.getStateMachine()); //

		var version = this.energyScheduler.getImplementationVersion();
		if (version != null) {
			switch (version) {
			case V1_ESS_ONLY -> {
				if (this.energyScheduleHandlerV1 == null || this.energyScheduleHandlerV1.getCurrentState() == null) {
					b.append("|No Schedule available");
				}
			}
			case V2_ENERGY_SCHEDULABLE -> {
				if (this.energyScheduleHandler.getCurrentPeriod() == null) {
					b.append("|No Schedule available");
				}
			}
			}
		}
		return b.toString();
	}

	@Override
	public EshWithDifferentModes<StateMachine, OptimizationContext, Void> getEnergyScheduleHandler() {
		return this.energyScheduleHandler;
	}

	@Override
	public EnergyScheduleHandlerV1 getEnergyScheduleHandlerV1() {
		return this.energyScheduleHandlerV1;
	}

	private EnergyScheduler.Config buildEnergySchedulerConfig() {
		final var ctrlGridOptimizedCharge = this.ctrlGridOptimizedCharges.stream()//
				.findFirst()//
				.orElse(null);

		Double targetSocBuffer = null;
		LocalTime manualTargetTime = null;

		if (ctrlGridOptimizedCharge != null) {
			switch (ctrlGridOptimizedCharge.getMode()) {
			case MANUAL -> manualTargetTime = ctrlGridOptimizedCharge.getManualTargetTime();
			case AUTOMATIC -> targetSocBuffer = ctrlGridOptimizedCharge.getRiskLevel().socBuffer;
			case OFF -> doNothing();
			}
		}

		return new EnergyScheduler.Config(this.getActiveModes(), targetSocBuffer, manualTargetTime);
	}

	private void enableRunOnGridOptimizedChargeControllers() {
		this.ctrlGridOptimizedCharges.forEach(ctrl -> ctrl.enableRun());
	}

	private void disableRunOnGridOptimizedChargeControllers() {
		this.ctrlGridOptimizedCharges.forEach(ctrl -> ctrl.disableRun());
	}

	private List<StateMachine> getActiveModes() {
		final var activeModes = new ArrayList<>(this.config.controlMode().modes);
		final boolean hasGridOptimizedCharge = this.ctrlGridOptimizedCharges.stream()//
				.anyMatch(ctrl -> ctrl.getMode() != io.openems.edge.controller.ess.gridoptimizedcharge.Mode.OFF);
		if (hasGridOptimizedCharge) {
			activeModes.addAll(List.of(//
					StateMachine.DELAY_CHARGE, //
					StateMachine.LIMIT_CHARGE, //
					StateMachine.DISCHARGE_CONSUMPTION));
		}
		return List.copyOf(activeModes);
	}

	private static StateMachine validateManualModeV1(StateMachine manualMode) {
		return switch (manualMode) {
		case DELAY_DISCHARGE -> StateMachine.DELAY_DISCHARGE;
		case CHARGE_GRID -> StateMachine.CHARGE_GRID;
		default -> null;
		};
	}
}
