package io.openems.edge.controller.ess.gridoptimizedselfconsumption;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;

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

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.predictor.api.manager.PredictorManager;
import io.openems.edge.predictor.api.oneday.Prediction24Hours;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.io.openems.edge.controller.ess.gridoptimizedselfconsumption", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class GridOptimizedSelfConsumptionImpl extends AbstractOpenemsComponent
		implements GridOptimizedSelfConsumption, Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(GridOptimizedSelfConsumptionImpl.class);

	private Config config = null;

	private Integer targetMinute;
	private boolean debugMode;

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

	public GridOptimizedSelfConsumptionImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				GridOptimizedSelfConsumption.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
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
		 * Set active power limits depending on the maximum sell to grid power
		 */

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
			return;
		}

		// Get the grid power and ess power
		int gridPower = this.meter.getActivePower().getOrError(); /* current buy-from/sell-to grid */

		// Checking if the grid power is above the maximum feed-in
		if (gridPower * -1 > this.config.maximumSellToGridPower()) {

			// Calculate actual limit for Ess
			int essPowerLimit = gridPower + this.ess.getActivePower().getOrError()
					+ this.config.maximumSellToGridPower();

			// Apply limit
			this.ess.setActivePowerLessOrEquals(essPowerLimit);
		}

		getCalculatedPowerLimit();

		if (this.ess instanceof HybridEss) {

		}

	}

	/**
	 * Calculates the charging power limit for the current cycle.
	 * 
	 * @return the calculated charging power limit or null if no limit should be
	 *         applied
	 * @throws OpenemsNamedException on error
	 */
	public Integer getCalculatedPowerLimit() throws OpenemsNamedException {

		ZonedDateTime now = ZonedDateTime.now(this.componentManager.getClock()).withZoneSameInstant(ZoneOffset.UTC);

		Integer calculatedPower = null;

		// Predictions
		Prediction24Hours hourlyPredictionProduction = this.predictorManager
				.get24HoursPrediction(new ChannelAddress("_sum", "ProductionActivePower"));
		Prediction24Hours hourlyPredictionConsumption = this.predictorManager
				.get24HoursPrediction(new ChannelAddress("_sum", "ConsumptionActivePower"));
		ZonedDateTime predictionStartQuarterHour = (roundZonedDateTimeDownTo15Minutes(now));

		// Predictions as Integer array
		Integer[] hourlyProduction = hourlyPredictionProduction.getValues();
		Integer[] hourlyConsumption = hourlyPredictionConsumption.getValues();

		/*
		 * Calculate the target hour once at midnight.
		 * 
		 * Possible improvement: calculate the target hour more often, e.g. once every
		 * hour to incorporate more accurate prediction during the day.
		 * 
		 * TODO: make sure target hour is calculated immediately at start of OpenEMS and
		 * not only at next midnight.
		 */
		// if (now.getHour() == 0 && !this.isTargetHourCalculated) {

		// Integer[] hourlyProduction =
		// productionHourlyPredictor.get24hPrediction().getValues();
		// Integer[] hourlyConsumption =
		// consumptionHourlyPredictor.get24hPrediction().getValues();

// calculating target hour
		this.targetMinute = this.calculateTargetMinute(hourlyProduction, hourlyConsumption, predictionStartQuarterHour);

// for running once
		// this.isTargetHourCalculated = true;
		// }

		// if (now.getHour() == 1 && this.isTargetHourCalculated) {
		// this.isTargetHourCalculated = false;
		// }

//Displays the production values in log.
		if (this.debugMode) {
			for (int i = 0; i < 24; i++) {
				this.logDebug(log, "Production[" + i + "] " + " - " + hourlyProduction[i] + " this.Consumption[" + i
						+ "] " + " - " + hourlyConsumption[i]);

				this.debugMode = false;
			}
		}

//target hour = null --> not enough production or Initial run(no values)
		if (this.targetMinute == null) {
			this.setChannels(State.TARGET_HOUR_NOT_CALCULATED, 0);
			return null;
		}

//crossed target hour
		if (now.get(ChronoField.MINUTE_OF_DAY) >= this.targetMinute) {

			this.setChannels(State.PASSED_TARGET_HOUR, 0);
			return null;
		}

//battery capacity in wh
		int capacity = ess.getCapacity().getOrError();

//Remaining capacity of the battery in Ws till target point.
		int remainingCapacity = capacity * (100 - ess.getSoc().getOrError()) * 36;

//No remaining capacity -> no restrictions
		if (remainingCapacity < 0) {
			this.setChannels(State.NO_REMAINING_CAPACITY, 0);
			return null;
		}

//remaining time in seconds till the target point.
		int remainingTime = calculateRemainingTime(now);

//calculate charge power limit
		calculatedPower = remainingCapacity / remainingTime;

//reduce limit to MaxApparentPower to avoid very high values in the last
//seconds
		calculatedPower = Math.min(calculatedPower, ess.getMaxApparentPower().getOrError());

		this.setChannels(State.ACTIVE_LIMIT, calculatedPower);

		return calculatedPower;
	}

	/**
	 * Calculates the number of seconds left to the target hour.
	 * 
	 * @return the remaining time
	 */
	private int calculateRemainingTime(ZonedDateTime now) {
		int targetSecondOfDay = this.targetMinute * 60;
		int remainingTime = targetSecondOfDay - now.get(ChronoField.SECOND_OF_DAY);

		return remainingTime;
	}

	/**
	 * Calculates the target minute from quarter-hourly production and consumption
	 * predictions.
	 * 
	 * @param hourlyProduction           the production prediction
	 * @param hourlyConsumption          the consumption prediction
	 * @param predictionStartQuarterHour the prediction start quarterHour
	 * @return the target hour
	 */
	private Integer calculateTargetMinute(Integer[] quaterHourlyProduction, Integer[] quaterHourlyConsumption,
			ZonedDateTime predictionStartQuarterHour) {

		// lastQuaterHour --> last hour when production was greater than consumption.
		int lastQuaterHour = 0;
		Integer targetMinuteActual = null;
		Integer targetMinuteAdjusted = null;
		int predictionStartQuarterHourIndex = predictionStartQuarterHour.get(ChronoField.MINUTE_OF_DAY) / 15;

		// iterate predictions till midnight
		for (int i = 0; i < (96 - predictionStartQuarterHourIndex); i++) {
			// to avoid null and negative consumption values.
			if ((quaterHourlyProduction[i] != null && quaterHourlyConsumption[i] != null
					&& quaterHourlyConsumption[i] >= 0)) {

				// Updating last quarter hour if production is higher than consumption plus
				// power buffer
				if (quaterHourlyProduction[i] > quaterHourlyConsumption[i] + this.config.powerBuffer()) {
					lastQuaterHour = i;
				}
			}
		}
		if (lastQuaterHour > 0) {

			// targetSecondActual = lastQuaterHour;
			targetMinuteActual = predictionStartQuarterHour.plusMinutes(lastQuaterHour * 15)
					.get(ChronoField.MINUTE_OF_DAY);

			// target hour adjusted based on buffer hour.
			targetMinuteAdjusted = targetMinuteActual - this.config.noOfBufferMinutes();
		}

		// setting the channel id values
		IntegerReadChannel targetHourActualValue = this
				.channel(GridOptimizedSelfConsumption.ChannelId.TARGET_MINUTE_ACTUAL);
		targetHourActualValue.setNextValue(targetMinuteActual);

		IntegerReadChannel targetHourAdjustedValue = this
				.channel(GridOptimizedSelfConsumption.ChannelId.TARGET_MINUTE_ADJUSTED);
		targetHourAdjustedValue.setNextValue(targetMinuteAdjusted);

		return targetMinuteAdjusted;
	}

	/**
	 * Update the StateMachine and ChargePowerLimit channels.
	 * 
	 * @param state the {@link State}
	 * @param limit the ChargePowerLimit
	 */
	private void setChannels(State state, int limit) {
		EnumReadChannel stateMachineChannel = this.channel(GridOptimizedSelfConsumption.ChannelId.STATE_MACHINE);
		stateMachineChannel.setNextValue(state);

		IntegerReadChannel chargePowerLimitChannel = this
				.channel(GridOptimizedSelfConsumption.ChannelId.CHARGE_POWER_LIMIT);
		chargePowerLimitChannel.setNextValue(limit);
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
}
