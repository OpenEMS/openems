package io.openems.edge.controller.ess.timeofusetariff;

import static io.openems.edge.controller.ess.timeofusetariff.EnergyScheduler.buildEnergyScheduleHandler;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.CHARGE_GRID;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DELAY_DISCHARGE;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.calculateAutomaticMode;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

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
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.ess.emergencycapacityreserve.ControllerEssEmergencyCapacityReserve;
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
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateActiveTime;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ess.Time-Of-Use-Tariff", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@SuppressWarnings("deprecation")
public class TimeOfUseTariffControllerImpl extends AbstractOpenemsComponent implements TimeOfUseTariffController,
		EnergySchedulable, Controller, OpenemsComponent, TimedataProvider, ComponentJsonApi {

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
	private Sum sum;

	// This is only required to get the current price for UI chart
	@Reference
	private TimeOfUseTariff timeOfUseTariff;

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

	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY)
	private ManagedSymmetricEss ess;

	@Reference
	@Deprecated
	private io.openems.edge.energy.api.EnergyScheduler energyScheduler;

	private EshWithDifferentModes<StateMachine, OptimizationContext, Void> energyScheduleHandler;
	private Config config = null;

	public TimeOfUseTariffControllerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				TimeOfUseTariffController.ChannelId.values() //
		);

		this.energyScheduleHandlerV1 = new EnergyScheduleHandlerV1(//
				() -> this.config.controlMode().modes, //
				() -> new ContextV1(this.ctrlEmergencyCapacityReserves, this.ctrlLimitTotalDischarges,
						this.ctrlLimiter14as, this.ess, this.config.controlMode(),
						this.config.maxChargePowerFromGrid()));
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.energyScheduleHandler = buildEnergyScheduleHandler(this, //
				() -> this.config.enabled() && this.config.mode() == Mode.AUTOMATIC //
						? new EnergyScheduler.Config(this.config.controlMode()) //
						: null);
		this.applyConfig(config);
	}

	@Modified
	private void modified(ComponentContext context, Config config) {
		super.modified(context, config.id(), config.alias(), config.enabled());
		this.applyConfig(config);
		this.energyScheduleHandler.triggerReschedule("TimeOfUseTariffControllerImpl::modified()");
	}

	private synchronized void applyConfig(Config config) {
		this.config = config;

		// update filter for 'ess'
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "ess", config.ess_id())) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		var version = this.energyScheduler.getImplementationVersion();
		if (version == null) {
			return;
		}

		// Version and Mode given from the configuration.
		final var am = switch (this.energyScheduler.getImplementationVersion()) {

		case V1_ESS_ONLY //
			-> switch (this.config.mode()) {
			case AUTOMATIC //
				-> UtilsV1.calculateAutomaticMode(this.energyScheduleHandlerV1, this.sum, this.ess,
						this.ctrlLimiter14as, this.config.maxChargePowerFromGrid());
			case OFF //
				-> new ApplyMode(StateMachine.BALANCING, null);
			};

		case V2_ENERGY_SCHEDULABLE //
			-> switch (this.config.mode()) {
			case AUTOMATIC //
				-> calculateAutomaticMode(this.sum, this.ess, this.config.maxChargePowerFromGrid(),
						this.energyScheduleHandler.getCurrentPeriod());
			case OFF //
				-> new ApplyMode(StateMachine.BALANCING, null);
			};
		};

		// Update Channels
		this._setStateMachine(am.actualMode());
		this.calculateChargedTime.update(am.actualMode() == CHARGE_GRID);
		this.calculateDelayedTime.update(am.actualMode() == DELAY_DISCHARGE);
		this._setQuarterlyPrices(this.timeOfUseTariff.getPrices().getFirst());

		// Apply ActivePower set-point
		if (am.setPoint() != null) {
			this.ess.setActivePowerLessOrEquals(this.ess.getPower().fitValueIntoMinMaxPower(this.id(), this.ess,
					Phase.ALL, Pwr.ACTIVE, am.setPoint()));
		}
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		var version = this.energyScheduler.getImplementationVersion();
		if (version == null) {
			return;
		}

		builder.handleRequest(GetScheduleRequest.METHOD, call -> //
		switch (version) {
		case V1_ESS_ONLY //
			-> this.energyScheduler.handleGetScheduleRequestV1(call, this.id());

		case V2_ENERGY_SCHEDULABLE //
			-> GetScheduleResponse.from(call.getRequest().getId(), //
					this.id(), this.componentManager.getClock(), this.ess, this.timedata, this.energyScheduleHandler);
		});
	}

	@Override
	public String debugLog() {
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
}
