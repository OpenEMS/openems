package io.openems.edge.controller.ess.gridoptimizedcharge;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.InvalidValueException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.predictor.api.manager.PredictorManager;
import io.openems.edge.predictor.api.oneday.Prediction24Hours;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ess.GridOptimizedCharge", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class GridOptimizedChargeImpl extends AbstractOpenemsComponent
		implements GridOptimizedCharge, Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(GridOptimizedChargeImpl.class);

	private Config config = null;

	// Buffer in watt, considered in the calculation of the target Minute.
	private final static int DEFAULT_POWER_BUFFER = 100;

	// ZonedDateTime with the current time.
	private ZonedDateTime now;

	// Keeps the current day to detect changes in day.
	private LocalDate currentDay = LocalDate.MIN;

	private int lastSellToGridLimit = 0;

	private boolean debugMode;

	@Reference
	protected Sum sum;

	@Reference
	protected PredictorManager predictorManager;

	@Reference
	protected ComponentManager componentManager;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ManagedSymmetricEss ess;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private SymmetricMeter meter;

	public GridOptimizedChargeImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				GridOptimizedCharge.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.updateConfig(config);
	}

	@Modified
	void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		super.modified(context, config.id(), config.alias(), config.enabled());
		this.updateConfig(config);
	}

	private void updateConfig(Config config) {
		this.config = config;
		this.debugMode = config.debugMode();

		// update filter for 'ess'
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "ess", config.ess_id())) {
			return;
		}

		// update filter for 'meter'
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "meter", config.meter_id())) {
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

		/*
		 * Check that we are On-Grid (and warn on undefined Grid-Mode)
		 */
		GridMode gridMode = this.ess.getGridMode();
		if (gridMode.isUndefined()) {
			this.logWarn(this.log, "Grid-Mode is [UNDEFINED]");
		}
		switch (gridMode) {
		case ON_GRID:
		case UNDEFINED:
			break;
		case OFF_GRID:
			this._setSellToGridLimitState(SellToGridLimitState.UNDEFINED);
			this._setDelayChargeState(DelayChargeState.UNDEFINED);
			return;
		}

		// Set the current time global, once a cycle
		this.now = ZonedDateTime.now(this.componentManager.getClock());

		/*
		 * Run the logic of the different modes, depending on the configuration
		 */
		switch (this.config.mode()) {
		case OFF:
			return;
		case AUTOMATIC:
			applySellToGridLimit();
			applyPredictiveDelayCharge();
			break;
		case MANUAL:
			applySellToGridLimit();
			applyManualDelayCharge();
			break;
		}
	}

	/**
	 * Set active power limits depending on the maximum sell to grid power.
	 * 
	 * @throws OpenemsNamedException on error
	 */
	public void applySellToGridLimit() throws OpenemsNamedException {

		// Current buy-from/sell-to grid
		int gridPower = this.meter.getActivePower().getOrError();

		// Current ess charge/discharge power
		int essActivePower = getIntValueOrSetStateAndException(this.ess.getActivePower(),
				GridOptimizedCharge.ChannelId.ESS_HAS_NO_ACTIVE_POWER);

		// Calculate actual limit for Ess
		int essMinChargePower = gridPower + essActivePower + this.config.maximumSellToGridPower();

		// Adjust value so that it fits into Min/MaxActivePower
		essMinChargePower = ess.getPower().fitValueIntoMinMaxPower(this.id(), ess, Phase.ALL, Pwr.ACTIVE,
				essMinChargePower);

		// Adjust ramp
		essMinChargePower = applyPowerRamp(essMinChargePower);

		// Avoid max discharge constraint
		if (essMinChargePower > 0) {
			setSellToGridLimitChannelsAndLastLimit(SellToGridLimitState.NO_LIMIT, 0);
			return;
		}

		// Apply limit
		this.ess.setActivePowerLessOrEquals(essMinChargePower);
		setSellToGridLimitChannelsAndLastLimit(SellToGridLimitState.ACTIVE_LIMIT, essMinChargePower);
	}

	/**
	 * Set Channels and lastLimit for SellToGridLimit part.
	 * 
	 * @param state                 SellToGridLimitState
	 * @param sellToGridChargeLimit sellToGridLimit absolute charge limit
	 * @param essPowerLimit         ess power limit
	 */
	private void setSellToGridLimitChannelsAndLastLimit(SellToGridLimitState state, int essMinChargePower) {
		this._setSellToGridLimitState(state);
		this._setSellToGridLimitMinimumChargeLimit(essMinChargePower * -1);
		this.lastSellToGridLimit = essMinChargePower;
	}

	/**
	 * Apply power ramp, to react in a smooth way.
	 * 
	 * <p>
	 * Calculates a limit depending on the given power limit and the last power
	 * limit. Stronger limits are taken directly, while the last limit will only be
	 * reduced if the new limit is lower.
	 * 
	 * @param essPowerLimit essPowerLimit
	 * @return adjusted ess power limit
	 * @throws InvalidValueException on error
	 */
	private int applyPowerRamp(int essPowerLimit) throws InvalidValueException {

		// Stronger Limit will be taken
		if (this.lastSellToGridLimit == 0 || essPowerLimit <= this.lastSellToGridLimit) {
			return essPowerLimit;
		}

		// Maximum power
		int maxEssPower = this.ess.getMaxApparentPower().orElse(this.config.maximumSellToGridPower());

		// Reduce last SellToGridLimit by configured percentage
		double percentage = (this.config.sellToGridLimitRampPercentage() / 100.0);
		int rampValue = (int) (maxEssPower * percentage);
		essPowerLimit = lastSellToGridLimit + rampValue;
		return essPowerLimit;
	}

	/**
	 * Set active power limits depending on the prediction values.
	 * 
	 * <p>
	 * Calculates the target minute, when the state of charge should reach 100
	 * percent depending on the predicted production and consumption and limits the
	 * charge value of the ESS, to get full at this calculated target minute
	 * including a configured buffer.
	 * 
	 * @throws OpenemsNamedException on error
	 */
	private void applyPredictiveDelayCharge() throws OpenemsNamedException {

		this.applyDelayCharge(this.getCalculatedTargetMinute());
	}

	/**
	 * Set active power limits depending the configured target time.
	 * 
	 * <p>
	 * Limits the charge value of the ESS, to get full at the given target minute.
	 * 
	 * @throws OpenemsNamedException on error
	 */
	private void applyManualDelayCharge() throws OpenemsNamedException {
		LocalTime configureTargetTime = LocalTime.parse(this.config.manual_targetTime());
		int targetMinute = configureTargetTime.get(ChronoField.MINUTE_OF_DAY);
		this.applyDelayCharge(targetMinute);
	}

	/**
	 * /** Set active power limits depending on the given target minute.
	 * 
	 * <p>
	 * Limits the charge value of the ESS, to reach 100 percent at this calculated
	 * target minute.
	 * 
	 * @param targetMinute Minute when the production get's lower than the
	 *                     consumption
	 * @throws OpenemsNamedException on error
	 */
	private void applyDelayCharge(Integer targetMinute) throws OpenemsNamedException {

		// Set target minute independent of the current mode
		this._setTargetMinute(targetMinute);

		// Return if there is no target minute
		if (targetMinute == null) {
			this.setDelayChargeStateAndLimit(DelayChargeState.TARGET_MINUTE_NOT_CALCULATED, null);
			return;
		}

		// Return if we passed the target minute
		if (this.passedTargetMinute(targetMinute)) {
			this.setDelayChargeStateAndLimit(DelayChargeState.NO_REMAINING_TIME, null);
			return;
		}

		// Calculate the power limit depending on the remaining time and capacity
		Integer calculatedPower = getCalculatedPowerLimit(targetMinute);
		if (calculatedPower == null) {
			return;
		}

		// Apply the power limit
		this.applyCalculatedPowerLimit(calculatedPower);
	}

	/**
	 * Apply the calculated power limit as constraint, depending on the ESS type.
	 * 
	 * @param calculatedPower maximum power that needs should be charged by the ESS
	 * @throws OpenemsException on error
	 */
	private void applyCalculatedPowerLimit(int calculatedPower) throws OpenemsException {

		Integer currentLimit = calculatedPower;
		DelayChargeState state = DelayChargeState.NO_CHARGE_LIMIT;

		// Avoid discharging the ESS
		if (calculatedPower < 0) {
			this.setDelayChargeStateAndLimit(DelayChargeState.NO_CHARGE_LIMIT, null);
			this.logDebug("System would charge from the grid under these constraints");
			this.channel(GridOptimizedCharge.ChannelId.DELAY_CHARGE_NEGATIVE_LIMIT).setNextValue(true);
			return;
		}

		// Calculate AC-Setpoint depending on the DC production
		int productionDcPower = this.sum.getProductionDcActualPower().orElse(0);
		currentLimit = productionDcPower - calculatedPower;

		// Avoid discharging more than the sellToGridPowerLimit allows
		if (currentLimit > this.config.maximumSellToGridPower()) {
			currentLimit = this.config.maximumSellToGridPower() - DEFAULT_POWER_BUFFER;
		}

		// Set the power limitation constraint
		state = this.setActivePowerConstraint("GridOptimizedSelfConsumption - PredictiveDelayCharge", currentLimit);

		// Set channels
		this.setDelayChargeStateAndLimit(state, calculatedPower);
		this.channel(GridOptimizedCharge.ChannelId.DELAY_CHARGE_NEGATIVE_LIMIT).setNextValue(false);
	}

	/**
	 * Set active power constraint.
	 * 
	 * <p>
	 * Sets an active power constraint depending on the given power limit.
	 * 
	 * @param description  description for the constraint
	 * @param currentLimit limit that needs to be set
	 * @return is the constraint successfully set
	 */
	/**
	 * 
	 * @param description
	 * @param currentLimit
	 */
	private DelayChargeState setActivePowerConstraint(String description, int currentLimit) {
		try {
			ess.addPowerConstraintAndValidate(description, Phase.ALL, Pwr.ACTIVE, Relationship.GREATER_OR_EQUALS,
					currentLimit);
			return DelayChargeState.ACTIVE_LIMIT;
		} catch (OpenemsException e) {
			return DelayChargeState.NO_FEASABLE_SOLUTION;
		}
	}

	/**
	 * Gets the calculated target minute.
	 * 
	 * <p>
	 * Calculates the target minute when there is no longer surplus power depending
	 * on the predicted production and consumption.
	 * 
	 * @return predicted target minute (Minute of the day)
	 */
	public Integer getCalculatedTargetMinute() {

		Integer targetMinute = null;

		// Predictions
		Prediction24Hours hourlyPredictionProduction = this.predictorManager
				.get24HoursPrediction(new ChannelAddress("_sum", "ProductionActivePower"));
		Prediction24Hours hourlyPredictionConsumption = this.predictorManager
				.get24HoursPrediction(new ChannelAddress("_sum", "ConsumptionActivePower"));

		this.now = ZonedDateTime.now(this.componentManager.getClock());
		ZonedDateTime predictionStartQuarterHour = (roundZonedDateTimeDownTo15Minutes(this.now));

		this.resetTargetMinutesAtMidnight();

		// Predictions as Integer array
		Integer[] hourlyProduction = hourlyPredictionProduction.getValues();
		Integer[] hourlyConsumption = hourlyPredictionConsumption.getValues();

		// Displays the production values once.
		if (this.debugMode) {
			this.logInfo(this.log, "Production: " + Arrays.toString(hourlyProduction));
			this.logInfo(this.log, "Consumption: " + Arrays.toString(hourlyConsumption));
			this.debugMode = false;
		}

		// Calculate target minute
		targetMinute = this.calculateTargetMinute(hourlyProduction, hourlyConsumption, predictionStartQuarterHour);

		// Production was never higher than consumption
		if (targetMinute == null) {
			this.setDelayChargeStateAndLimit(DelayChargeState.TARGET_MINUTE_NOT_CALCULATED, null);

			this.logDebug("No target minute calculated - Production may never be higher than consumption");
			return null;
		}

		return targetMinute;
	}

	/**
	 * Checks if we passed already the target minute.
	 * 
	 * @return true if it is later than the target minute.
	 */
	private boolean passedTargetMinute(int targetMinute) {

		if (this.now.get(ChronoField.MINUTE_OF_DAY) >= targetMinute) {
			return true;
		}
		return false;
	}

	/**
	 * Calculates the charging power limit for the current cycle.
	 * 
	 * @return the calculated charging power limit or null if no limit should be
	 *         applied
	 * @throws OpenemsNamedException on error
	 */
	public Integer getCalculatedPowerLimit(Integer targetMinute) throws OpenemsNamedException {

		if (targetMinute == null) {
			return null;
		}

		Integer calculatedPower = null;

		// Battery capacity in wh
		int capacity = getIntValueOrSetStateAndException(ess.getCapacity(),
				GridOptimizedCharge.ChannelId.ESS_HAS_NO_CAPACITY);

		// State of charge
		int soc = getIntValueOrSetStateAndException(ess.getSoc(), GridOptimizedCharge.ChannelId.ESS_HAS_NO_SOC);

		// Remaining capacity of the battery in Ws till target point.
		int remainingCapacity = capacity * (100 - soc) * 36;

		// No remaining capacity -> no restrictions
		if (remainingCapacity <= 0) {
			this.setDelayChargeStateAndLimit(DelayChargeState.NO_REMAINING_CAPACITY, null);
			return null;
		}

		// Remaining time in seconds till the target point.
		int remainingTime = calculateRemainingTime(targetMinute);

		// No remaining time -> no restrictions
		if (remainingTime <= 0) {
			this.setDelayChargeStateAndLimit(DelayChargeState.NO_REMAINING_TIME, null);
			return null;
		}

		// Calculate charge power limit
		calculatedPower = remainingCapacity / remainingTime;

		// Max apparent power
		int maxApparentPower = this.getIntValueOrSetStateAndException(this.ess.getMaxApparentPower(),
				GridOptimizedCharge.ChannelId.ESS_HAS_NO_APPARENT_POWER);

		// Reduce limit to MaxApparentPower to avoid very high values in the last
		// seconds
		calculatedPower = Math.min(calculatedPower, maxApparentPower);

		return calculatedPower;
	}

	/**
	 * Get the int value of the given Channel value.
	 * 
	 * <p>
	 * Set StateChannel's and throws OpenemsException if the value is not defined.
	 * 
	 * @param value        Channel value
	 * @param stateChannel state channel that should be set
	 * @return int value
	 * @throws OpenemsException on null value
	 */
	private int getIntValueOrSetStateAndException(Value<Integer> value,
			io.openems.edge.common.channel.ChannelId stateChannel) throws OpenemsException {
		if (!value.isDefined()) {
			this.channel(stateChannel).setNextValue(true);
			this.logDebug(stateChannel.doc().getText());
			throw new OpenemsException(stateChannel.doc().getText());
		}
		this.channel(stateChannel).setNextValue(false);
		return value.get();
	}

	/**
	 * Calculates the number of seconds left to the target hour.
	 * 
	 * @return the remaining time
	 */
	private int calculateRemainingTime(int targetMinute) {
		int targetSecondOfDay = targetMinute * 60;
		int remainingTime = targetSecondOfDay - this.now.get(ChronoField.SECOND_OF_DAY);

		return remainingTime;
	}

	/**
	 * Calculates the target minute from quarter-hourly production and consumption
	 * predictions.
	 * 
	 * <p>
	 * Returning the last valid target minute if it is present and the new
	 * calculated target minute would be null.
	 * 
	 * @param hourlyProduction           the production prediction
	 * @param hourlyConsumption          the consumption prediction
	 * @param predictionStartQuarterHour the prediction start quarterHour
	 * @return the target hour
	 */
	private Integer calculateTargetMinute(Integer[] quaterHourlyProduction, Integer[] quaterHourlyConsumption,
			ZonedDateTime predictionStartQuarterHour) {

		int predictionStartQuarterHourIndex = predictionStartQuarterHour.get(ChronoField.MINUTE_OF_DAY) / 15;

		// Last hour when production was greater than consumption.
		int lastQuaterHour = -1;
		Integer targetMinuteActual = null;
		Integer targetMinuteAdjusted = null;

		// Iterate predictions till midnight
		for (int i = 0; i < (96 - predictionStartQuarterHourIndex); i++) {
			// to avoid null and negative consumption values.
			if ((quaterHourlyProduction[i] != null && quaterHourlyConsumption[i] != null
					&& quaterHourlyConsumption[i] >= 0)) {

				// Updating last quarter hour if production is higher than consumption plus
				// power buffer
				if (quaterHourlyProduction[i] > quaterHourlyConsumption[i] + DEFAULT_POWER_BUFFER) {
					lastQuaterHour = i;
				}
			}
		}

		// Production was never higher than consumption
		if (lastQuaterHour != -1) {

			targetMinuteActual = predictionStartQuarterHour.plusMinutes(lastQuaterHour * 15)
					.get(ChronoField.MINUTE_OF_DAY);

			// target hour adjusted based on buffer hour.
			targetMinuteAdjusted = targetMinuteActual - this.config.noOfBufferMinutes();
		}

		/*
		 * Initial target minute already passed or there's no higher production than
		 * consumption in this prediction
		 */
		if (targetMinuteAdjusted == null) {
			// TODO: Specify if the initial target minute already passed or no higher
			// production during the day and set a more expressive Channel/State
			// e.g. if(predictionStartQuarterHourIndex == 0 || this.initialPrediction)

			// Return the initial or last defined predicted target minute
			if (this.getPredictedTargetMinuteAdjusted().isDefined()) {
				return this.getPredictedTargetMinuteAdjusted().get();
			}
			return null;
		}

		// Set the predicted target minutes
		this._setPredictedTargetMinute(targetMinuteActual);
		this._setPredictedTargetMinuteAdjusted(targetMinuteAdjusted);

		return targetMinuteAdjusted;
	}

	/**
	 * Update the StateMachine and ChargePowerLimit channels.
	 * 
	 * @param state the {@link DelayChargeState}
	 * @param limit the ChargePowerLimit
	 */
	private void setDelayChargeStateAndLimit(DelayChargeState state, Integer limit) {
		this._setDelayChargeState(state);
		this._setDelayChargeLimit(limit);
	}

	/**
	 * Resets the predicted target minutes at midnight.
	 */
	private void resetTargetMinutesAtMidnight() {
		LocalDate today = LocalDate.now(this.componentManager.getClock());
		if (!this.currentDay.equals(today)) {
			this._setPredictedTargetMinute(null);
			this._setPredictedTargetMinuteAdjusted(null);
			this.currentDay = today;
		}
	}

	/**
	 * Rounds a {@link ZonedDateTime} down to 15 minutes.
	 * 
	 * @param d the {@link ZonedDateTime}
	 * @return the rounded result
	 */
	private static ZonedDateTime roundZonedDateTimeDownTo15Minutes(ZonedDateTime d) {
		int minuteOfDay = d.get(ChronoField.MINUTE_OF_DAY);
		return d.with(ChronoField.NANO_OF_DAY, 0).plus(minuteOfDay / 15 * 15, ChronoUnit.MINUTES);
	}

	private void logDebug(String message) {
		if (this.config.debugMode()) {
			this.logDebug(this.log, message);
		}
	}
}
