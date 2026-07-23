package io.openems.edge.batteryinverter.refu;

import io.openems.common.channel.Level;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.batteryinverter.refu.statemachine.StateMachine.State;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;

public interface BatteryInverterRefu extends ManagedSymmetricBatteryInverter, SymmetricBatteryInverter, ModbusComponent,
		OpenemsComponent, StartStoppable {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		STATE_MACHINE(Doc.of(State.values())//
				.text("Current State of State-Machine")), //
		RUN_FAILED(Doc.of(Level.WARNING)//
				.text("Running the Logic failed")), //
		MAX_START_TIMEOUT(Doc.of(Level.WARNING)//
				.text("Max start time is exceeded")), //
		MAX_STOP_TIMEOUT(Doc.of(Level.WARNING)//
				.text("Max stop time is exceeded")), //
		INVERTER_CURRENT_STATE_FAULT(Doc.of(Level.WARNING)//
				.text("The operating state is invalid or unexpected")); //

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
	 * Gets the target Start/Stop mode from config or StartStop-Channel.
	 *
	 * @return {@link StartStop}
	 */
	public StartStop getStartStopTarget();

	/**
	 * Gets the Channel for {@link ChannelId#INVERTER_CURRENT_STATE_FAULT}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getInverterCurrentStateChannel() {
		return this.channel(ChannelId.INVERTER_CURRENT_STATE_FAULT);
	}

	/**
	 * set INVERTER_CURRENT_STATE_FAULT channel.
	 *
	 * @param value the next value
	 */
	public default void setInverterCurrentStateFault(boolean value) {
		this.getInverterCurrentStateChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAX_START_TIMEOUT}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getMaxStartTimeoutChannel() {
		return this.channel(ChannelId.MAX_START_TIMEOUT);
	}

	/**
	 * Gets the {@link StateChannel} value for {@link ChannelId#MAX_START_TIMEOUT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getMaxStartTimeout() {
		return this.getMaxStartTimeoutChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#MAX_START_TIMEOUT}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMaxStartTimeout(boolean value) {
		this.getMaxStartTimeoutChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAX_STOP_TIMEOUT}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getMaxStopTimeoutChannel() {
		return this.channel(ChannelId.MAX_STOP_TIMEOUT);
	}

	/**
	 * Gets the {@link StateChannel} value for {@link ChannelId#MAX_STOP_TIMEOUT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getMaxStopTimeout() {
		return this.getMaxStopTimeoutChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#MAX_STOP_TIMEOUT}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMaxStopTimeout(boolean value) {
		this.getMaxStopTimeoutChannel().setNextValue(value);
	}
}
