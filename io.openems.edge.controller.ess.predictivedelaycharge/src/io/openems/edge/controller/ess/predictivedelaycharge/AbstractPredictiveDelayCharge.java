package io.openems.edge.controller.ess.predictivedelaycharge;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.osgi.service.component.ComponentContext;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.predictor.api.ConsumptionHourlyPredictor;
import io.openems.edge.predictor.api.ProductionHourlyPredictor;

public abstract class AbstractPredictiveDelayCharge extends AbstractOpenemsComponent implements OpenemsComponent {

	/**
	 * The number of buffer hours to make sure the battery still charges full, even
	 * on prediction errors.
	 */
	private int noOfBufferHours;

	private boolean isTargetHourCalculated = false;
	private Integer targetHour;
	private boolean debugMode;
	private Integer[] hourlyProduction = new Integer[24];
	private Integer[] hourlyConsumption = new Integer[24];

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		STATE_MACHINE(Doc.of(State.values()) //
				.text("Current State of State-Machine")),
		CHARGE_POWER_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.text("Charge-Power limitation")),
		TARGET_HOUR_ACTUAL(Doc.of(OpenemsType.INTEGER) //
				.text("Actual Target hour calculated from prediction")),
		TARGET_HOUR_ADJUSTED(Doc.of(OpenemsType.INTEGER) //
				.text("Actual Target hour calculated from prediction"));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	protected AbstractPredictiveDelayCharge() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ChannelId.values()//
		);
	}

	protected void activate(ComponentContext context, String id, String alias) {
		throw new IllegalArgumentException("Use the other activate method");
	}

	/**
	 * Abstract activator.
	 * 
	 * @param context         the Bundle context
	 * @param id              the Component-ID
	 * @param alias           the Component Alias
	 * @param enabled         is the Component enabled?
	 * @param meterId         the Meter-ID
	 * @param noOfBufferHours the number of buffer hours to make sure the battery
	 *                        still charges full, even on prediction errors
	 */
	protected void activate(ComponentContext context, String id, String alias, boolean enabled, String meterId,
			int noOfBufferHours, boolean debugMode) {
		super.activate(context, id, alias, enabled);
		this.noOfBufferHours = noOfBufferHours;
		this.debugMode = debugMode;
	}

	/**
	 * Calculates the charging power limit for the current cycle.
	 * 
	 * @param productionHourlyPredictor  the {@link ProductionHourlyPredictor}
	 * @param consumptionHourlyPredictor the {@link ConsumptionHourlyPredictor}
	 * @param componentManager           the {@link ComponentManager}
	 * @return the calculated charging power limit or null if no limit should be
	 *         applied
	 * @throws OpenemsNamedException on error
	 */
	public Integer getCalculatedPower(ManagedSymmetricEss ess, ProductionHourlyPredictor productionHourlyPredictor,
			ConsumptionHourlyPredictor consumptionHourlyPredictor, ComponentManager componentManager)
			throws OpenemsNamedException {

		ZonedDateTime now = ZonedDateTime.now(this.getComponentManager().getClock())
				.withZoneSameInstant(ZoneOffset.UTC);

		Integer calculatedPower = null;

		/*
		 * Calculate the target hour once at midnight.
		 * 
		 * Possible improvement: calculate the target hour more often, e.g. once every
		 * hour to incorporate more accurate prediction during the day.
		 * 
		 * TODO: make sure target hour is calculated immediately at start of OpenEMS and
		 * not only at next midnight.
		 * 
		 * TODO: update production and consumption predictor to use local timeseries
		 * data.
		 */
		if (now.getHour() == 0 && !this.isTargetHourCalculated) {
			Integer[] hourlyProduction = productionHourlyPredictor.get24hPrediction().getValues();
			Integer[] hourlyConsumption = consumptionHourlyPredictor.get24hPrediction().getValues();

			// Start hour of the predicted values
			ZonedDateTime predictionStartHour = productionHourlyPredictor.get24hPrediction().getStart();

			// For Debug Purpose
			this.hourlyProduction = hourlyProduction;
			this.hourlyConsumption = hourlyConsumption;

			// calculating target hour
			this.targetHour = this.calculateTargetHour(hourlyProduction, hourlyConsumption, predictionStartHour);

			// for running once
			this.isTargetHourCalculated = true;
		}

		if (now.getHour() == 1 && this.isTargetHourCalculated) {
			this.isTargetHourCalculated = false;
		}

		// Displays the production values in log.
		if (this.debugMode) {
			for (int i = 0; i < 24; i++) {
				System.out.println("Production[" + i + "] " + " - " + this.hourlyProduction[i] + " this.Consumption["
						+ i + "] " + " - " + hourlyConsumption[i]);
			}
			this.debugMode = false;
		}

		// target hour = null --> not enough production or Initial run(no values)
		if (this.targetHour == null) {
			this.setChannels(State.TARGET_HOUR_NOT_CALCULATED, 0);
			return null;
		}

		// crossed target hour
		if (now.getHour() >= this.targetHour) {

			this.setChannels(State.PASSED_TARGET_HOUR, 0);
			return null;
		}

		// battery capacity in wh
		int capacity = ess.getCapacity().getOrError();

		// Remaining capacity of the battery in Ws till target point.
		int remainingCapacity = capacity * (100 - ess.getSoc().getOrError()) * 36;

		// No remaining capacity -> no restrictions
		if (remainingCapacity < 0) {
			this.setChannels(State.NO_REMAINING_CAPACITY, 0);
			return null;
		}

		// remaining time in seconds till the target point.
		int remainingTime = calculateRemainingTime();

		// calculate charge power limit
		calculatedPower = remainingCapacity / remainingTime;

		// reduce limit to MaxApparentPower to avoid very high values in the last
		// seconds
		calculatedPower = Math.min(calculatedPower, ess.getMaxApparentPower().orElse(0));

		this.setChannels(State.ACTIVE_LIMIT, calculatedPower);

		return calculatedPower;
	}

	/**
	 * Calculates the target hour from hourly production and consumption
	 * predictions.
	 * 
	 * @param hourlyProduction    the production prediction
	 * @param hourlyConsumption   the consumption prediction
	 * @param predictionStartHour
	 * @return the target hour
	 */
	private Integer calculateTargetHour(Integer[] hourlyProduction, Integer[] hourlyConsumption,
			ZonedDateTime predictionStartHour) {

		// lastHour --> last hour when production was greater than consumption.
		int lastHour = 0;
		Integer targetHourActual = null;
		Integer targetHourAdjusted = null;

		for (int i = 0; i < 24; i++) {
			// to avoid null and negative consumption values.
			if ((hourlyProduction[i] != null && hourlyConsumption[i] != null && hourlyConsumption[i] >= 0)) {
				if (hourlyProduction[i] > hourlyConsumption[i]) {
					lastHour = i;
				}
			}
		}
		if (lastHour > 0) {
			// target hour --> immediate next hour from the last Hour
			targetHourActual = predictionStartHour.plusHours(lastHour).getHour();

			// target hour adjusted based on buffer hour.
			targetHourAdjusted = targetHourActual - this.noOfBufferHours;
		}

		// setting the channel id values
		IntegerReadChannel targetHourActualValue = this.channel(ChannelId.TARGET_HOUR_ACTUAL);
		targetHourActualValue.setNextValue(targetHourActual);

		IntegerReadChannel targetHourAdjustedValue = this.channel(ChannelId.TARGET_HOUR_ADJUSTED);
		targetHourAdjustedValue.setNextValue(targetHourAdjusted);

		return targetHourAdjusted;
	}

	/**
	 * Calculates the number of seconds left to the target hour.
	 * 
	 * @return the remaining time
	 */
	private int calculateRemainingTime() {
		int targetSecondOfDay = this.targetHour * 3600;
		int remainingTime = targetSecondOfDay - this.currentSecondOfDay();

		return remainingTime;
	}

	/**
	 * Gets the current second of the day.
	 * 
	 * @return the current second of the day
	 */
	private int currentSecondOfDay() {
		ZonedDateTime now = ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC);
		return now.getHour() * 3600 + now.getMinute() * 60 + now.getSecond();
	}

	/**
	 * Update the StateMachine and ChargePowerLimit channels.
	 * 
	 * @param state the {@link State}
	 * @param limit the ChargePowerLimit
	 */
	private void setChannels(State state, int limit) {
		EnumReadChannel stateMachineChannel = this.channel(AbstractPredictiveDelayCharge.ChannelId.STATE_MACHINE);
		stateMachineChannel.setNextValue(state);

		IntegerReadChannel chargePowerLimitChannel = this
				.channel(AbstractPredictiveDelayCharge.ChannelId.CHARGE_POWER_LIMIT);
		chargePowerLimitChannel.setNextValue(limit);
	}

	protected abstract ComponentManager getComponentManager();
}
