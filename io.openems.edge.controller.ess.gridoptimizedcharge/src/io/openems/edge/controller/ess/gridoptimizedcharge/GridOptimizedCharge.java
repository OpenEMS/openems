package io.openems.edge.controller.ess.gridoptimizedcharge;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public interface GridOptimizedCharge extends Controller, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		DELAY_CHARGE_STATE(Doc.of(DelayChargeState.values()) //
				.text("Current state of the delayed charge function")),
		SELL_TO_GRID_LIMIT_STATE(Doc.of(SellToGridLimitState.values()) //
				.text("Current state of the sell to grid limit function")),
		DELAY_CHARGE_MAXIMUM_CHARGE_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.text("Delay-Charge power limitation")), //
		SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.text("Sell to grid limit charge power limitation")),
		PREDICTED_TARGET_MINUTE(Doc.of(OpenemsType.INTEGER) //
				.text("Actual target minute calculated from prediction without buffer hours")),
		PREDICTED_TARGET_MINUTE_ADJUSTED(Doc.of(OpenemsType.INTEGER) //
				.text("Adjusted target minute calculated from prediction including the buffer hours (for automatic mode)")),
		TARGET_MINUTE(Doc.of(OpenemsType.INTEGER) //
				.text("Target minute independent of the current mode Manual and Automatic"));
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
	public default Channel<DelayChargeState> getSellToGridLimitStateChannel() {
		return this.channel(ChannelId.SELL_TO_GRID_LIMIT_STATE);
	}

	/**
	 * Gets the Status of the grid optimized self consumption. See
	 * {@link ChannelId#SELL_TO_GRID_LIMIT_STATE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default DelayChargeState getSellToGridLimitState() {
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
	 * Gets the Channel for
	 * {@link ChannelId#SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getSellToGridLimitChargeLimitChannel() {
		return this.channel(ChannelId.SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT);
	}

	/**
	 * Gets the sell to grid limit charge power limit in [W]. See
	 * {@link ChannelId#SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getSellToGridLimitChargeLimit() {
		return this.getSellToGridLimitChargeLimitChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setSellToGridLimitChargeLimit(Integer value) {
		this.getSellToGridLimitChargeLimitChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#SELL_TO_GRID_LIMIT_MINIMUM_CHARGE_LIMIT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setSellToGridLimitChargeLimit(int value) {
		this.getSellToGridLimitChargeLimitChannel().setNextValue(value);
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
}
