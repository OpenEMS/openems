package io.openems.edge.controller.ess.gridoptimizedcharge;

import static io.openems.edge.controller.ess.gridoptimizedcharge.ControllerEssGridOptimizedChargeImpl.DEFAULT_POWER_BUFFER;
import static java.lang.Math.min;
import static java.time.temporal.ChronoField.MINUTE_OF_DAY;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.DateUtils;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;

public class DelayCharge {

	protected static final LocalTime DEFAULT_TARGET_TIME = LocalTime.of(17, 0);

	/**
	 * Minimum power factor is applied to the maximum allowed charge power of the
	 * ess, to avoid very low charge power. (Default 8.1% equals minimum 4A on every
	 * System). If the sellToGridLimit logic has been triggered, half of this value
	 * is used to avoid inverter standby.
	 */
	private static final float MINIMUM_POWER_FACTOR = 0.081F;

	/**
	 * Reference to parent controller.
	 */
	private final ControllerEssGridOptimizedChargeImpl parent;

	/**
	 * The whole prediction should only be logged once.
	 */
	private boolean predictionDebugLog = true;

	public DelayCharge(ControllerEssGridOptimizedChargeImpl parent) {
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

		return this.calculateDelayChargeMaxCharge(this.getCalculatedTargetMinute(),
				this.parent.config.delayChargeRiskLevel());
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
		LocalTime targetTime = parseTime(this.parent.config.manualTargetTime());
		if (targetTime == null) {
			targetTime = DEFAULT_TARGET_TIME;
			StateChannel noValidManualTargetTime = this.parent
					.channel(ControllerEssGridOptimizedCharge.ChannelId.NO_VALID_MANUAL_TARGET_TIME);
			noValidManualTargetTime.setNextValue(true);
			this.parent.logDebug(noValidManualTargetTime.channelDoc().getText());
		}

		var targetMinute = targetTime.get(MINUTE_OF_DAY);
		return this.calculateDelayChargeMaxCharge(targetMinute, DelayChargeRiskLevel.MEDIUM);
	}

	protected static LocalTime parseTime(String time) {
		// Try to parse the configured Time as LocalTime or ZonedDateTime, which is the
		// format that comes from UI.
		final var localTime = DateUtils.parseLocalTimeOrNull(time);
		if (localTime != null) {
			return localTime;
		}
		final var zonedDateTime = DateUtils.parseZonedDateTimeOrNull(time);
		if (zonedDateTime != null) {
			return zonedDateTime.toLocalTime();
		}
		return null;
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

		// Avoid charging with low power
		if (rawDelayChargeMaxChargePower == 0) {
			state = DelayChargeState.AVOID_LOW_CHARGING;
		}

		// Set channels
		this.setDelayChargeStateAndLimit(state, rawDelayChargeMaxChargePower);
		this.parent.channel(ControllerEssGridOptimizedCharge.ChannelId.DELAY_CHARGE_NEGATIVE_LIMIT).setNextValue(false);
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
				.getPrediction(new ChannelAddress("_sum", "ProductionActivePower"));
		var hourlyPredictionConsumption = this.parent.predictorManager
				.getPrediction(new ChannelAddress("_sum", "ConsumptionActivePower"));

		var now = ZonedDateTime.now(this.parent.componentManager.getClock());
		var predictionStartQuarterHour = roundZonedDateTimeDownTo15Minutes(now);

		// Predictions as Integer array
		var hourlyProduction = hourlyPredictionProduction.asArray();
		var hourlyConsumption = hourlyPredictionConsumption.asArray();

		// Displays the production values once, if debug mode is activated.
		if (this.predictionDebugLog) {
			this.parent.logDebug("Production: " + Arrays.toString(hourlyProduction));
			this.parent.logDebug("Consumption: " + Arrays.toString(hourlyConsumption));
			this.predictionDebugLog = false;
		}

		// Calculate target minute
		var targetMinute = DelayCharge.calculateTargetMinute(hourlyProduction, hourlyConsumption,
				predictionStartQuarterHour);

		/*
		 * Initial target minute already passed or there's no higher production than
		 * consumption in this prediction
		 */
		if (targetMinute.isEmpty()) {

			// Return the initial or last defined predicted target minute (Already passed
			// the target minute)
			if (this.parent.getPredictedTargetMinuteAdjusted().isDefined()) {
				return this.parent.getPredictedTargetMinuteAdjusted().get();
			}

			// No target minute calculated - Production may never be higher than consumption
			this.setDelayChargeStateAndLimit(DelayChargeState.TARGET_MINUTE_NOT_CALCULATED, null);
			this.parent.logDebug("No target minute calculated - Production may never be higher than consumption");
			return null;

		} else {
			/*
			 * Set valid target minute
			 */

			int targetMinuteActual = targetMinute.get();

			// target hour adjusted based on buffer hour.
			int targetMinuteAdjusted = targetMinuteActual - this.parent.config.delayChargeRiskLevel().bufferMinutes;

			// Set the predicted target minutes
			this.parent._setPredictedTargetMinute(targetMinuteActual);
			this.parent._setPredictedTargetMinuteAdjusted(targetMinuteAdjusted);

			return targetMinuteAdjusted;
		}
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
	 * @param riskLevel    Current chosen risk level
	 * @return Maximum charge power
	 * @throws OpenemsNamedException on error
	 */
	private Integer calculateDelayChargeMaxCharge(Integer targetMinute, DelayChargeRiskLevel riskLevel)
			throws OpenemsNamedException {

		// Set target minute independent of the current mode
		this.parent._setTargetMinute(targetMinute);

		// Return if there is no target minute
		if (targetMinute == null) {
			this.setDelayChargeStateAndLimit(DelayChargeState.TARGET_MINUTE_NOT_CALCULATED, null);
			return null;
		}

		// Global clock
		Clock clock = this.parent.componentManager.getClock();

		// Return if we passed the target minute
		if (DelayCharge.passedTargetMinute(targetMinute, clock)) {
			// Slowly increasing the maximum charge power with a ramp
			float maximum = this.parent.ess.getMaxApparentPower().getOrError();
			Optional<Integer> currentLimitOpt = this.parent.getDelayChargeLimit().asOptional();

			if (currentLimitOpt.isPresent() && currentLimitOpt.get() < maximum) {
				// Apply ramp filter
				return this.parent.rampFilter.getFilteredValueAsInteger(currentLimitOpt.get(), maximum, maximum,
						0.0025f);
			}

			// Already reached the maximum
			if ((!currentLimitOpt.isPresent()) || maximum <= currentLimitOpt.get()) {
				this.setDelayChargeStateAndLimit(DelayChargeState.NO_REMAINING_TIME, null);
				return null;

			}
		}

		/*
		 * Gathering all required information to calculate the maximum charging power.
		 */

		// State of charge
		var soc = this.parent.ess.getSoc().getOrError();

		// Battery capacity in wh
		var capacity = this.parent.ess.getCapacity().getOrError();

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
		var remainingTime = DelayCharge.calculateRemainingTime(clock, targetMinute);

		// Predictions
		var quarterHourlyPredictionProduction = this.parent.predictorManager
				.getPrediction(new ChannelAddress("_sum", "ProductionActivePower"));
		var quarterHourlyPredictionConsumption = this.parent.predictorManager
				.getPrediction(new ChannelAddress("_sum", "ConsumptionActivePower"));

		// Predictions as Integer array
		var quarterHourlyProduction = quarterHourlyPredictionProduction.asArray();
		var quarterHourlyConsumption = quarterHourlyPredictionConsumption.asArray();

		// Max apparent power
		int maxApparentPower = this.parent.ess.getMaxApparentPower().getOrError();

		// Minimum charge power, to avoid low charge power.
		// The half of the factor is applied, if the sellToGridLimit force charge was
		// active, to avoid standby of the inverter directly after it.
		var minimumPowerFactor = MINIMUM_POWER_FACTOR;

		boolean delayChargeMinimumReached = this.parent.getDelayChargeStateChannel().getPastValues()
				.tailMap(LocalDateTime.now(this.parent.componentManager.getClock()).with(MINUTE_OF_DAY, 5), true)
				.values().stream().filter(Value::isDefined)
				.filter(channel -> channel.asEnum() == DelayChargeState.ACTIVE_LIMIT).findAny().isPresent();

		minimumPowerFactor = delayChargeMinimumReached ? minimumPowerFactor * 0.5F : minimumPowerFactor;
		var minimumPower = Math.round(capacity * minimumPowerFactor);
		this.parent._setDebugDelayChargeMinimumPower(minimumPower);

		/*
		 * Calculate the power limit depending on the specified parameters.
		 */
		Integer calculatedPower = DelayCharge.getCalculatedPowerLimit(remainingCapacity, remainingTime,
				quarterHourlyProduction, quarterHourlyConsumption, clock, riskLevel, maxApparentPower, targetMinute,
				minimumPower, this.parent);

		if (calculatedPower == null) {
			this.setDelayChargeStateAndLimit(DelayChargeState.NO_CHARGE_LIMIT, null);
			return null;
		}

		// Avoid discharging the ESS
		if (calculatedPower < 0) {
			this.setDelayChargeStateAndLimit(DelayChargeState.NO_CHARGE_LIMIT, null);
			this.parent.logDebug("System would charge from the grid under these constraints");
			this.parent.channel(ControllerEssGridOptimizedCharge.ChannelId.DELAY_CHARGE_NEGATIVE_LIMIT)
					.setNextValue(true);
			return null;
		}

		// Avoid charging with low power
		if (calculatedPower == 0) {
			return 0;
		}

		/*
		 * Fit into maximum range and apply a ramp.
		 */

		// Reduce limit to MaxApparentPower to avoid very high values in the last
		// seconds
		calculatedPower = min(calculatedPower, maxApparentPower);

		/*
		 * Calculate the average with the last 900 limits
		 */
		IntegerReadChannel delayChargeLimitRawChannel = this.parent.getRawDelayChargeLimitChannel();
		this.parent._setRawDelayChargeLimit(calculatedPower);

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

		return TypeUtils.getAsType(OpenemsType.INTEGER, Math.round(limitValueOpt.getAsDouble()));
	}

	/**
	 * Calculates the charging power limit.
	 * 
	 * @param remainingCapacity        remaining capacity of the ess
	 * @param remainingTime            remaining time till target minute in seconds
	 * @param quarterHourlyProduction  predicted production
	 * @param quarterHourlyConsumption predicted consumption
	 * @param clock                    clock
	 * @param riskLevel                risk level
	 * @param maxApparentPower         maximum apparent power of the ess
	 * @param targetMinute             target as minute of the day
	 * @param minimumChargePower       minimumChargePower configured by the user
	 * @param parent                   {@link ControllerEssGridOptimizedChargeImpl}
	 *                                 to set debug channels
	 * @return the calculated charging power limit or null if no limit should be
	 *         applied
	 */
	protected static Integer getCalculatedPowerLimit(int remainingCapacity, int remainingTime,
			Integer[] quarterHourlyProduction, Integer[] quarterHourlyConsumption, Clock clock,
			DelayChargeRiskLevel riskLevel, int maxApparentPower, int targetMinute, double minimumChargePower,
			ControllerEssGridOptimizedChargeImpl parent) {

		Integer calculatedPower = null;

		// Do not divide by zero
		if (remainingTime <= 0) {
			return null;
		}
		// Calculate charge power limit
		calculatedPower = remainingCapacity / remainingTime;

		// Minimum power for more efficiency during a day (Avoid charging with low
		// power.
		if (calculatedPower < minimumChargePower) {
			calculatedPower = 0;
		}

		/**
		 * Check if the predicted Energy is enough to fully charge the battery.
		 * 
		 * Since the prediction of available energy is not suitable for every system, we
		 * apply this logic only to low-risk configurations for now.
		 */
		// Predicted available Energy
		double predictedAvailEnergy = DelayCharge.calculateAvailEnergy(quarterHourlyProduction,
				quarterHourlyConsumption, clock, targetMinute);

		// Buffer depending on the risk level configured.
		predictedAvailEnergy = predictedAvailEnergy * riskLevel.getEneryBuffer();

		float remainingCapacityWh = remainingCapacity / 60.0f / 60.0f;

		// Set Channel for historical analysis
		parent.channel(ControllerEssGridOptimizedCharge.ChannelId.DELAY_CHARGE_PREDICTED_ENERGY_LEFT)
				.setNextValue(predictedAvailEnergy);
		parent.channel(ControllerEssGridOptimizedCharge.ChannelId.DELAY_CHARGE_CAPACITY_WITH_BUFFER_LEFT)
				.setNextValue(remainingCapacityWh);
		parent.channel(ControllerEssGridOptimizedCharge.ChannelId.DELAY_CHARGE_TIME_LEFT).setNextValue(remainingTime);

		// The power should be only limited if the predicted available energy is enough
		if (riskLevel.equals(DelayChargeRiskLevel.LOW) && predictedAvailEnergy <= remainingCapacityWh) {
			return maxApparentPower;
		}

		return calculatedPower;
	}

	/**
	 * Get calculated power limit.
	 * 
	 * <p>
	 * Deprecated method, ignoring efficient charge power and predicted energy.
	 * 
	 * @param remainingCapacity remaining capacity of the ess
	 * @param remainingTime     remaining time till target minute in seconds
	 * 
	 * @return the calculated charging power limit or null if no limit should be
	 *         applied
	 */
	@Deprecated
	protected static Integer _getCalculatedPowerLimit(int remainingCapacity, int remainingTime) {

		Integer calculatedPower = null;

		if (remainingTime <= 0) {
			return null;
		}

		// Calculate charge power limit
		calculatedPower = remainingCapacity / remainingTime;

		return calculatedPower;
	}

	/**
	 * Calculates the number of seconds left to the target hour.
	 * 
	 * @param clock        clock
	 * @param targetMinute target as minute of the day
	 * @return the remaining time
	 */
	protected static int calculateRemainingTime(Clock clock, int targetMinute) {
		int targetSecondOfDay = targetMinute * 60;
		int remainingTime = targetSecondOfDay - ZonedDateTime.now(clock).get(ChronoField.SECOND_OF_DAY);

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
	 * @return the target minute
	 */
	protected static Optional<Integer> calculateTargetMinute(Integer[] quarterHourlyProduction,
			Integer[] quarterHourlyConsumption, ZonedDateTime predictionStartQuarterHour) {

		var predictionStartQuarterHourIndex = predictionStartQuarterHour.get(MINUTE_OF_DAY) / 15;

		// Last hour when production was greater than consumption.
		Optional<Integer> lastQuarterHour = Optional.empty();

		// Iterate predictions till midnight
		for (var i = 0; i < min(96 - predictionStartQuarterHourIndex, quarterHourlyProduction.length); i++) {
			// to avoid null and negative consumption values.
			if (quarterHourlyProduction[i] != null && quarterHourlyConsumption[i] != null
					&& quarterHourlyConsumption[i] >= 0) {

				// Updating last quarter hour if production is higher than consumption plus
				// power buffer
				if (quarterHourlyProduction[i] > quarterHourlyConsumption[i] + DEFAULT_POWER_BUFFER) {
					lastQuarterHour = Optional.of(i);
				}
			}
		}

		return lastQuarterHour.map(t -> {
			return predictionStartQuarterHour.plusMinutes(t * 15).get(MINUTE_OF_DAY);
		});
	}

	/**
	 * Calculate the available Energy.
	 * 
	 * @param targetMinute target minute
	 * @return available Energy for the remaining time
	 */
	/**
	 * Calculate the available Energy.
	 * 
	 * @param quarterHourlyProduction  predicted production
	 * @param quarterHourlyConsumption predicted consumption
	 * @param clock                    clock
	 * @param targetMinute             target as minute of the day
	 * @return available energy in Wh
	 */
	protected static int calculateAvailEnergy(Integer[] quarterHourlyProduction, Integer[] quarterHourlyConsumption,
			Clock clock, int targetMinute) {

		ZonedDateTime now = ZonedDateTime.now(clock);
		ZonedDateTime predictionStartQuarterHour = DelayCharge.roundZonedDateTimeDownTo15Minutes(now);

		int dailyStartIndex = predictionStartQuarterHour.get(MINUTE_OF_DAY) / 15;
		int dailyEndIndex = DelayCharge.getAsZonedDateTime(targetMinute, clock).get(MINUTE_OF_DAY) / 15;

		// Relevant quarter hours
		int endIndex = dailyEndIndex - dailyStartIndex;

		float productionEnergyTotal = 0;
		float consumptionEnergyTotal = 0;

		/*
		 * Summarize and calculate every quarterly power, if there is more than one
		 * quarter hour left
		 */
		if (endIndex > 0) {
			List<Integer> productionList = Arrays.asList(quarterHourlyProduction).subList(1, endIndex);
			List<Integer> consumptionList = Arrays.asList(quarterHourlyConsumption).subList(1, endIndex);

			productionEnergyTotal = productionList.stream() //
					.filter(Objects::nonNull) //
					.mapToInt(Integer::intValue) //
					.sum() * 0.25f;

			consumptionEnergyTotal = consumptionList.stream() //
					.filter(Objects::nonNull) //
					.mapToInt(Integer::intValue) //
					.sum() * 0.25f;
		}

		// Add energy of the first index separately to ignore the already passed energy
		float timeOfQuarterLeft = ChronoUnit.SECONDS.between(now, predictionStartQuarterHour.plusMinutes(15)) / 60.0f
				/ 60.0f;
		var currentProduction = quarterHourlyProduction[0] == null ? 0 : quarterHourlyProduction[0];
		var currentConsumption = quarterHourlyConsumption[0] == null ? 0 : quarterHourlyConsumption[0];
		float leftProdEnergy = timeOfQuarterLeft * currentProduction;
		float leftConsEnergy = timeOfQuarterLeft * currentConsumption;

		float productionEnergy = productionEnergyTotal + leftProdEnergy;
		float consumptionEnergy = consumptionEnergyTotal + leftConsEnergy;

		return Math.round(productionEnergy - consumptionEnergy);
	}

	/**
	 * Rounds a {@link ZonedDateTime} down to 15 minutes.
	 * 
	 * @param d the {@link ZonedDateTime}
	 * @return the rounded result
	 */
	private static ZonedDateTime roundZonedDateTimeDownTo15Minutes(ZonedDateTime d) {
		var minuteOfDay = d.get(MINUTE_OF_DAY);
		return d.with(ChronoField.NANO_OF_DAY, 0).plus(minuteOfDay / 15 * 15, ChronoUnit.MINUTES);
	}

	/**
	 * Rounds a {@link ZonedDateTime} up to 5 minutes.
	 * 
	 * @param d the {@link ZonedDateTime}
	 * @return the rounded result
	 */
	private static ZonedDateTime roundZonedDateTimeUpTo5Minutes(ZonedDateTime d) {
		var minuteOfDay = d.get(MINUTE_OF_DAY);
		long roundMinutes = TypeUtils.getAsType(OpenemsType.LONG, Math.ceil(minuteOfDay / 5.0) * 5);
		return d.with(ChronoField.NANO_OF_DAY, 0).plusMinutes(roundMinutes);
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
	 * @param clock        clock
	 * @return true if it is later than the target minute.
	 */
	private static boolean passedTargetMinute(int targetMinute, Clock clock) {

		if (ZonedDateTime.now(clock).get(MINUTE_OF_DAY) >= targetMinute) {
			return true;
		}
		return false;
	}

	protected static ZonedDateTime getAsZonedDateTime(int minuteOfDay, Clock clock) {
		ZonedDateTime now = ZonedDateTime.now(clock);
		return now.with(ChronoField.NANO_OF_DAY, 0) //
				.plus(minuteOfDay, ChronoUnit.MINUTES);
	}

	/**
	 * Gets the predicted charge start time as epoch seconds.
	 * 
	 * @param targetTime target time
	 * @param capacity   capacity of the ess
	 * @param soc        state of charge
	 * @param clock      clock
	 * @return start of charging the battery as epoch seconds
	 * @throws OpenemsException on error
	 */
	protected static Long getPredictedChargeStart(int targetTime, int capacity, int soc, Clock clock)
			throws OpenemsException {

		if (DelayCharge.passedTargetMinute(targetTime, clock)) {
			return null;
		}

		int remainingSoC = 100 - soc;
		var minPowerPercent = MINIMUM_POWER_FACTOR * 100;

		// Calculate required time
		double requiredTime = remainingSoC / minPowerPercent;
		var minutes = Math.round((requiredTime % 1) * 60);
		requiredTime = ((int) requiredTime * 60) + minutes;

		// Calculate start time
		int chargeStartMinute = TypeUtils.getAsType(OpenemsType.INTEGER, targetTime - requiredTime);

		// Format to ZonedDateTime
		var chargeStartTime = DelayCharge.getAsZonedDateTime(chargeStartMinute, clock);
		chargeStartTime = DelayCharge.roundZonedDateTimeUpTo5Minutes(chargeStartTime);

		var chargeStartLocal = LocalDate.now(clock)
				.atTime(LocalTime.of(chargeStartTime.getHour(), chargeStartTime.getMinute()));

		return ZonedDateTime.ofLocal(chargeStartLocal, ZoneId.systemDefault(), null).toEpochSecond();
	}
}
