package io.openems.edge.controller.ess.timeofusetariff;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public interface TimeOfUseTariffController extends Controller, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Current state of the Time of use tariff controller.
		 */
		STATE_MACHINE(Doc.of(StateMachine.values()) //
				.persistencePriority(PersistencePriority.VERY_HIGH) //
				.text("Current state of the Controller")),

		/**
		 * Aggregated seconds when storage is being force charged.
		 */
		CHARGED_TIME(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_SECONDS) //
				.persistencePriority(PersistencePriority.HIGH)), //
		CHARGED(Doc.of(OpenemsType.BOOLEAN)//
				.text("The controller currently recharge the battery")),
		CHARGE_VALUE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)//
				.text("The amount of energy set to charge from grid in a period")), //

		/**
		 * Aggregated seconds when storage is blocked for discharge.
		 */
		DELAYED_TIME(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_SECONDS) //
				.persistencePriority(PersistencePriority.HIGH)), //
		DELAYED(Doc.of(OpenemsType.BOOLEAN)//
				.text("The controller currently blocks discharge")),
		DISCHARGE_VALUE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)//
				.text("The amount of energy set to discharge from the battery in a period")), //

		CHARGE_DISCHARGE_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.text("Charge/Discharge energy calculated for the period.")), //
		GRID_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.text("Grid energy calculated for the period.")), //

		AVAILABLE_CAPACITY(Doc.of(OpenemsType.INTEGER) //
				.text("Available capcity in the battery during evening")), //
		USABLE_CAPACITY(Doc.of(OpenemsType.INTEGER) //
				.text("Usable capcity in the battery during after taking limit soc into consideration")), //
		PREDICTED_PRODUCTION(Doc.of(OpenemsType.INTEGER) //
				.text("Predicted Production for the current quarterly hour")),
		PREDICTED_CONSUMPTION(Doc.of(OpenemsType.INTEGER) //
				.text("Predicted Consumption for the current quarterly hour")),
		MIN_SOC(Doc.of(OpenemsType.INTEGER) //
				.text("Minimum SoC to avoid complete discharge")), //
		QUARTERLY_PRICES(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.EUROS_PER_MEGAWATT_HOUR) //
				.text("Price of the electricity for the current Hour")
				.persistencePriority(PersistencePriority.VERY_HIGH)), //

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
	 * Gets the Channel for {@link ChannelId#CHARGED}.
	 *
	 * @return the Channel
	 */
	public default Channel<Boolean> getChargedChannel() {
		return this.channel(ChannelId.CHARGED);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#CHARGED} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setCharged(boolean value) {
		this.getChargedChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CHARGE_VALUE}.
	 *
	 * @return the Channel
	 */
	public default Channel<Boolean> getChargeValueChannel() {
		return this.channel(ChannelId.CHARGE_VALUE);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#CHARGE_VALUE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setChargeValue(Integer value) {
		this.getChargeValueChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#DISCHARGE_VALUE}.
	 *
	 * @return the Channel
	 */
	public default Channel<Boolean> getDischargeValueChannel() {
		return this.channel(ChannelId.DISCHARGE_VALUE);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#DISCHARGE_VALUE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDischargeValue(Integer value) {
		this.getDischargeValueChannel().setNextValue(value);
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
	 * Gets the Channel for {@link ChannelId#CHARGE_DISCHARGE_ENERGY}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getChargeDischargeEnergyChannel() {
		return this.channel(ChannelId.CHARGE_DISCHARGE_ENERGY);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#CHARGE_DISCHARGE_ENERGY} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setChargeDischargeEnergyChannel(Integer value) {
		this.getChargeDischargeEnergyChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#GRID_ENERGY}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getGridEnergyChannel() {
		return this.channel(ChannelId.GRID_ENERGY);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#GRID_ENERGY}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setGridEnergyChannel(Integer value) {
		this.getGridEnergyChannel().setNextValue(value);
	}

}
