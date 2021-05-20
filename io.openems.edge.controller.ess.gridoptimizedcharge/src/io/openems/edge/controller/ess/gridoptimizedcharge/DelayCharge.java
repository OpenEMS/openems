package io.openems.edge.controller.ess.gridoptimizedcharge;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.predictor.api.oneday.Prediction24Hours;

public class DelayCharge {

	/**
	 * Buffer in watt, considered in the calculation of the target Minute.
	 */
	private static final int DEFAULT_POWER_BUFFER = 100;

	/**
	 * Keeps the current day to detect changes in day.
	 */
	private LocalDate currentDay = LocalDate.MIN;

	/**
	 * Reference to parent controller.
	 */
	private final GridOptimizedChargeImpl parent;

	/**
	 * The whole prediction should only be logged once.
	 */
	private boolean predictionDebugLog = true;

	public DelayCharge(GridOptimizedChargeImpl parent) {
		this.parent = parent;
		this.predictionDebugLog = parent.config != null ? this.parent.config.debugMode() : false;
	}

	/**
	 * Get the maximum active power limit depending on the prediction values.
	 * 
	 * <p>
	 * Calculates the target minute, when the state of charge should reach 100
	 * percent depending on the predicted production and consumption and limits the
	 * charge value of the ESS, to get full at this calculated target minute
	 * including a configured buffer.
	 * 
	 * @return Maximum charge power
	 * @throws OpenemsNamedException on error
	 */
	protected Integer getPredictiveDelayChargeMaxCharge() throws OpenemsNamedException {

		return this.calculateDelayChargeMaxCharge(this.getCalculatedTargetMinute());
	}

	/**
	 * Get the maximum active power limit depending the configured target time.
	 * 
	 * <p>
	 * Limits the charge value of the ESS, to get full at the given target minute.
	 * 
	 * @return Maximum charge power
	 * @throws OpenemsNamedException on error
	 */
	protected Integer getManualDelayChargeMaxCharge() throws OpenemsNamedException {
		int targetMinute = LocalTime.parse(this.parent.config.manualTargetTime()).get(ChronoField.MINUTE_OF_DAY);
		return this.calculateDelayChargeMaxCharge(targetMinute);
	}

	/**
	 * Apply the calculated power limit as constraint.
	 * 
	 * @param rawDelayChargeMaxChargePower raw maximum charge power (Not adapted to
	 *                                     DC)
	 * @param delayChargeMaxChargePower    maximum power that should be charged by
	 *                                     the ESS
	 * @throws OpenemsException on error
	 */
	protected void applyCalculatedLimit(int rawDelayChargeMaxChargePower, int delayChargeMaxChargePower) {

		// Current DelayCharge state
		DelayChargeState state = DelayChargeState.ACTIVE_LIMIT;

		try {
			// Set the power limitation constraint
			this.parent.ess.setActivePowerGreaterOrEquals(delayChargeMaxChargePower);
		} catch (OpenemsNamedException e) {
			state = DelayChargeState.NO_FEASABLE_SOLUTION;
		}

		// Set channels
		this.setDelayChargeStateAndLimit(state, rawDelayChargeMaxChargePower);
		this.parent.channel(GridOptimizedCharge.ChannelId.DELAY_CHARGE_NEGATIVE_LIMIT).setNextValue(false);
	}

	/**
	 * Calculate the maximum active power limit depending on the given target
	 * minute.
	 * 
	 * <p>
	 * Limits the charge value of the ESS, to reach 100 percent at this calculated
	 * target minute.
	 * 
	 * 
	 * @param targetMinute Minute when the production get's lower than the
	 *                     consumption
	 * @return Maximum charge power
	 * @throws OpenemsNamedException on error
	 */
	private Integer calculateDelayChargeMaxCharge(Integer targetMinute) throws OpenemsNamedException {

		// Set target minute independent of the current mode
		this.parent._setTargetMinute(targetMinute);

		// Return if there is no target minute
		if (targetMinute == null) {
			this.setDelayChargeStateAndLimit(DelayChargeState.TARGET_MINUTE_NOT_CALCULATED, null);
			return null;
		}

		// Return if we passed the target minute
		if (this.passedTargetMinute(targetMinute)) {
			this.setDelayChargeStateAndLimit(DelayChargeState.NO_REMAINING_TIME, null);
			return null;
		}

		// Calculate the power limit depending on the remaining time and capacity
		Integer calculatedPower = this.getCalculatedPowerLimit(targetMinute);
		if (calculatedPower == null) {
			return null;
		}

		// Avoid discharging the ESS
		if (calculatedPower < 0) {
			this.setDelayChargeStateAndLimit(DelayChargeState.NO_CHARGE_LIMIT, null);
			this.parent.logDebug("System would charge from the grid under these constraints");
			this.parent.channel(GridOptimizedCharge.ChannelId.DELAY_CHARGE_NEGATIVE_LIMIT).setNextValue(true);
			return null;
		}

		return calculatedPower;
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
	private Integer getCalculatedTargetMinute() {

		// Predictions
		Prediction24Hours hourlyPredictionProduction = this.parent.predictorManager
				.get24HoursPrediction(new ChannelAddress("_sum", "ProductionActivePower"));
		Prediction24Hours hourlyPredictionConsumption = this.parent.predictorManager
				.get24HoursPrediction(new ChannelAddress("_sum", "ConsumptionActivePower"));

		ZonedDateTime now = ZonedDateTime.now(this.parent.componentManager.getClock());
		ZonedDateTime predictionStartQuarterHour = roundZonedDateTimeDownTo15Minutes(now);

		this.resetTargetMinutesAtMidnight();

		// Predictions as Integer array
		Integer[] hourlyProduction = hourlyPredictionProduction.getValues();
		Integer[] hourlyConsumption = hourlyPredictionConsumption.getValues();

		// Displays the production values once, if debug mode is activated.
		if (this.predictionDebugLog) {
			this.parent.logDebug("Production: " + Arrays.toString(hourlyProduction));
			this.parent.logDebug("Consumption: " + Arrays.toString(hourlyConsumption));
			this.predictionDebugLog = false;
		}

		// Calculate target minute
		Integer targetMinute = this.calculateTargetMinute(hourlyProduction, hourlyConsumption,
				predictionStartQuarterHour);

		// Production was never higher than consumption
		if (targetMinute == null) {
			this.setDelayChargeStateAndLimit(DelayChargeState.TARGET_MINUTE_NOT_CALCULATED, null);

			this.parent.logDebug("No target minute calculated - Production may never be higher than consumption");
			return null;
		}

		return targetMinute;
	}

	/**
	 * Calculates the charging power limit for the current cycle.
	 * 
	 * @param targetMinute target as minute of the day
	 * @return the calculated charging power limit or null if no limit should be
	 *         applied
	 * @throws OpenemsNamedException on error
	 */
	public Integer getCalculatedPowerLimit(Integer targetMinute) throws OpenemsNamedException {

		if (targetMinute == null) {
			return null;
		}

		// Battery capacity in wh
		int capacity = this.parent.ess.getCapacity().getOrError();

		// State of charge
		int soc = this.parent.ess.getSoc().getOrError();

		// Remaining capacity of the battery in Ws till target point.
		int remainingCapacity = capacity * (100 - soc) * 36;

		// No remaining capacity -> no restrictions
		if (remainingCapacity <= 0) {
			this.setDelayChargeStateAndLimit(DelayChargeState.NO_REMAINING_CAPACITY, null);
			return null;
		}

		// Remaining time in seconds till the target point.
		int remainingTime = this.calculateRemainingTime(targetMinute);

		// No remaining time -> no restrictions
		if (remainingTime <= 0) {
			this.setDelayChargeStateAndLimit(DelayChargeState.NO_REMAINING_TIME, null);
			return null;
		}

		// Calculate charge power limit
		Integer calculatedPower = remainingCapacity / remainingTime;

		// Max apparent power
		int maxApparentPower = this.parent.ess.getMaxApparentPower().orElse(Integer.MAX_VALUE);

		// Reduce limit to MaxApparentPower to avoid very high values in the last
		// seconds
		calculatedPower = Math.min(calculatedPower, maxApparentPower);

		return calculatedPower;
	}

	/**
	 * Calculates the number of seconds left to the target hour.
	 * 
	 * @param targetMinute target as minute of the day
	 * @return the remaining time
	 */
	private int calculateRemainingTime(int targetMinute) {
		int targetSecondOfDay = targetMinute * 60;
		int remainingTime = targetSecondOfDay
				- ZonedDateTime.now(this.parent.componentManager.getClock()).get(ChronoField.SECOND_OF_DAY);

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
	 * @param quarterHourlyProduction    the production prediction
	 * @param quarterHourlyConsumption   the consumption prediction
	 * @param predictionStartQuarterHour the prediction start quarterHour
	 * @return the target hour
	 */
	private Integer calculateTargetMinute(Integer[] quarterHourlyProduction, Integer[] quarterHourlyConsumption,
			ZonedDateTime predictionStartQuarterHour) {

		int predictionStartQuarterHourIndex = predictionStartQuarterHour.get(ChronoField.MINUTE_OF_DAY) / 15;

		// Last hour when production was greater than consumption.
		int lastQuarterHour = -1;
		Integer targetMinuteActual = null;
		Integer targetMinuteAdjusted = null;

		// Iterate predictions till midnight
		for (int i = 0; i < (96 - predictionStartQuarterHourIndex); i++) {
			// to avoid null and negative consumption values.
			if ((quarterHourlyProduction[i] != null && quarterHourlyConsumption[i] != null
					&& quarterHourlyConsumption[i] >= 0)) {

				// Updating last quarter hour if production is higher than consumption plus
				// power buffer
				if (quarterHourlyProduction[i] > quarterHourlyConsumption[i] + DEFAULT_POWER_BUFFER) {
					lastQuarterHour = i;
				}
			}
		}

		// Production was never higher than consumption
		if (lastQuarterHour != -1) {

			targetMinuteActual = predictionStartQuarterHour.plusMinutes(lastQuarterHour * 15)
					.get(ChronoField.MINUTE_OF_DAY);

			// target hour adjusted based on buffer hour.
			targetMinuteAdjusted = targetMinuteActual - this.parent.config.noOfBufferMinutes();
		}

		/*
		 * Initial target minute already passed or there's no higher production than
		 * consumption in this prediction
		 */
		if (targetMinuteAdjusted == null) {
			// Return the initial or last defined predicted target minute
			if (this.parent.getPredictedTargetMinuteAdjusted().isDefined()) {
				return this.parent.getPredictedTargetMinuteAdjusted().get();
			}
			return null;
		}

		// Set the predicted target minutes
		this.parent._setPredictedTargetMinute(targetMinuteActual);
		this.parent._setPredictedTargetMinuteAdjusted(targetMinuteAdjusted);

		return targetMinuteAdjusted;
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

	/**
	 * Resets the predicted target minutes at midnight.
	 */
	private void resetTargetMinutesAtMidnight() {
		LocalDate today = LocalDate.now(this.parent.componentManager.getClock());
		if (!this.currentDay.equals(today)) {
			this.parent._setPredictedTargetMinute(null);
			this.parent._setPredictedTargetMinuteAdjusted(null);
			this.currentDay = today;
		}
	}

	/**
	 * Update the StateMachine and ChargePowerLimit channels.
	 * 
	 * @param state the {@link DelayChargeState}
	 * @param limit the ChargePowerLimit
	 */
	protected void setDelayChargeStateAndLimit(DelayChargeState state, Integer limit) {
		this.parent._setDelayChargeState(state);
		this.parent._setDelayChargeLimit(limit);
	}

	/**
	 * Checks if we passed already the target minute.
	 * 
	 * @param targetMinute target as minute of the day
	 * @return true if it is later than the target minute.
	 */
	private boolean passedTargetMinute(int targetMinute) {

		if (ZonedDateTime.now(this.parent.componentManager.getClock()).get(ChronoField.MINUTE_OF_DAY) >= targetMinute) {
			return true;
		}
		return false;
	}
}
