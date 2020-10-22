package io.openems.edge.ess.generic.symmetric;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.generic.symmetric.statemachine.StateMachine.State;

public interface GenericManagedSymmetricEss extends ManagedSymmetricEss, StartStoppable, ModbusSlave  {

	/**
	 * Retry set-command after x Seconds, e.g. for starting battery or
	 * battery-inverter.
	 */
	public static int RETRY_COMMAND_SECONDS = 30;

	/**
	 * Retry x attempts for set-command.
	 */
	public static int RETRY_COMMAND_MAX_ATTEMPTS = 30;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		STATE_MACHINE(Doc.of(State.values()) //
				.text("Current State of State-Machine")), //
		RUN_FAILED(Doc.of(Level.FAULT) //
				.text("Running the Logic failed")), //
		MAX_BATTERY_START_ATTEMPTS_FAULT(Doc.of(Level.FAULT) //
				.text("The maximum number of Battery start attempts failed")), //
		MAX_BATTERY_STOP_ATTEMPTS_FAULT(Doc.of(Level.FAULT) //
				.text("The maximum number of Battery stop attempts failed")), //
		MAX_BATTERY_INVERTER_START_ATTEMPTS_FAULT(Doc.of(Level.FAULT) //
				.text("The maximum number of Battery-Inverter start attempts failed")), //
		MAX_BATTERY_INVERTER_STOP_ATTEMPTS_FAULT(Doc.of(Level.FAULT) //
				.text("The maximum number of Battery-Inverter stop attempts failed")); //

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}
	
	@Override
	public default ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable( //
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricEss.getModbusSlaveNatureTable(accessMode), //
				StartStoppable.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(GenericManagedSymmetricEss.class, accessMode, 100) //
						.channel(0, GenericManagedSymmetricEss.ChannelId.STATE_MACHINE, ModbusType.UINT16) //
						.build());

	}

	/**
	 * Gets the target Start/Stop mode from config or StartStop-Channel.
	 * 
	 * @return {@link StartStop}
	 */
	public StartStop getStartStopTarget();

	/**
	 * Gets the Channel for {@link ChannelId#MAX_BATTERY_START_ATTEMPTS_FAULT}.
	 * 
	 * @return the Channel
	 */
	public default StateChannel getMaxBatteryStartAttemptsFaultChannel() {
		return this.channel(ChannelId.MAX_BATTERY_START_ATTEMPTS_FAULT);
	}

	/**
	 * Gets the {@link StateChannel} for
	 * {@link ChannelId#MAX_BATTERY_START_ATTEMPTS_FAULT}.
	 * 
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getMaxBatteryStartAttemptsFault() {
		return this.getMaxBatteryStartAttemptsFaultChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#MAX_BATTERY_START_ATTEMPTS_FAULT} Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setMaxBatteryStartAttemptsFault(boolean value) {
		this.getMaxBatteryStartAttemptsFaultChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAX_BATTERY_STOP_ATTEMPTS_FAULT}.
	 * 
	 * @return the Channel
	 */
	public default StateChannel getMaxBatteryStopAttemptsFaultChannel() {
		return this.channel(ChannelId.MAX_BATTERY_STOP_ATTEMPTS_FAULT);
	}

	/**
	 * Gets the {@link StateChannel} for
	 * {@link ChannelId#MAX_BATTERY_STOP_ATTEMPTS_FAULT}.
	 * 
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getMaxBatteryStopAttemptsFault() {
		return this.getMaxBatteryStopAttemptsFaultChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#MAX_BATTERY_STOP_ATTEMPTS_FAULT} Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setMaxBatteryStopAttemptsFault(boolean value) {
		this.getMaxBatteryStopAttemptsFaultChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for
	 * {@link ChannelId#MAX_BATTERY_INVERTER_START_ATTEMPTS_FAULT}.
	 * 
	 * @return the Channel
	 */
	public default StateChannel getMaxBatteryInverterStartAttemptsFaultChannel() {
		return this.channel(ChannelId.MAX_BATTERY_INVERTER_START_ATTEMPTS_FAULT);
	}

	/**
	 * Gets the {@link StateChannel} for
	 * {@link ChannelId#MAX_BATTERY_INVERTER_START_ATTEMPTS_FAULT}.
	 * 
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getMaxBatteryInverterStartAttemptsFault() {
		return this.getMaxBatteryInverterStartAttemptsFaultChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#MAX_BATTERY_INVERTER_START_ATTEMPTS_FAULT} Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setMaxBatteryInverterStartAttemptsFault(boolean value) {
		this.getMaxBatteryInverterStartAttemptsFaultChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for
	 * {@link ChannelId#MAX_BATTERY_INVERTER_STOP_ATTEMPTS_FAULT}.
	 * 
	 * @return the Channel
	 */
	public default StateChannel getMaxBatteryInverterStopAttemptsFaultChannel() {
		return this.channel(ChannelId.MAX_BATTERY_INVERTER_STOP_ATTEMPTS_FAULT);
	}

	/**
	 * Gets the {@link StateChannel} for
	 * {@link ChannelId#MAX_BATTERY_INVERTER_STOP_ATTEMPTS_FAULT}.
	 * 
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getMaxBatteryInverterStopAttemptsFault() {
		return this.getMaxBatteryInverterStopAttemptsFaultChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#MAX_BATTERY_INVERTER_STOP_ATTEMPTS_FAULT} Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setMaxBatteryInverterStopAttemptsFault(boolean value) {
		this.getMaxBatteryInverterStopAttemptsFaultChannel().setNextValue(value);
	}

}
