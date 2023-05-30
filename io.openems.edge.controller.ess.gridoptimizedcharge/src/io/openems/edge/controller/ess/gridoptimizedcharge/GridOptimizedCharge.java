package io.openems.edge.controller.ess.gridoptimizedcharge;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public interface GridOptimizedCharge extends Controller, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/**
		 * Current state of the delayed charge function.
		 */
		DELAY_CHARGE_STATE(Doc.of(DelayChargeState.values()) //
				.text("Current state of the delayed charge function.")),

		/**
		 * Current state of the sell to grid limit function.
		 */
		SELL_TO_GRID_LIMIT_STATE(Doc.of(SellToGridLimitState.values()) //
				.text("Current state of the sell to grid limit function.")),

		/**
		 * Delay-Charge power limitation.
		 */
		DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.text("Delay-Charge power limitation.")), //

		/**
		 * Capacity left used for delayed charge.
		 */
		DELAY_CHARGE_CAPACITY_WITH_BUFFER_LEFT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.text("Capacity left.")), //

		/**
		 * Time left used for delayed charge.
		 */
		DELAY_CHARGE_TIME_LEFT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS) //
				.text("Capacity left.")), //

		/**
		 * Predicted energy left used for delayed charge.
		 */
		DELAY_CHARGE_PREDICTED_ENERGY_LEFT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.text("Predicted energy left.")), //

		/**
		 * Raw Delay-Charge without consideration of the last limits.
		 * 
		 * <p>
		 * This channel is used for debugging and to calculate the average for the
		 * {@link ChannelId#DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT} channel.
		 */
		RAW_DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.text("Delay-Charge power limitation.")), //

		/**
		 * Sell to grid limit charge power limitation.
		 */
		SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.text("Sell to grid limit charge power limitation in a readable AC format.")),

		/**
		 * Raw sell to grid limit charge power limitation.
		 * 
		 * <p>
		 * This value is negative for DC systems. Prefer
		 * SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT for visualization.
		 */
		RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.text("Raw sell to grid limit charge power limitation.")),

		/**
		 * Predicted target minute as minute of the day.
		 * 
		 * <p>
		 * Actual target minute calculated from prediction without buffer hours (for
		 * automatic mode).
		 */
		PREDICTED_TARGET_MINUTE(Doc.of(OpenemsType.INTEGER) //
				.text("Actual target minute calculated from prediction without buffer hours.")),

		/**
		 * Predicted target minute adjusted with a buffer as minute of the day.
		 * 
		 * <p>
		 * Adjusted target minute calculated from prediction including the buffer hours
		 * (for automatic mode).
		 */
		PREDICTED_TARGET_MINUTE_ADJUSTED(Doc.of(OpenemsType.INTEGER) //
				.text("Adjusted target minute calculated from prediction including the buffer hours (for automatic mode).")),

		/**
		 * Target minute as epoch seconds.
		 * 
		 * <p>
		 * Automatically set, when the original TARGET_MINUTE is set.
		 */
		TARGET_EPOCH_SECONDS(Doc.of(OpenemsType.LONG) //
				.text("Target minute as epoch seconds independent of the current mode Manual and Automatic.")),

		/**
		 * Target minute as minute of the day.
		 * 
		 * <p>
		 * Target minute independent of the current mode Manual and Automatic.
		 */
		TARGET_MINUTE(new IntegerDoc() //
				.text("Target minute independent of the current mode Manual and Automatic.") //
				.<GridOptimizedChargeImpl>onChannelSetNextValue((self, value) -> {
					value.asOptional().ifPresent(targetTime -> {
						var targetDateTime = LocalDate.now(self.componentManager.getClock())
								.atTime(LocalTime.of(targetTime / 60, targetTime % 60));

						var zonedDateTime = ZonedDateTime.ofLocal(targetDateTime, ZoneId.systemDefault(), null);
						var targetEpochTime = zonedDateTime.toEpochSecond();

						self.channel(ChannelId.TARGET_EPOCH_SECONDS).setNextValue(targetEpochTime);
					});
				})),

		/**
		 * Start time as epoch seconds of the current day.
		 * 
		 * <p>
		 * Keeps the time, when the production higher than the consumption for the first
		 * time on the current day.
		 */
		START_EPOCH_SECONDS(Doc.of(OpenemsType.LONG) //
				.text("Time when the production is higher than the consumption for the first time on the current day.")), //

		/**
		 * Predicted charge start time as epoch seconds of the current day.
		 * 
		 * <p>
		 * Keeps the time, when the delayed charge will start on the current day.
		 */
		PREDICTED_CHARGE_START_EPOCH_SECONDS(Doc.of(OpenemsType.LONG) //
				.text("Time when the delayed charge will start on the current day.")), //

		/**
		 * Debug Channel for the minimum charge power for delay Charge.
		 */
		DEBUG_DELAY_CHARGE_MINIMUM_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT) //
				.text("Debug Channel for the minimum charge power for delay Charge.")),

		/**
		 * Info State Channel, if the delay charge limit would be negative.
		 */
		DELAY_CHARGE_NEGATIVE_LIMIT(Doc.of(Level.INFO) //
				.text("System would be charged from the grid under these constraints.")), //

		/**
		 * Configured manual target time is not valid.
		 */
		NO_VALID_MANUAL_TARGET_TIME(Doc.of(Level.INFO) //
				.text("Configured manual target time is not valid. Default of 5 pm is used.")), //

		/**
		 * Configured ESS is not managed.
		 */
		CONFIGURED_ESS_IS_NOT_MANAGED(Doc.of(Level.FAULT) //
				.text("The Energy Storage System is in read-only mode and does not allow to be controlled.")), //

		/**
		 * Cumulated seconds of the state delay charge.
		 */
		DELAY_CHARGE_TIME(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_SECONDS) //
				.persistencePriority(PersistencePriority.HIGH)), //

		/**
		 * Cumulated seconds of the state sell to grid limit.
		 */
		SELL_TO_GRID_LIMIT_TIME(Doc.of(OpenemsType.LONG)//
				.unit(Unit.CUMULATED_SECONDS) //
				.persistencePriority(PersistencePriority.HIGH)), //

		/**
		 * Cumulated seconds of the state avoid low charging.
		 */
		AVOID_LOW_CHARGING_TIME(Doc.of(OpenemsType.LONG)//
				.unit(Unit.CUMULATED_SECONDS) //
				.persistencePriority(PersistencePriority.HIGH)), //

		/**
		 * Cumulated seconds if no limitation is present.
		 */
		NO_LIMITATION_TIME(Doc.of(OpenemsType.LONG)//
				.unit(Unit.CUMULATED_SECONDS) //
				.persistencePriority(PersistencePriority.HIGH)),//
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the Channel for {@link ChannelId#DELAY_CHARGE_STATE}.
	 *
	 * @return the Channel
	 */
	public default Channel<DelayChargeState> getDelayChargeStateChannel() {
		return this.channel(ChannelId.DELAY_CHARGE_STATE);
	}

	/**
	 * Gets the Status of the grid optimized self consumption. See
	 * {@link ChannelId#DELAY_CHARGE_STATE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default DelayChargeState getDelayChargeState() {
		return this.getDelayChargeStateChannel().value().asEnum();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#DELAY_CHARGE_STATE} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDelayChargeState(DelayChargeState value) {
		this.getDelayChargeStateChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SELL_TO_GRID_LIMIT_STATE}.
	 *
	 * @return the Channel
	 */
	public default Channel<SellToGridLimitState> getSellToGridLimitStateChannel() {
		return this.channel(ChannelId.SELL_TO_GRID_LIMIT_STATE);
	}

	/**
	 * Gets the Status of the grid optimized self consumption. See
	 * {@link ChannelId#SELL_TO_GRID_LIMIT_STATE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default SellToGridLimitState getSellToGridLimitState() {
		return this.getSellToGridLimitStateChannel().value().asEnum();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#SELL_TO_GRID_LIMIT_STATE} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setSellToGridLimitState(SellToGridLimitState value) {
		this.getSellToGridLimitStateChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDelayChargeLimitChannel() {
		return this.channel(ChannelId.DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT);
	}

	/**
	 * Gets the delay charge power limit in [W]. See
	 * {@link ChannelId#DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDelayChargeLimit() {
		return this.getDelayChargeLimitChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDelayChargeLimit(Integer value) {
		this.getDelayChargeLimitChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDelayChargeLimit(int value) {
		this.getDelayChargeLimitChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#RAW_DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getRawDelayChargeLimitChannel() {
		return this.channel(ChannelId.RAW_DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT);
	}

	/**
	 * Gets the delay charge power limit in [W]. See
	 * {@link ChannelId#RAW_DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getRawDelayChargeLimit() {
		return this.getRawDelayChargeLimitChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#RAW_DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setRawDelayChargeLimit(Integer value) {
		this.getRawDelayChargeLimitChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#RAW_DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setRawDelayChargeLimit(int value) {
		this.getRawDelayChargeLimitChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for
	 * {@link ChannelId#SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getSellToGridLimitMinimumChargeLimitChannel() {
		return this.channel(ChannelId.SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT);
	}

	/**
	 * Gets the sell to grid limit charge power limit in [W]. See
	 * {@link ChannelId#SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getSellToGridLimitMinimumChargeLimit() {
		return this.getSellToGridLimitMinimumChargeLimitChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setSellToGridLimitMinimumChargeLimit(Integer value) {
		this.getSellToGridLimitMinimumChargeLimitChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setSellToGridLimitMinimumChargeLimit(int value) {
		this.getSellToGridLimitMinimumChargeLimitChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getRawSellToGridLimitChargeLimitChannel() {
		return this.channel(ChannelId.RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT);
	}

	/**
	 * Gets the raw sell to grid limit charge power limit in [W]. See
	 * {@link ChannelId#RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getRawSellToGridLimitChargeLimit() {
		return this.getRawSellToGridLimitChargeLimitChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setRawSellToGridLimitChargeLimit(Integer value) {
		this.getRawSellToGridLimitChargeLimitChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#RAW_SELL_TO_GRID_LIMIT_CHARGE_LIMIT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setRawSellToGridLimitChargeLimit(int value) {
		this.getRawSellToGridLimitChargeLimitChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#PREDICTED_TARGET_MINUTE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getPredictedTargetMinuteChannel() {
		return this.channel(ChannelId.PREDICTED_TARGET_MINUTE);
	}

	/**
	 * Gets the predicted target minute of the Day. See
	 * {@link ChannelId#PREDICTED_TARGET_MINUTE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getPredictedTargetMinute() {
		return this.getPredictedTargetMinuteChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#PREDICTED_TARGET_MINUTE} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setPredictedTargetMinute(Integer value) {
		this.getPredictedTargetMinuteChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#PREDICTED_TARGET_MINUTE} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setPredictedTargetMinute(int value) {
		this.getPredictedTargetMinuteChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#PREDICTED_TARGET_MINUTE_ADJUSTED}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getPredictedTargetMinuteAdjustedChannel() {
		return this.channel(ChannelId.PREDICTED_TARGET_MINUTE_ADJUSTED);
	}

	/**
	 * Gets the adjusted predicted target minute of the Day. See
	 * {@link ChannelId#PREDICTED_TARGET_MINUTE_ADJUSTED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getPredictedTargetMinuteAdjusted() {
		return this.getPredictedTargetMinuteAdjustedChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#PREDICTED_TARGET_MINUTE_ADJUSTED} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setPredictedTargetMinuteAdjusted(Integer value) {
		this.getPredictedTargetMinuteAdjustedChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#PREDICTED_TARGET_MINUTE_ADJUSTED} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setPredictedTargetMinuteAdjusted(int value) {
		this.getPredictedTargetMinuteAdjustedChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#TARGET_MINUTE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getTargetMinuteChannel() {
		return this.channel(ChannelId.TARGET_MINUTE);
	}

	/**
	 * Gets the actual target minute of the Day. See
	 * {@link ChannelId#TARGET_MINUTE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getTargetMinute() {
		return this.getTargetMinuteChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#TARGET_MINUTE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setTargetMinute(Integer value) {
		this.getTargetMinuteChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#TARGET_MINUTE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setTargetMinute(int value) {
		this.getTargetMinuteChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#START_EPOCH_SECONDS}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getStartEpochSecondsChannel() {
		return this.channel(ChannelId.START_EPOCH_SECONDS);
	}

	/**
	 * Gets the actual start time of the Day. See
	 * {@link ChannelId#START_EPOCH_SECONDS}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Long> getStartEpochSeconds() {
		return this.getStartEpochSecondsChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#START_EPOCH_SECONDS} Channel.
	 *
	 * @param value the next value
	 * @param clock clock
	 */
	public default void _setStartEpochSeconds(LocalTime value, Clock clock) {

		var startDateTime = LocalDate.now(clock).atTime(value);
		var zonedDateTime = ZonedDateTime.ofLocal(startDateTime, clock.getZone(), null);

		this.getStartEpochSecondsChannel().setNextValue(zonedDateTime.toEpochSecond());
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#START_EPOCH_SECONDS} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setStartEpochSeconds(Long value) {
		this.getStartEpochSecondsChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#PREDICTED_CHARGE_START_EPOCH_SECONDS}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getPredictedChargeStartEpochSecondsChannel() {
		return this.channel(ChannelId.PREDICTED_CHARGE_START_EPOCH_SECONDS);
	}

	/**
	 * Gets the actual predicted charge start time of the Day. See
	 * {@link ChannelId#PREDICTED_CHARGE_START_EPOCH_SECONDS}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Long> getPredictedChargeStartEpochSeconds() {
		return this.getPredictedChargeStartEpochSecondsChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#PREDICTED_CHARGE_START_EPOCH_SECONDS} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setPredictedChargeStartEpochSeconds(Long value) {
		this.getPredictedChargeStartEpochSecondsChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#DEBUG_DELAY_CHARGE_MINIMUM_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDebugDelayChargeMinimumPowerChannel() {
		return this.channel(ChannelId.DEBUG_DELAY_CHARGE_MINIMUM_POWER);
	}

	/**
	 * Gets the information of the minimum delay charge power. See
	 * {@link ChannelId#DEBUG_DELAY_CHARGE_MINIMUM_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDebugDelayChargeMinimumPower() {
		return this.getDebugDelayChargeMinimumPowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#DEBUG_DELAY_CHARGE_MINIMUM_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDebugDelayChargeMinimumPower(Integer value) {
		this.getDebugDelayChargeMinimumPowerChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CONFIGURED_ESS_IS_NOT_MANAGED}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getConfiguredEssIsNotManagedChannel() {
		return this.channel(ChannelId.CONFIGURED_ESS_IS_NOT_MANAGED);
	}

	/**
	 * Gets the boolean if the state channel is active. See
	 * {@link ChannelId#CONFIGURED_ESS_IS_NOT_MANAGED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getConfiguredEssIsNotManaged() {
		return this.getConfiguredEssIsNotManagedChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#CONFIGURED_ESS_IS_NOT_MANAGED} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setConfiguredEssIsNotManaged(Boolean value) {
		this.getConfiguredEssIsNotManagedChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#DELAY_CHARGE_TIME}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDelayChargeTimeChannel() {
		return this.channel(ChannelId.DELAY_CHARGE_TIME);
	}

	/**
	 * Gets the actual time counter in seconds of the state delay charge. See
	 * {@link ChannelId#DELAY_CHARGE_TIME}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDelayChargeTime() {
		return this.getDelayChargeTimeChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#DELAY_CHARGE_TIME}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDelayChargeTime(Integer value) {
		this.getDelayChargeTimeChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#DELAY_CHARGE_TIME}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDelayChargeTime(int value) {
		this.getDelayChargeTimeChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SELL_TO_GRID_LIMIT_TIME}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getSellToGridLimitTimeChannel() {
		return this.channel(ChannelId.SELL_TO_GRID_LIMIT_TIME);
	}

	/**
	 * Gets the actual time counter in seconds of the state sell to grid limit. See
	 * {@link ChannelId#SELL_TO_GRID_LIMIT_TIME}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getSellToGridLimitTime() {
		return this.getSellToGridLimitTimeChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#SELL_TO_GRID_LIMIT_TIME} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setSellToGridLimitTime(Integer value) {
		this.getSellToGridLimitTimeChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#SELL_TO_GRID_LIMIT_TIME} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setSellToGridLimitTime(int value) {
		this.getSellToGridLimitTimeChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#NO_LIMITATION_TIME}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getNoLimitationTimeChannel() {
		return this.channel(ChannelId.DELAY_CHARGE_TIME);
	}

	/**
	 * Gets the actual time counter in seconds for the time, no limitation is
	 * active. See {@link ChannelId#NO_LIMITATION_TIME}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getNoLimitationTime() {
		return this.getNoLimitationTimeChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#NO_LIMITATION_TIME} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setNoLimitationTime(Integer value) {
		this.getNoLimitationTimeChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#NO_LIMITATION_TIME} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setNoLimitationTime(int value) {
		this.getNoLimitationTimeChannel().setNextValue(value);
	}
}
