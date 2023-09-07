package io.openems.edge.controller.ess.timeofusetariff;

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
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.common.user.User;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.ess.emergencycapacityreserve.ControllerEssEmergencyCapacityReserve;
import io.openems.edge.controller.ess.limittotaldischarge.ControllerEssLimitTotalDischarge;
import io.openems.edge.controller.ess.timeofusetariff.jsonrpc.GetScheduleRequest;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.predictor.api.manager.PredictorManager;
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
	private static final ChannelAddress SUM_CONSUMPTION = new ChannelAddress("_sum", "ConsumptionActivePower");
	private static final int PERIODS_PER_HOUR = 4;
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
		this._setTotalQuarterlyPricesChannel(prices.getValues().length);

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

		// Calculate the net usable energy of the battery.
		final var limitEnergy = this.getLimitEnergy(netEssCapacity);
		final var netUsableEnergy = TypeUtils.max(0, netEssCapacity - limitEnergy);

		// Calculate current usable energy [Wh] in the battery.
		currentAvailableEnergy = TypeUtils.max(0, currentAvailableEnergy - limitEnergy);
		this.channel(TimeOfUseTariffController.ChannelId.USABLE_CAPACITY).setNextValue(currentAvailableEnergy);

		// Power Values for scheduling battery for individual periods.
		var power = this.ess.getPower();
		var dischargePower = power.getMaxPower(this.ess, Phase.ALL, Pwr.ACTIVE);
		var chargePower = power.getMinPower(this.ess, Phase.ALL, Pwr.ACTIVE);

		// If both values are 0, its likely that components are not yet activated
		// completely.
		// TODO: Handle cases where max and min power are very low in beginning.
		if (dischargePower == 0 && chargePower == 0) {
			return;
		}

		// Power to Energy.
		var dischargeEnergy = dischargePower / PERIODS_PER_HOUR;
		var chargeEnergy = chargePower / PERIODS_PER_HOUR;
		var allowedChargeEnergyFromGrid = this.config.maxChargePowerFromGrid() / PERIODS_PER_HOUR;

		// Initialize Schedule
		this.schedule = new Schedule(this.config.controlMode(), netUsableEnergy, currentAvailableEnergy,
				dischargeEnergy, chargeEnergy, prices.getValues(), predictionConsumption, predictionProduction,
				allowedChargeEnergyFromGrid);

		// Generate Final schedule.
		this.schedule.createSchedule();

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
	private void setChargeOrDischarge() {
		if (this.schedule.periods.isEmpty()) {
			this.setDefaultValues();
			return;
		}

		final var period = this.schedule.periods.get(0);
		final var stateMachine = period.getStateMachine(this.config.controlMode());
		var charged = false;
		var delayed = false;

		var activePower = switch (stateMachine) {
		case CHARGING -> {
			charged = true;
			yield period.chargeDischargeEnergy * PERIODS_PER_HOUR; // energy to power
		}
		case DELAYED -> {
			delayed = true;
			var value = period.chargeDischargeEnergy * PERIODS_PER_HOUR; // energy to power

			if (this.ess instanceof HybridEss hybridEss) {
				// DC or Hybrid system: limit AC export power to DC production power
				value += TypeUtils.subtract(this.ess.getActivePower().get(), hybridEss.getDcDischargePower().get());
			}

			yield value;
		}
		// Do not set active power
		case ALLOWS_DISCHARGE -> null;
		case STANDBY -> null;
		};

		if (activePower != null) {
			try {
				this.ess.setActivePowerLessOrEquals(activePower);
			} catch (OpenemsNamedException e) {
				e.printStackTrace();
				delayed = false;
				charged = false;
			}
		}

		this._setDelayed(delayed);
		this._setCharged(charged);
		this._setStateMachine(stateMachine);
	}

	/**
	 * Returns the amount of energy that is not usable for scheduling.
	 * 
	 * @param netEssCapacity net capacity of the battery.
	 * @return the amount of energy that is limited.
	 */
	private int getLimitEnergy(int netEssCapacity) {

		// Usable capacity based on minimum SoC from Limit total discharge and emergency
		// reserve controllers.
		var limitSoc = IntStream.concat(//
				this.ctrlLimitTotalDischarges.stream().mapToInt(ctrl -> ctrl.getMinSoc().get()), //
				this.ctrlEmergencyCapacityReserves.stream().mapToInt(ctrl -> ctrl.getActualReserveSoc().get())) //
				.max().orElse(0);
		this.channel(TimeOfUseTariffController.ChannelId.MIN_SOC).setNextValue(limitSoc);

		return netEssCapacity /* [Wh] */ / 100 * limitSoc;
	}

	/**
	 * Apply the mode OFF logic.
	 */
	private void modeOff() {
		this.setDefaultValues();
	}

	/**
	 * Sets the Default values to the channels, if the Periods are not yet
	 * calculated or if the Mode is 'OFF'.
	 */
	private void setDefaultValues() {
		this._setCharged(false);
		this._setDelayed(false);
		this._setStateMachine(StateMachine.STANDBY);
	}

	/**
	 * This is only to visualize data for better debugging.
	 */
	private void updateVisualizationChannels() {

		if (this.schedule == null || this.schedule.periods.isEmpty()) {
			// Values are not yet calculated.
			this.setDefaultValues();
			return;
		}

		// Update the timer.
		this.calculateChargedTime.update(this.getChargedChannel().getNextValue().orElse(false));
		this.calculateDelayedTime.update(this.getDelayedChannel().getNextValue().orElse(false));

		// First period is always the current period.
		final var period = this.schedule.periods.get(0);

		// Set the channels
		this._setQuarterlyPrices(period.price);
		this._setPredictedConsumption(period.consumptionPrediction);
		this._setPredictedProduction(period.productionPrediction);
		this._setGridEnergyChannel(period.gridEnergy);
		this._setChargeDischargeEnergyChannel(period.chargeDischargeEnergy);
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
		final var MINUTES_PER_PERIOD = 15;
		final var now = TimeOfUseTariffUtils.getNowRoundedDownToMinutes(componentManager.getClock(),
				MINUTES_PER_PERIOD);
		final var fromDate = now.minusHours(3);
		final var channeladdressPrices = new ChannelAddress(this.id(), "QuarterlyPrices");
		final var channeladdressStateMachine = new ChannelAddress(this.id(), "StateMachine");

		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryResult = new TreeMap<>();

		// Query database for the last three hours, with 15-minute resolution.
		try {
			queryResult = timedata.queryHistoricData(null, fromDate, now,
					Set.of(channeladdressPrices, channeladdressStateMachine), new Resolution(15, ChronoUnit.MINUTES));
		} catch (OpenemsNamedException e) {
			this.logError(this.log, e.getMessage());
			e.printStackTrace();
		}

		var response = ScheduleUtils.handleGetScheduleRequest(this.schedule, this.config, request, queryResult,
				channeladdressPrices, channeladdressStateMachine);

		return CompletableFuture.completedFuture(response);
	}
}
