package io.openems.edge.controller.ess.predictivedelaycharge;

import java.time.Clock;
import java.time.LocalDateTime;

import org.osgi.service.component.annotations.Reference;

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

public abstract class AbstractPredictiveDelayCharge extends AbstractOpenemsComponent
		implements Controller, OpenemsComponent {

	private final Clock clock;

	protected String meterId;
	protected String essId;
	protected String chargerId;
	protected int bufferHour;

	private boolean executed = false;
	private int targetHour = 0;
	private LocalDateTime predictionStartHour;
	private Integer calculatedPower;

	private Integer[] hourlyProduction = new Integer[24];
	private Integer[] hourlyConsumption = new Integer[24];

	@Reference
	protected ComponentManager componentManager;

	protected abstract ComponentManager getComponentManager();

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

	protected AbstractPredictiveDelayCharge(Clock clock, String componentId,
			io.openems.edge.common.channel.ChannelId channelId) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ChannelId.values()//
		);
		this.clock = clock;
	}

	public void run(Integer[] production, Integer[] consumption, LocalDateTime starthour) throws OpenemsNamedException {

		// Get required variables
		ManagedSymmetricEss ess = this.getComponentManager().getComponent(this.essId);

		LocalDateTime now = LocalDateTime.now(this.clock);

		this.calculatedPower = null;

		// resets during midnight.
		if (now.getHour() == 0 && !executed) {
			this.hourlyProduction = production;
			this.hourlyConsumption = consumption;

			// Start hour of the predicted values
			this.predictionStartHour = starthour;

			// calculating target hour
			this.targetHour = calculateTargetHour();

			// Setting the Target hour channel id
			IntegerReadChannel targetHour = this.channel(ChannelId.TARGET_HOUR);
			targetHour.setNextValue(this.targetHour);

			// for running once
			this.executed = true;
		}

		if (now.getHour() == 1 && executed) {
			this.executed = false;
		}

		// target hour = 0 --> not enough production or Initial run(no values)
		if (this.targetHour == 0) {
			this.setChannels(State.TARGET_HOUR_NOT_CALCULATED, 0);
			return;
		}

		// buffer hour --> limits are not applied from this hour.
		int bufferHour = this.targetHour - this.bufferHour;

		// reached buffer hour
		if (now.getHour() >= bufferHour) {
			this.setChannels(State.PASSED_BUFFER_HOUR, 0);
			return;
		}

		// remaining time in seconds till the target point.
		int remainingTime = calculateRemainingTime();

		// reached the target point
		if (remainingTime < 0) {
			this.setChannels(State.PASSED_TARGET_HOUR, 0);
			return;
		}

		// battery capacity in wh
		int capacity = ess.getCapacity().value().getOrError();

		// Remaining capacity of the battery in Ws till target point.
		int remainingCapacity = capacity * (100 - ess.getSoc().value().getOrError()) * 36;

		// No remaining capacity -> no restrictions
		if (remainingCapacity < 0) {
			this.setChannels(State.NO_REMAINING_CAPACITY, 0);
			return;
		}

		// calculate charge power limit
		this.calculatedPower = remainingCapacity / remainingTime;

		// reduce limit to MaxApparentPower to avoid very high values in the last
		// seconds
		this.calculatedPower = Math.min(this.calculatedPower, ess.getMaxApparentPower().value().orElse(0));
	}

	private int calculateTargetHour() {

		// counter --> last hour when production > consumption.
		int lastHour = 0;
		int targetHour = 0;

		for (int i = 0; i < 24; i++) {
			// to avoid null and negative consumption values.
			if ((this.hourlyProduction[i] != null && this.hourlyConsumption[i] != null
					&& this.hourlyConsumption[i] >= 0)) {
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

	public Integer getCalculatedPower() {
		return this.calculatedPower;
	}
}
