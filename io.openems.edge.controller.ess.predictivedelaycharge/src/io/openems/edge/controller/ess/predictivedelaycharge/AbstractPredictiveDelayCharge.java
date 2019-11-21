package io.openems.edge.controller.ess.predictivedelaycharge;

import java.time.Clock;
import java.time.LocalDateTime;

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

	private final Clock clock;

	private String essId;
	private int bufferHour;

	private boolean executed = false;
	private Integer targetHour;

	private LocalDateTime predictionStartHour;

	private Integer[] hourlyProduction = new Integer[24];
	private Integer[] hourlyConsumption = new Integer[24];

	private IntegerReadChannel targetHourvalue = this.channel(ChannelId.TARGET_HOUR);

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		STATE_MACHINE(Doc.of(State.values()) //
				.text("Current State of State-Machine")),
		CHARGE_POWER_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.text("Charge-Power limitation")),
		TARGET_HOUR(Doc.of(OpenemsType.INTEGER) //
				.text("Target hour"));

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
		this.clock = Clock.systemDefaultZone();
	}

	protected void activate(ComponentContext context, String id, String alias) {
		throw new IllegalArgumentException("Use the other activate method");
	}

	protected void activate(ComponentContext context, String id, String alias, boolean enabled, String meterId,
			String essId, int bufferHour) {
		super.activate(context, id, alias, enabled);
		this.essId = essId;
		this.bufferHour = bufferHour;
	}

	public Integer getCalculatedPower(ProductionHourlyPredictor productionHourlyPredictor,
			ConsumptionHourlyPredictor consumptionHourlyPredictor, ComponentManager componentManager)
			throws OpenemsNamedException {

		// Get required variables
		ManagedSymmetricEss ess = componentManager.getComponent(this.essId);

		LocalDateTime now = LocalDateTime.now(this.clock);

		Integer calculatedPower = null;

		// resets during midnight.
		if (now.getHour() == 0 && !this.executed) {
			this.hourlyProduction = productionHourlyPredictor.get24hPrediction().getValues();
			this.hourlyConsumption = consumptionHourlyPredictor.get24hPrediction().getValues();

			// Start hour of the predicted values
			this.predictionStartHour = productionHourlyPredictor.get24hPrediction().getStart();

			// calculating target hour
			this.targetHour = calculateTargetHour();

			// for running once
			this.executed = true;
		}

		if (now.getHour() == 1 && this.executed) {
			this.executed = false;
		}

		// target hour = 0 --> not enough production or Initial run(no values)
		if (this.targetHour == null) {
			this.setChannels(State.TARGET_HOUR_NOT_CALCULATED, 0);

			return null;
		}

		// buffer hour --> limits are not applied from this hour.
		int bufferHour = this.targetHour - this.bufferHour;

		// remaining time in seconds till the target point.
		int remainingTime = calculateRemainingTime();

		// reached buffer hour
		if (now.getHour() > bufferHour) {

			if (remainingTime < 0) {
				// crossed the target point
				this.setChannels(State.PASSED_TARGET_HOUR, 0);
				return null;
			} else {
				this.setChannels(State.PASSED_BUFFER_HOUR, 0);
			}

			return null;
		}

		// battery capacity in wh
		int capacity = ess.getCapacity().value().getOrError();

		// Remaining capacity of the battery in Ws till target point.
		int remainingCapacity = capacity * (100 - ess.getSoc().value().getOrError()) * 36;

		// No remaining capacity -> no restrictions
		if (remainingCapacity < 0) {
			this.setChannels(State.NO_REMAINING_CAPACITY, 0);
			return null;
		}

		// calculate charge power limit
		calculatedPower = remainingCapacity / remainingTime;

		// reduce limit to MaxApparentPower to avoid very high values in the last
		// seconds
		calculatedPower = Math.min(calculatedPower, ess.getMaxApparentPower().value().orElse(0));

		return calculatedPower;
	}

	private Integer calculateTargetHour() {

		// counter --> last hour when production > consumption.
		int lastHour = 0;
		Integer targetHour = null;

		for (int i = 0; i < 24; i++) {
			// to avoid null and negative consumption values.
			if ((this.hourlyProduction[i] != null && this.hourlyConsumption[i] != null
					&& this.hourlyConsumption[i] >= 0)) {
				if (this.hourlyProduction[i] > this.hourlyConsumption[i]) {
					lastHour = i;
				}
			}
		}
		if (lastHour != 0) {
			// target hour --> immediate next hour from the last Hour
			targetHour = this.predictionStartHour.plusHours(lastHour).plusHours(1).getHour();
		}

		// Setting the Target hour channel id
		targetHourvalue.setNextValue(this.targetHour);
		return targetHour;
	}

	// number of seconds left to the target hour.
	private int calculateRemainingTime() {
		int targetSecondOfDay = targetHour * 3600;
		int remainingTime = targetSecondOfDay - currentSecondOfDay();

		return remainingTime;
	}

	private int currentSecondOfDay() {
		LocalDateTime now = LocalDateTime.now();
		return now.getHour() * 3600 + now.getMinute() * 60 + now.getSecond();
	}

	private void setChannels(State state, int limit) {
		EnumReadChannel stateMachineChannel = this.channel(AbstractPredictiveDelayCharge.ChannelId.STATE_MACHINE);
		stateMachineChannel.setNextValue(state);

		IntegerReadChannel chargePowerLimitChannel = this
				.channel(AbstractPredictiveDelayCharge.ChannelId.CHARGE_POWER_LIMIT);
		chargePowerLimitChannel.setNextValue(limit);
	}
}
