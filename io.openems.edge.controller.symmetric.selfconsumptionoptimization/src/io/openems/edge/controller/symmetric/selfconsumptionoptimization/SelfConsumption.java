package io.openems.edge.controller.symmetric.selfconsumptionoptimization;

import java.time.Clock;
import java.time.LocalDateTime;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

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
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.predictor.api.ConsumptionHourlyPredictor;
import io.openems.edge.predictor.api.HourlyPrediction;
import io.openems.edge.predictor.api.HourlyPredictor;
import io.openems.edge.predictor.api.ProductionHourlyPredictor;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.Symmetric.SelfConsmptionOptimization", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class SelfConsumption extends AbstractOpenemsComponent implements Controller, OpenemsComponent, HourlyPredictor {

	@Reference
	protected ComponentManager componentManager;

	@Reference
	protected ProductionHourlyPredictor productionHourlyPredictor;

	@Reference
	protected ConsumptionHourlyPredictor consumptionHourlyPredictor;

	private final Clock clock;
	private int targetHour = 0;
	private LocalDateTime predictionStartHour;
	private boolean executed = false;

	private Config config = null;

	private Integer[] hourlyProduction = new Integer[24];
	private Integer[] hourlyConsumption = new Integer[24];

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		STATE_MACHINE(Doc.of(State.values()) //
				.text("Current State of State-Machine")),
		CHARGE_POWER_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.text("Charge-Power limitation")),
		TARGET_HOUR(Doc.of(OpenemsType.INTEGER) //
				.text("Target hour")),
		GRID_POWER(Doc.of(OpenemsType.INTEGER) //
				.text("Actual Grid Power")); //

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public SelfConsumption() {
		this(Clock.systemDefaultZone());
	}

	public SelfConsumption(Clock clock) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ChannelId.values() //
		);
		this.clock = clock;
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {

		// Get required variables
		ManagedSymmetricEss ess = this.componentManager.getComponent(this.config.ess_id());
		SymmetricMeter meter = this.componentManager.getComponent(this.config.meter_id());

		int capacity = ess.getCapacity().value().getOrError();

		LocalDateTime now = LocalDateTime.now();

		// start hour of the controller has been given in config in order to enable it
		// whenever possible for testing.
		if (now.getHour() == config.Start_hour() && !executed) {
			this.hourlyProduction = productionHourlyPredictor.get24hPrediction().getValues();
			this.hourlyConsumption = consumptionHourlyPredictor.get24hPrediction().getValues();

			// Start hour of the predicted values
			this.predictionStartHour = productionHourlyPredictor.get24hPrediction().getStart();

			// calculating target hour
			this.targetHour = calculateTargetHour();

			// Setting the Target hour channel id
			IntegerReadChannel targetHour = this.channel(ChannelId.TARGET_HOUR);
			targetHour.setNextValue(this.targetHour);

			// for running once
			this.executed = true;
		}

		if ((now.getHour() == config.Start_hour() + 1) && executed) {
			this.executed = false;
		}

		// condition for initial run
		if (this.targetHour == 0) {
			this.setChannels(State.TARGET_HOUR_NOT_CALCULATED, 0);
			return;
		}

		int remainingTime = calculateRemainingTime();

		if (remainingTime <= 0) {
			this.setChannels(State.PASSED_TARGET_HOUR, 0);
			return;
		}

		// calculate remaining capacity in Ws
		int remainingCapacity = capacity * (100 - ess.getSoc().value().getOrError()) * 36;

		// No remaining capacity -> no restrictions
		if (remainingCapacity < 0) {
			this.setChannels(State.NO_REMAINING_CAPACITY, 0);
			return;
		}

		int bufferHour = this.targetHour - this.config.Buffer_hours();

		if (now.getHour() >= bufferHour) {
			this.setChannels(State.PASSED_BUFFER_HOUR, 0);
			return;
		}

		// Calculating the actual grid power
		int gridPower = meter.getActivePower().value().orElse(0) /* current buy-from/sell-to grid */
				+ ess.getActivePower().value().orElse(0); /* current charge/discharge Ess */

		// Setting the Grid Power channel id
		IntegerReadChannel actualGridPower = this.channel(ChannelId.GRID_POWER);
		actualGridPower.setNextValue(gridPower);

		int maximumFeedIn = config.Maximum_Feed_In();

		// calculate charge power limit
		int limit = remainingCapacity / remainingTime * -1;

		int feedIn = gridPower - limit;

		// Checking if the grid power is below the maximum feed-in
		if (feedIn > maximumFeedIn) {
			this.setChannels(State.REDUCING_THE_FEED_IN_POWER, 0);

			// Adjusting the limit to the maximum feed-in.
			limit = gridPower - maximumFeedIn;
		}

		// reduce limit to MaxApparentPower to avoid very high values in the last
		// seconds
		limit = Math.min(limit, ess.getMaxApparentPower().value().orElse(0));

		// set ActiveLimit channel
		setChannels(State.ACTIVE_LIMIT, limit * -1);

		// Set limitation for ChargePower
		ess.getSetActivePowerGreaterOrEquals().setNextWriteValue(limit);
	}

	private int calculateTargetHour() {

		// counter --> last hour when production > consumption.
		int lastHour = 0;
		int targetHour = 0;

		for (int i = 0; i < this.hourlyProduction.length; i++) {
			// to avoid null and negative consumption values.
			if ((this.hourlyProduction[i] != null && this.hourlyConsumption[i] != null && this.hourlyConsumption[i] >= 0)) {
				if (this.hourlyProduction[i] > this.hourlyConsumption[i]) {
					lastHour = i;
				}
			}
		}

		// target hour --> immediate next hour from the last Hour
		targetHour = this.predictionStartHour.plusHours(lastHour).plusHours(1).getHour();
		return targetHour;
	}

	// number of seconds left to the target hour.
	private int calculateRemainingTime() {
		int targetSecondOfDay = targetHour * 3600;
		int remainingTime = targetSecondOfDay - currentSecondOfDay();

		return remainingTime;
	}

	private int currentSecondOfDay() {
		LocalDateTime now = LocalDateTime.now(this.clock);
		return now.getHour() * 3600 + now.getMinute() * 60 + now.getSecond();
	}

	private void setChannels(State state, int limit) {
		EnumReadChannel stateMachineChannel = this.channel(ChannelId.STATE_MACHINE);
		stateMachineChannel.setNextValue(state);

		IntegerReadChannel chargePowerLimitChannel = this.channel(ChannelId.CHARGE_POWER_LIMIT);
		chargePowerLimitChannel.setNextValue(limit);
	}

	@Override
	public HourlyPrediction get24hPrediction() {
		return null;
	}

}
