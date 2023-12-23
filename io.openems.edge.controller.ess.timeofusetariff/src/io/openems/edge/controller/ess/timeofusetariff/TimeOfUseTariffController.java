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

	public static final int PERIODS_PER_HOUR = 4;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Current state of the Time of use tariff controller.
		 */
		STATE_MACHINE(Doc.of(StateMachine.values()) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Current state of the Controller")),

		QUARTERLY_PRICES(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.EUROS_PER_MEGAWATT_HOUR) //
				.text("Price of the electricity for the current Hour")//
				.persistencePriority(PersistencePriority.HIGH)), //

		/**
		 * Aggregated seconds when storage is being force charged from the grid.
		 */
		CHARGED_TIME(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_SECONDS) //
				.persistencePriority(PersistencePriority.HIGH)), //

		/**
		 * Aggregated seconds when storage is blocked for discharge.
		 */
		DELAYED_TIME(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_SECONDS) //
				.persistencePriority(PersistencePriority.HIGH)); //

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
	 * Gets the {@link ChannelId#STATE_MACHINE}.
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
}
