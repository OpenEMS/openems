package io.openems.edge.controller.ess.timeofusetariff;

import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.BALANCING;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.CHARGE_GRID;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DELAY_DISCHARGE;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.calculateAutomaticMode;

import java.util.List;
import java.util.concurrent.CompletableFuture;
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

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.session.Role;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.ess.emergencycapacityreserve.ControllerEssEmergencyCapacityReserve;
import io.openems.edge.controller.ess.limittotaldischarge.ControllerEssLimitTotalDischarge;
import io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffControllerImpl.Context;
import io.openems.edge.controller.ess.timeofusetariff.Utils.ApplyState;
import io.openems.edge.energy.api.EnergySchedulable;
import io.openems.edge.energy.api.EnergyScheduleHandler;
import io.openems.edge.energy.api.EnergyScheduler;
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
public class TimeOfUseTariffControllerImpl extends AbstractOpenemsComponent implements TimeOfUseTariffController,
		EnergySchedulable<StateMachine, Context>, Controller, OpenemsComponent, TimedataProvider, ComponentJsonApi {

	public static record Context(List<ControllerEssEmergencyCapacityReserve> ctrlEmergencyCapacityReserves,
			List<ControllerEssLimitTotalDischarge> ctrlLimitTotalDischarges, ManagedSymmetricEss ess,
			ControlMode controlMode, int maxChargePowerFromGrid, boolean limitChargePowerFor14aEnWG) {
	}

	private final EnergyScheduleHandler<StateMachine, Context> energyScheduleHandler;
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

	@Reference(policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata;

	@Reference(policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			target = "(&(enabled=true)(isReserveSocEnabled=true))")
	private List<ControllerEssEmergencyCapacityReserve> ctrlEmergencyCapacityReserves = new CopyOnWriteArrayList<>();

	@Reference(policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			target = "(enabled=true)")
	private List<ControllerEssLimitTotalDischarge> ctrlLimitTotalDischarges = new CopyOnWriteArrayList<>();

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ManagedSymmetricEss ess;

	@Reference
	private EnergyScheduler energyScheduler;

	private Config config = null;

	public TimeOfUseTariffControllerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				TimeOfUseTariffController.ChannelId.values() //
		);
		this.energyScheduleHandler = new EnergyScheduleHandler<>(//
				() -> this.config.controlMode().states, //
				() -> new Context(this.ctrlEmergencyCapacityReserves, this.ctrlLimitTotalDischarges, this.ess,
						this.config.controlMode(), this.config.maxChargePowerFromGrid(),
						this.config.limitChargePowerFor14aEnWG()));
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.applyConfig(config);
	}

	@Modified
	private void modified(ComponentContext context, Config config) {
		super.modified(context, config.id(), config.alias(), config.enabled());
		this.applyConfig(config);
	}

	private synchronized void applyConfig(Config config) {
		this.config = config;

		// update filter for 'ess'
		OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "ess", config.ess_id());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		// Mode given from the configuration.
		var as = switch (this.config.mode()) {
		case AUTOMATIC -> calculateAutomaticMode(this.sum, this.ess,
				this.energyScheduleHandler.getCurrentEssChargeInChargeGrid(), this.config.maxChargePowerFromGrid(),
				this.config.limitChargePowerFor14aEnWG(), this.getCurrentPeriodState());
		case OFF -> new ApplyState(StateMachine.BALANCING, null);
		};

		// Update Channels
		this._setStateMachine(as.actualState());
		this.calculateChargedTime.update(as.actualState() == CHARGE_GRID);
		this.calculateDelayedTime.update(as.actualState() == DELAY_DISCHARGE);
		this._setQuarterlyPrices(this.timeOfUseTariff.getPrices().getFirst());

		// Apply ActivePower set-point
		if (as.setPoint() != null) {
			this.ess.setActivePowerLessOrEquals(this.ess.getPower().fitValueIntoMinMaxPower(this.id(), this.ess,
					Phase.ALL, Pwr.ACTIVE, as.setPoint()));
		}
	}

	private StateMachine getCurrentPeriodState() {
		var state = this.energyScheduleHandler.getCurrentState();
		if (state != null) {
			return state;
		}
		return BALANCING; // Default Fallback
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		this.energyScheduler.buildJsonApiRoutes(builder);
	}

	@Override
	public String debugLog() {
		var b = new StringBuilder() //
				.append(this.getStateMachine()); //
		if (this.getCurrentPeriodState() == null) {
			b.append("|No Schedule available");
		}
		return b.toString();
	}

	@Override
	public EnergyScheduleHandler<StateMachine, Context> getEnergyScheduleHandler() {
		return this.energyScheduleHandler;
	}
}
