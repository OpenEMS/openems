package io.openems.edge.controller.ess.timeofusetariff;

import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.CHARGE_GRID;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DELAY_DISCHARGE;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.ESS_MAX_SOC;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.calculateAutomaticMode;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.calculateChargeEnergyInChargeGrid;
import static io.openems.edge.energy.api.simulation.Coefficient.ESS;
import static io.openems.edge.energy.api.simulation.Coefficient.GRID_TO_CONS;
import static io.openems.edge.energy.api.simulation.Coefficient.GRID_TO_ESS;
import static io.openems.edge.energy.api.simulation.Coefficient.PROD_TO_ESS;
import static io.openems.edge.energy.api.simulation.Coefficient.PROD_TO_GRID;
import static java.lang.Math.min;
import static java.lang.Math.round;
import static org.apache.commons.math3.optim.linear.Relationship.EQ;
import static org.apache.commons.math3.optim.nonlinear.scalar.GoalType.MAXIMIZE;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

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
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.ess.emergencycapacityreserve.ControllerEssEmergencyCapacityReserve;
import io.openems.edge.controller.ess.limittotaldischarge.ControllerEssLimitTotalDischarge;
import io.openems.edge.controller.ess.timeofusetariff.Utils.ApplyState;
import io.openems.edge.controller.ess.timeofusetariff.jsonrpc.GetScheduleRequest;
import io.openems.edge.controller.ess.timeofusetariff.jsonrpc.GetScheduleResponse;
import io.openems.edge.controller.ess.timeofusetariff.v1.ContextV1;
import io.openems.edge.controller.ess.timeofusetariff.v1.EnergyScheduleHandlerV1;
import io.openems.edge.controller.ess.timeofusetariff.v1.UtilsV1;
import io.openems.edge.energy.api.EnergySchedulable;
import io.openems.edge.energy.api.EnergyScheduleHandler;
import io.openems.edge.energy.api.EnergyScheduler;
import io.openems.edge.energy.api.simulation.EnergyFlow;
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
		EnergySchedulable, Controller, OpenemsComponent, TimedataProvider, ComponentJsonApi {

	@Deprecated
	private final EnergyScheduleHandlerV1 energyScheduleHandlerV1;
	private final EnergyScheduleHandler.WithDifferentStates<StateMachine, EshContext> energyScheduleHandler;
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

	@Deprecated
	@Reference(policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			target = "(&(enabled=true)(isReserveSocEnabled=true))")
	private volatile List<ControllerEssEmergencyCapacityReserve> ctrlEmergencyCapacityReserves = new CopyOnWriteArrayList<>();

	@Deprecated
	@Reference(policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			target = "(enabled=true)")
	private volatile List<ControllerEssLimitTotalDischarge> ctrlLimitTotalDischarges = new CopyOnWriteArrayList<>();

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ManagedSymmetricEss ess;

	// TODO remove after v1
	@Reference
	private EnergyScheduler energyScheduler;

	private Config config = null;

	public TimeOfUseTariffControllerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				TimeOfUseTariffController.ChannelId.values() //
		);

		this.energyScheduleHandlerV1 = new EnergyScheduleHandlerV1(//
				() -> this.config.controlMode().states, //
				() -> new ContextV1(this.ctrlEmergencyCapacityReserves, this.ctrlLimitTotalDischarges, this.ess,
						this.config.controlMode(), this.config.maxChargePowerFromGrid(),
						this.config.limitChargePowerFor14aEnWG()));

		this.energyScheduleHandler = buildEnergyScheduleHandler(//
				() -> this.ess, //
				() -> this.config.controlMode(), //
				() -> this.config.maxChargePowerFromGrid(), //
				() -> this.config.limitChargePowerFor14aEnWG());
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

		this.energyScheduleHandler.triggerReschedule();

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
		// Version and Mode given from the configuration.
		final var as = switch (this.config.version()) {

		case V1_ESS_ONLY //
			-> switch (this.config.mode()) {
			case AUTOMATIC //
				-> UtilsV1.calculateAutomaticMode(this.energyScheduleHandlerV1, this.sum, this.ess,
						this.config.maxChargePowerFromGrid(), this.config.limitChargePowerFor14aEnWG());
			case OFF //
				-> new ApplyState(StateMachine.BALANCING, null);
			};

		case V2_ENERGY_SCHEDULABLE //
			-> switch (this.config.mode()) {
			case AUTOMATIC //
				-> calculateAutomaticMode(this.sum, this.ess, this.config.maxChargePowerFromGrid(),
						this.config.limitChargePowerFor14aEnWG(), this.energyScheduleHandler.getCurrentPeriod());
			case OFF //
				-> new ApplyState(StateMachine.BALANCING, null);
			};
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

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.handleRequest(GetScheduleRequest.METHOD, call -> {
			return switch (this.config.version()) {
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
		var b = new StringBuilder() //
				.append(this.getStateMachine()); //

		switch (this.config.version()) {
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
		return b.toString();
	}

	/**
	 * Builds the {@link EnergyScheduleHandler}.
	 * 
	 * <p>
	 * This is public so that it can be used by the EnergyScheduler integration
	 * test.
	 * 
	 * @param ess                        a supplier for the
	 *                                   {@link ManagedSymmetricEss}
	 * @param controlMode                a supplier for the configured
	 *                                   {@link ControlMode}
	 * @param maxChargePowerFromGrid     a supplier for the configured
	 *                                   maxChargePowerFromGrid
	 * @param limitChargePowerFor14aEnWG a supplier for the configured
	 *                                   limitChargePowerFor14aEnWG
	 * @return a typed {@link EnergyScheduleHandler}
	 */
	public static EnergyScheduleHandler.WithDifferentStates<StateMachine, EshContext> buildEnergyScheduleHandler(
			Supplier<ManagedSymmetricEss> ess, Supplier<ControlMode> controlMode, IntSupplier maxChargePowerFromGrid,
			BooleanSupplier limitChargePowerFor14aEnWG) {
		return EnergyScheduleHandler.of(//
				StateMachine.BALANCING, //
				() -> controlMode.get().states, //
				simContext -> {
					// Maximium-SoC in CHARGE_GRID is 90 %
					var maxSocEnergyInChargeGrid = round(simContext.ess().totalEnergy() * (ESS_MAX_SOC / 100));
					var essChargeInChargeGrid = calculateChargeEnergyInChargeGrid(simContext);
					return new EshContext(ess.get(), controlMode.get(), maxChargePowerFromGrid.getAsInt(),
							limitChargePowerFor14aEnWG.getAsBoolean(), maxSocEnergyInChargeGrid, essChargeInChargeGrid);
				}, //
				(simContext, period, energyFlow, ctrlContext, state) -> {
					switch (state) {
					case BALANCING -> applyBalancing(energyFlow); // TODO Move to CtrlBalancing
					case DELAY_DISCHARGE -> applyDelayDischarge(energyFlow);
					case CHARGE_GRID -> {
						energyFlow.setEssMaxCharge(ctrlContext.maxSocEnergyInChargeGrid - simContext.getEssInitial());
						applyChargeGrid(energyFlow, ctrlContext.essChargeInChargeGrid);
					}
					}
				});
	}

	/**
	 * Simulate {@link EnergyFlow} in {@link StateMachine#BALANCING}.
	 * 
	 * @param model the {@link EnergyFlow.Model}
	 */
	public static void applyBalancing(EnergyFlow.Model model) {
		var target = model.consumption - model.production;
		model.setFittingCoefficientValue(ESS, EQ, target);
	}

	/**
	 * Simulate {@link EnergyFlow} in DELAY_DISCHARGE.
	 * 
	 * @param model the {@link EnergyFlow.Model}
	 */
	public static void applyDelayDischarge(EnergyFlow.Model model) {
		var target = min(0 /* Charge -> apply Balancing */, model.consumption - model.production);
		model.setFittingCoefficientValue(ESS, EQ, target);
	}

	/**
	 * Simulate {@link EnergyFlow} in {@link StateMachine#CHARGE_GRID}.
	 * 
	 * @param model        the {@link EnergyFlow.Model}
	 * @param chargeEnergy the target charge-from-grid energy
	 */
	public static void applyChargeGrid(EnergyFlow.Model model, int chargeEnergy) {
		model.setExtremeCoefficientValue(PROD_TO_ESS, MAXIMIZE);
		model.setExtremeCoefficientValue(GRID_TO_CONS, MAXIMIZE);
		model.setFittingCoefficientValue(GRID_TO_ESS, EQ, chargeEnergy);
	}

	/**
	 * Simulate {@link EnergyFlow} in a future DISCHARGE_GRID state.
	 * 
	 * @param model           the {@link EnergyFlow.Model}
	 * @param dischargeEnergy the target discharge-to-grid energy
	 */
	public static void applyDischargeGrid(EnergyFlow.Model model, int dischargeEnergy) {
		model.setExtremeCoefficientValue(PROD_TO_GRID, MAXIMIZE);
		model.setFittingCoefficientValue(GRID_TO_ESS, EQ, -dischargeEnergy);
	}

	public static record EshContext(ManagedSymmetricEss ess, ControlMode controlMode, int maxChargePowerFromGrid,
			boolean limitChargePowerFor14aEnWG, int maxSocEnergyInChargeGrid, int essChargeInChargeGrid) {
	}

	@Override
	public EnergyScheduleHandler.WithDifferentStates<StateMachine, EshContext> getEnergyScheduleHandler() {
		return this.energyScheduleHandler;
	}

	@Override
	public EnergyScheduleHandlerV1 getEnergyScheduleHandlerV1() {
		return this.energyScheduleHandlerV1;
	}
}
