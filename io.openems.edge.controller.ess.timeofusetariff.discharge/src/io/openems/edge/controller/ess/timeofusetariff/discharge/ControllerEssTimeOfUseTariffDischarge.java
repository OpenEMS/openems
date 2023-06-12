package io.openems.edge.controller.ess.timeofusetariff.discharge;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public interface ControllerEssTimeOfUseTariffDischarge extends Controller, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/**
		 * Current state of the Time of use tariff discharge controller.
		 */
		STATE_MACHINE(Doc.of(StateMachine.values()) //
				.text("Current state of the Controller")),

		/**
		 * Aggregated seconds when storage is blocked for discharge.
		 */
		DELAYED_TIME(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_SECONDS) //
				.persistencePriority(PersistencePriority.HIGH)), //

		DELAYED(Doc.of(OpenemsType.BOOLEAN)//
				.text("The controller currently blocks discharge")),
		TARGET_HOURS_IS_EMPTY(Doc.of(OpenemsType.BOOLEAN)//
				.text("The list of target hours is empty")),
		QUATERLY_PRICES_TAKEN(Doc.of(OpenemsType.BOOLEAN)//
				.text("The controller retrieves hourly Prices from API successfully")),
		TARGET_HOURS_CALCULATED(Doc.of(OpenemsType.BOOLEAN)//
				.text("The controller calculates target time to buy from grid successfully")),
		TOTAL_CONSUMPTION(Doc.of(OpenemsType.INTEGER) //
				.text("Total consmption for the night")),
		QUARTERLY_PRICES(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.EUROS_PER_MEGAWATT_HOUR) //
				.text("Price of the electricity for the current Hour")),
		REMAINING_CONSUMPTION(Doc.of(OpenemsType.DOUBLE) //
				.text("remaining consmption to charge from grid")),
		TARGET_HOURS(Doc.of(OpenemsType.INTEGER) //
				.text("Number of Target Hours")),
		AVAILABLE_CAPACITY(Doc.of(OpenemsType.INTEGER) //
				.text("Available capcity in the battery during evening")), //
		USABLE_CAPACITY(Doc.of(OpenemsType.INTEGER) //
				.text("Usable capcity in the battery during after taking limit soc into consideration")), //
		PRO_MORE_THAN_CON_ACTUAL(Doc.of(OpenemsType.INTEGER) //
				.text("Actual Hour of Production more than Consumption")),
		PRO_MORE_THAN_CON_SET(Doc.of(OpenemsType.INTEGER) //
				.text("Hour of Production more than Consumption set based on risk level")),
		PRO_LESS_THAN_CON(Doc.of(OpenemsType.INTEGER) //
				.text("Hour of Production less than Consumption")),
		PREDICTED_PRODUCTION(Doc.of(OpenemsType.INTEGER) //
				.text("Predicted Production for the current quarterly hour")),
		PREDICTED_CONSUMPTION(Doc.of(OpenemsType.INTEGER) //
				.text("Predicted Consumption for the current quarterly hour")),
		MIN_SOC(Doc.of(OpenemsType.INTEGER) //
				.text("Minimum SoC to avoid complete discharge")),
		PREDICTED_SOC_WITHOUT_LOGIC(Doc.of(OpenemsType.INTEGER) //
				.text("SoC prediction curve without controller logic")),;

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
	 * Gets the Channel for {@link ChannelId#STATE_MACHINE}.
	 *
	 * @return the Channel
	 */
	public default Channel<StateMachine> getStateMachineChannel() {
		return this.channel(ChannelId.STATE_MACHINE);
	}

	/**
	 * Gets the Status of the Controller. See {@link ChannelId#STATE_MACHINE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default StateMachine getStateMachine() {
		return this.getStateMachineChannel().value().asEnum();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#STATE_MACHINE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setStateMachine(StateMachine value) {
		this.getStateMachineChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#DELAYED}.
	 *
	 * @return the Channel
	 */
	public default Channel<Boolean> getDelayedChannel() {
		return this.channel(ChannelId.DELAYED);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#DELAYED} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDelayed(boolean value) {
		this.getDelayedChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#QUARTERLY_PRICES}.
	 *
	 * @return the Channel
	 */
	public default Channel<Float> getQuarterlyPricesChannel() {
		return this.channel(ChannelId.QUARTERLY_PRICES);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#QUARTERLY_PRICES}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setQuarterlyPrices(Float value) {
		this.getQuarterlyPricesChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#PREDICTED_PRODUCTION}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getPredictedProductionChannel() {
		return this.channel(ChannelId.PREDICTED_PRODUCTION);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#PREDICTED_PRODUCTION} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setPredictedProduction(Integer value) {
		this.getPredictedProductionChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#PREDICTED_CONSUMPTION}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getPredictedConsumptionChannel() {
		return this.channel(ChannelId.PREDICTED_CONSUMPTION);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#PREDICTED_CONSUMPTION} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setPredictedConsumption(Integer value) {
		this.getPredictedConsumptionChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#PREDICTED_SOC_WITHOUT_LOGIC}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getPredictedSocWithoutLogicChannel() {
		return this.channel(ChannelId.PREDICTED_SOC_WITHOUT_LOGIC);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#PREDICTED_SOC_WITHOUT_LOGIC} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setPredictedSocWithoutLogic(Integer value) {
		this.getPredictedSocWithoutLogicChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#TARGET_HOURS_CALCULATED}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getTargetHoursCalculatedChannel() {
		return this.channel(ChannelId.TARGET_HOURS_CALCULATED);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#TARGET_HOURS_CALCULATED} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setTargetHoursCalculated(Boolean value) {
		this.getTargetHoursCalculatedChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#TARGET_HOURS_IS_EMPTY}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getTargetHoursIsEmptyChannel() {
		return this.channel(ChannelId.TARGET_HOURS_IS_EMPTY);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#TARGET_HOURS_IS_EMPTY} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setTargetHoursIsEmpty(Boolean value) {
		this.getTargetHoursIsEmptyChannel().setNextValue(value);
	}
}
