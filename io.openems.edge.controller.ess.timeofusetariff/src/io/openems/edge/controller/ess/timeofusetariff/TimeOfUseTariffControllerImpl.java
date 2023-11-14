package io.openems.edge.controller.ess.timeofusetariff;

import static io.openems.edge.controller.ess.timeofusetariff.Utils.calculateChargeFromGridPower;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.calculateDelayDischargePower;
import static java.lang.Math.max;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.IntStream;

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
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.session.Role;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.user.User;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.ess.emergencycapacityreserve.ControllerEssEmergencyCapacityReserve;
import io.openems.edge.controller.ess.limittotaldischarge.ControllerEssLimitTotalDischarge;
import io.openems.edge.controller.ess.timeofusetariff.jsonrpc.GetScheduleRequest;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateActiveTime;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;
import io.openems.edge.timeofusetariff.api.utils.TimeOfUseTariffUtils;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ess.Time-Of-Use-Tariff", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class TimeOfUseTariffControllerImpl extends AbstractOpenemsComponent
		implements TimeOfUseTariffController, Controller, OpenemsComponent, TimedataProvider, JsonApi {

	private static final ChannelAddress SUM_PRODUCTION = new ChannelAddress("_sum", "ProductionActivePower");
	private static final ChannelAddress SUM_CONSUMPTION = new ChannelAddress("_sum", "UnmanagedConsumptionActivePower");
	private static final int MINUTES_PER_PERIOD = 15;
	private static final Duration TIME_PER_PERIOD = Duration.ofMinutes(MINUTES_PER_PERIOD);

	private final Logger log = LoggerFactory.getLogger(TimeOfUseTariffControllerImpl.class);

	/**
	 * Delayed Time is aggregated also after restart of OpenEMS.
	 */
	private final CalculateActiveTime calculateDelayedTime = new CalculateActiveTime(this,
			TimeOfUseTariffController.ChannelId.DELAYED_TIME);

	/**
	 * Charged Time is aggregated also after restart of OpenEMS.
	 */
	private final CalculateActiveTime calculateChargedTime = new CalculateActiveTime(this,
			TimeOfUseTariffController.ChannelId.CHARGED_TIME);

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

	private Config config = null;
	private Schedule schedule;
	private ZonedDateTime nextQuarter = null;

	@Reference(policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata;

	@Reference(policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			target = "(&(enabled=true)(isReserveSocEnabled=true))")
	private volatile List<ControllerEssEmergencyCapacityReserve> ctrlEmergencyCapacityReserves = new CopyOnWriteArrayList<>();

	@Reference(policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			target = "(enabled=true)")
	private volatile List<ControllerEssLimitTotalDischarge> ctrlLimitTotalDischarges = new CopyOnWriteArrayList<>();

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ManagedSymmetricEss ess;

	public TimeOfUseTariffControllerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				TimeOfUseTariffController.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
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

		// Current Date Time rounded off to NUMBER_OF_MINUTES.
		var now = TimeOfUseTariffUtils.getNowRoundedDownToMinutes(this.componentManager.getClock(), MINUTES_PER_PERIOD);

		// Mode given from the configuration.
		switch (this.config.mode()) {
		case AUTOMATIC -> this.modeAutomatic(now);
		case OFF -> this.modeOff();
		}

		this.updateVisualizationChannels();
	}

	/**
	 * Apply the actual logic of calculating the battery energy, schedules it in
	 * 15-minute intervals, and determines charge or delay discharge actions.
	 *
	 * @param now Current Date Time rounded off to NUMBER_OF_MINUTES.
	 * @throws OpenemsNamedException on error
	 */
	private void modeAutomatic(ZonedDateTime now) throws OpenemsNamedException {

		// Runs the logic every interval or when the schedule is not created due to
		// unavailability of values.
		if (!(this.nextQuarter == null || now.isEqual(this.nextQuarter) || this.schedule.periods.isEmpty())) {
			// Sets the charge or discharge of the system based on the mode.
			this.setChargeOrDischarge();
			return;
		}

		// Prediction values
		final var predictionProduction = this.predictorManager.get24HoursPrediction(SUM_PRODUCTION) //
				.getValues();
		final var predictionConsumption = this.predictorManager.get24HoursPrediction(SUM_CONSUMPTION) //
				.getValues();

		// Prices contains the price values and the time it is retrieved.
		final var prices = this.timeOfUseTariff.getPrices();

		this.channel(TimeOfUseTariffController.ChannelId.QUARTERLY_PRICES_ARE_EMPTY).setNextValue(prices.isEmpty());
		if (prices.isEmpty()) {
			return;
		}

		// Ess information.
		final var netEssCapacity = this.ess.getCapacity().getOrError();
		final var soc = this.ess.getSoc().getOrError();

		// Calculate available energy using "netCapacity" and "soc".
		var currentAvailableEnergy = netEssCapacity /* [Wh] */ / 100 * soc;
		this.channel(TimeOfUseTariffController.ChannelId.AVAILABLE_CAPACITY).setNextValue(currentAvailableEnergy);

		final var reduceAbove = 10; // TODO make this configurable via Risk Level
		final var reduceBelow = 10; // TODO make this configurable via Risk Level

		// Calculate the net usable energy of the battery.
		final var reduceAboveEnergy = netEssCapacity / 100 * reduceAbove;
		final var limitEnergy = this.getLimitEnergy(netEssCapacity, reduceBelow) + reduceAboveEnergy;
		final var netUsableEnergy = max(0, netEssCapacity - limitEnergy);

		// Calculate current usable energy [Wh] in the battery.
		currentAvailableEnergy = max(0, currentAvailableEnergy - limitEnergy);
		this.channel(TimeOfUseTariffController.ChannelId.USABLE_CAPACITY).setNextValue(currentAvailableEnergy);

		// Power Values for scheduling battery for individual periods.
		var power = this.ess.getPower();
		var dischargePower = max(1000 /* at least 1000 */, power.getMaxPower(this.ess, Phase.ALL, Pwr.ACTIVE));
		var chargePower = Math.min(-1000 /* at least 1000 */, power.getMinPower(this.ess, Phase.ALL, Pwr.ACTIVE));
		var maxChargePowerFromGrid = this.config.maxChargePowerFromGrid();

		// Generate Schedule.
		this.schedule = Schedule.createSchedule(this.config.controlMode(), this.config.riskLevel(), netUsableEnergy,
				currentAvailableEnergy, dischargePower, chargePower, prices.getValues(), predictionConsumption,
				predictionProduction, this.config.maxChargePowerFromGrid());

		// log the schedule
		var scheduleString = new StringBuilder();
		scheduleString.append("\n %s %d %s %d %s %d %s %d %s %d\n".formatted("netUsableEnergy:", netUsableEnergy,
				" currentAvailableEnergy:", currentAvailableEnergy, " dischargePower:", dischargePower, " chargePower:",
				chargePower, " maxChargePowerFromGrid:", maxChargePowerFromGrid));
		scheduleString.append("%s".formatted(this.schedule.toString()));
		this.logInfo(this.log, scheduleString.toString());

		// Update next quarter.
		this.nextQuarter = now.plus(TIME_PER_PERIOD);

		// Sets the charge or discharge of the system based on the mode.
		this.setChargeOrDischarge();
	}

	/**
	 * Force charges or delays the discharge if the schedule is set for current
	 * period.
	 * 
	 * @throws OpenemsNamedException on error.
	 */
	private void setChargeOrDischarge() throws OpenemsNamedException {
		if (this.schedule.periods.isEmpty()) {
			this.setDefaultValues();
			return;
		}

		final var period = this.schedule.periods.get(0);
		final var stateMachine = period.getStateMachine(this.config.controlMode());
		this._setStateMachine(stateMachine);

		// Update the timer.
		this.calculateChargedTime.update(stateMachine == StateMachine.CHARGE_FROM_GRID);
		this.calculateDelayedTime.update(stateMachine == StateMachine.DELAY_DISCHARGE);

		// Get and apply ActivePower Less-or-Equals Set-Point
		var activePower = switch (stateMachine) {
		case CHARGE_FROM_GRID -> calculateChargeFromGridPower(period.chargeDischargeEnergy, this.ess, this.sum,
				this.config.maxChargePowerFromGrid());
		case DELAY_DISCHARGE -> calculateDelayDischargePower(period.chargeDischargeEnergy, this.ess);
		case ALLOWS_DISCHARGE, CHARGE_FROM_PV -> null;
		};

		if (activePower != null) {
			this.ess.setActivePowerLessOrEquals(activePower);
		}

	}

	/**
	 * Returns the amount of energy that is not usable for scheduling.
	 * 
	 * @param netEssCapacity net capacity of the battery.
	 * @param reduceBelow    The lower SoC limit.
	 * @return the amount of energy that is limited.
	 */
	private int getLimitEnergy(int netEssCapacity, int reduceBelow) {

		// Usable capacity based on minimum SoC from Limit total discharge and emergency
		// reserve controllers.
		var limitSoc = IntStream.concat(//
				this.ctrlLimitTotalDischarges.stream().mapToInt(ctrl -> ctrl.getMinSoc().orElse(0)), //
				this.ctrlEmergencyCapacityReserves.stream().mapToInt(ctrl -> ctrl.getActualReserveSoc().orElse(0))) //
				.max().orElse(0) + reduceBelow;
		this.channel(TimeOfUseTariffController.ChannelId.MIN_SOC).setNextValue(limitSoc);
	 * Apply the mode OFF logic.
	 */
	private void modeOff() {
		this.setDefaultValues();
	}

	/**
	 * This is only to visualize data for better debugging.
	 */
	private void updateVisualizationChannels() {
		final Float quarterlyPrice;
		final Integer consumptionPrediction;
		final Integer productionPrediction;
		final Integer gridEnergy;
		final Integer chargeDischargeEnergy;
		if (this.schedule == null || this.schedule.periods.isEmpty()) {
			// Values are not available.
			quarterlyPrice = this.timeOfUseTariff.getPrices().getValues()[0];
			consumptionPrediction = null;
			productionPrediction = null;
			gridEnergy = null;
			chargeDischargeEnergy = null;

		} else {
			// First period is always the current period.
			final var period = this.schedule.periods.get(0);

			quarterlyPrice = period.price;
			consumptionPrediction = period.consumptionPrediction;
			productionPrediction = period.productionPrediction;
			gridEnergy = period.gridEnergy();
			chargeDischargeEnergy = period.chargeDischargeEnergy;
		}

		// Set the channels
		this._setQuarterlyPrices(quarterlyPrice);
		this._setPredictedConsumption(consumptionPrediction);
		this._setPredictedProduction(productionPrediction);
		this._setGridEnergyChannel(gridEnergy);
		this._setChargeDischargeEnergyChannel(chargeDischargeEnergy);
	}

	/**
	 * Sets the Default values to the channels, if the Periods are not yet
	 * calculated or if the Mode is 'OFF'.
	 */
	private void setDefaultValues() {
		// Update the timer.
		this.calculateChargedTime.update(false);
		this.calculateDelayedTime.update(false);

		// Default State Machine.
		this._setStateMachine(StateMachine.ALLOWS_DISCHARGE);
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public CompletableFuture<? extends JsonrpcResponseSuccess> handleJsonrpcRequest(User user, JsonrpcRequest request)
			throws OpenemsNamedException {

		user.assertRoleIsAtLeast("handleJsonrpcRequest", Role.GUEST);

		switch (request.getMethod()) {

		case GetScheduleRequest.METHOD:
			return this.handleGetScheduleRequest(user, GetScheduleRequest.from(request));

		default:
			throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
		}
	}

	/**
	 * Handles a GetScheduleRequest.
	 *
	 * @param user    the User0
	 * @param request the GetScheduleRequest
	 * @return the Future JSON-RPC Response
	 */
	private CompletableFuture<? extends JsonrpcResponseSuccess> handleGetScheduleRequest(User user,
			GetScheduleRequest request) {

		final var now = TimeOfUseTariffUtils.getNowRoundedDownToMinutes(this.componentManager.getClock(), 15);
		final var fromDate = now.minusHours(3);
		final var channeladdressPrices = new ChannelAddress(this.id(), "QuarterlyPrices");
		final var channeladdressStateMachine = new ChannelAddress(this.id(), "StateMachine");

		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryResult = new TreeMap<>();

		// Query database for the last three hours, with 15-minute resolution.
		try {
			queryResult = this.timedata.queryHistoricData(null, fromDate, now,
					Set.of(channeladdressPrices, channeladdressStateMachine), new Resolution(15, ChronoUnit.MINUTES));
		} catch (OpenemsNamedException e) {
			this.logError(this.log, e.getMessage());
			e.printStackTrace();
		}

		var response = ScheduleUtils.handleGetScheduleRequest(this.schedule, this.config.controlMode(), request.getId(),
				queryResult, channeladdressPrices, channeladdressStateMachine);

		return CompletableFuture.completedFuture(response);
	}
}
