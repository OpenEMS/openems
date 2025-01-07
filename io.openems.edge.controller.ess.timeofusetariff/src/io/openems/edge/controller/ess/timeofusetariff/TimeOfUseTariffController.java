package io.openems.edge.controller.ess.timeofusetariff;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.ess.timeofusetariff.v1.EnergyScheduleHandlerV1;
import io.openems.edge.energy.api.EnergySchedulable;


public interface TimeOfUseTariffController extends Controller, EnergySchedulable, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Current state of the Time of use tariff controller.
		 */
		STATE_MACHINE(Doc.of(StateMachine.values()) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Current state of the Controller")),

		QUARTERLY_PRICES(Doc.of(OpenemsType.DOUBLE) //
				.unit(Unit.MONEY_PER_MEGAWATT_HOUR) //
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
	 * Get the {@link EnergyScheduleHandlerV1}.
	 * 
	 * @return {@link EnergyScheduleHandlerV1}
	 */
	@Deprecated
	public EnergyScheduleHandlerV1 getEnergyScheduleHandlerV1();

	/**
	 * Gets the Channel for {@link ChannelId#QUARTERLY_PRICES}.
	 *
	 * @return the Channel
	 */
	public default Channel<Double> getQuarterlyPricesChannel() {
		return this.channel(ChannelId.QUARTERLY_PRICES);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#QUARTERLY_PRICES}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setQuarterlyPrices(Double value) {
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
	/**
	 * Used for Modbus/TCP Api Controller. Provides a Modbus table for the Channels
	 * of this Component.
	 *
	 * @param accessMode filters the Modbus-Records that should be shown
	 * @return the {@link ModbusSlaveNatureTable}
	 */
	public default ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
		return ModbusSlaveNatureTable.of(TimeOfUseTariffController.class, accessMode, 100) //
				.channel(0, ChannelId.QUARTERLY_PRICES, ModbusType.FLOAT32) //
				.channel(2, ChannelId.DELAYED_TIME, ModbusType.UINT32) //
				.build();
	}
}
