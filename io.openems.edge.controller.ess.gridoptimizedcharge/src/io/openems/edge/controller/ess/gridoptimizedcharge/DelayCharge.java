package io.openems.edge.controller.ess.gridoptimizedcharge;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.stream.IntStream;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;

public class DelayCharge {

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
		var targetTime = LocalTime.of(17, 0);

		// Try to parse the configured Time as LocalTime or ZonedDateTime, which is the
		// format that comes from UI.
		// TODO extract this feature into a DateTimeUtils class and reuse it wherever
		// feasible
		try {
			targetTime = LocalTime.parse(this.parent.config.manualTargetTime());
		} catch (DateTimeParseException e) {
			try {
				targetTime = ZonedDateTime.parse(this.parent.config.manualTargetTime()).toLocalTime();

			} catch (DateTimeParseException i) {

				// Set Info state channel and log
				StateChannel noValidManualTargetTime = this.parent
						.channel(GridOptimizedCharge.ChannelId.NO_VALID_MANUAL_TARGET_TIME);
				noValidManualTargetTime.setNextValue(true);
				this.parent.logDebug(noValidManualTargetTime.channelDoc().getText());
			}
		}

		var targetMinute = targetTime.get(ChronoField.MINUTE_OF_DAY);
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
		var state = DelayChargeState.ACTIVE_LIMIT;

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
			// Slowly increasing the maximum charge power with a ramp
			float maximum = this.parent.ess.getMaxApparentPower().getOrError();
			var currentLimitOpt = this.parent.getDelayChargeLimit().asOptional();

			if (currentLimitOpt.isPresent() && currentLimitOpt.get() < maximum) {
				return this.parent.rampFilter.getFilteredValueAsInteger(currentLimitOpt.get(), maximum, maximum,
						0.0025f);
			}

			// Already reached the maximum
			if (!currentLimitOpt.isPresent() || maximum <= currentLimitOpt.get()) {
				this.setDelayChargeStateAndLimit(DelayChargeState.NO_REMAINING_TIME, null);
				return null;

			}
		}

		// Calculate the power limit depending on the remaining time and capacity
		var calculatedPower = this.getCalculatedPowerLimit(targetMinute);
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
		var hourlyPredictionProduction = this.parent.predictorManager
				.get24HoursPrediction(new ChannelAddress("_sum", "ProductionActivePower"));
		var hourlyPredictionConsumption = this.parent.predictorManager
				.get24HoursPrediction(new ChannelAddress("_sum", "ConsumptionActivePower"));

		var now = ZonedDateTime.now(this.parent.componentManager.getClock());
		var predictionStartQuarterHour = roundZonedDateTimeDownTo15Minutes(now);

		// Predictions as Integer array
		var hourlyProduction = hourlyPredictionProduction.getValues();
		var hourlyConsumption = hourlyPredictionConsumption.getValues();

		// Displays the production values once, if debug mode is activated.
		if (this.predictionDebugLog) {
			this.parent.logDebug("Production: " + Arrays.toString(hourlyProduction));
			this.parent.logDebug("Consumption: " + Arrays.toString(hourlyConsumption));
			this.predictionDebugLog = false;
		}

		// Calculate target minute
		var targetMinute = this.calculateTargetMinute(hourlyProduction, hourlyConsumption, predictionStartQuarterHour);

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

		// State of charge
		int soc = this.parent.ess.getSoc().getOrError();

		// Battery capacity in wh
		int capacity = this.parent.ess.getCapacity().getOrError();

		// No remaining capacity
		var minPower = this.parent.ess.getPower().getMinPower(this.parent.ess, Phase.ALL, Pwr.ACTIVE);
		if (minPower >= 0 && soc > 95) {
			this.setDelayChargeStateAndLimit(DelayChargeState.NO_REMAINING_CAPACITY, null);
			return null;
		}

		// Calculate the remaining capacity with soc minus one, to avoid very high
		// results at the end.
		soc -= 1;

		// Remaining capacity of the battery in Ws till target point.
		var remainingCapacity = Math.round(capacity * (100 - soc) * 36);

		// Remaining time in seconds till the target point.
		var remainingTime = this.calculateRemainingTime(targetMinute);

		// No remaining time -> no restrictions
		if (remainingTime <= 0) {
			this.setDelayChargeStateAndLimit(DelayChargeState.NO_REMAINING_TIME, null);
			return null;
		}
		// Calculate charge power limit
		var calculatedPower = remainingCapacity / remainingTime;

		// Max apparent power
		int maxApparentPower = this.parent.ess.getMaxApparentPower().orElse(Integer.MAX_VALUE);

		// Reduce limit to MaxApparentPower to avoid very high values in the last
		// seconds
		calculatedPower = Math.min(calculatedPower, maxApparentPower);

		this.parent._setRawDelayChargeLimit(calculatedPower);

		/**
		 * Calculate the average with the last 900 limits
		 */
		IntegerReadChannel delayChargeLimitRawChannel = this.parent.getRawDelayChargeLimitChannel();

		var pastLimits = delayChargeLimitRawChannel.getPastValues()
				.tailMap(LocalDateTime.now(this.parent.componentManager.getClock()).minusSeconds(900), true) //
				.values().stream().filter(Value::isDefined).mapToInt(Value::get);

		var currentLimit = IntStream.of(calculatedPower);

		// Concat the limit values of the last 900 seconds with the current limit
		var limits = IntStream.concat(pastLimits, currentLimit); //

		// Get the average of the past values including the current
		var limitValueOpt = limits.average();

		if (!limitValueOpt.isPresent()) {
			return null;
		}

		return (int) Math.round(limitValueOpt.getAsDouble());
	}

	/**
	 * Calculates the number of seconds left to the target hour.
	 *
	 * @param targetMinute target as minute of the day
	 * @return the remaining time
	 */
	private int calculateRemainingTime(int targetMinute) {
		var targetSecondOfDay = targetMinute * 60;
		return targetSecondOfDay
				- ZonedDateTime.now(this.parent.componentManager.getClock()).get(ChronoField.SECOND_OF_DAY);
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

		var predictionStartQuarterHourIndex = predictionStartQuarterHour.get(ChronoField.MINUTE_OF_DAY) / 15;

		// Last hour when production was greater than consumption.
		var lastQuarterHour = -1;
		Integer targetMinuteActual = null;
		Integer targetMinuteAdjusted = null;

		// Iterate predictions till midnight
		for (var i = 0; i < 96 - predictionStartQuarterHourIndex; i++) {
			// to avoid null and negative consumption values.
			if (quarterHourlyProduction[i] != null && quarterHourlyConsumption[i] != null
					&& quarterHourlyConsumption[i] >= 0) {

				// Updating last quarter hour if production is higher than consumption plus
				// power buffer
				if (quarterHourlyProduction[i] > quarterHourlyConsumption[i]
						+ GridOptimizedChargeImpl.DEFAULT_POWER_BUFFER) {
					lastQuarterHour = i;
				}
			}
		}

		// Production was never higher than consumption
		if (lastQuarterHour != -1) {

			targetMinuteActual = predictionStartQuarterHour.plusMinutes(lastQuarterHour * 15)
					.get(ChronoField.MINUTE_OF_DAY);

			// target hour adjusted based on buffer hour.
			targetMinuteAdjusted = targetMinuteActual - this.parent.config.delayChargeRiskLevel().bufferMinutes;
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
		var minuteOfDay = d.get(ChronoField.MINUTE_OF_DAY);
		return d.with(ChronoField.NANO_OF_DAY, 0).plus(minuteOfDay / 15 * 15, ChronoUnit.MINUTES);
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
