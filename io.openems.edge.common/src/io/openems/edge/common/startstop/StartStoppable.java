package io.openems.edge.common.startstop;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusType;

/**
 * Declares an OpenEMS Component as being able to get started and stopped.
 *
 * <p>
 * A device or service inside OpenEMS that implements this OpenEMS Nature can be
 * started or stopped.
 *
 * <p>
 * Implementing this Nature also requires the Component to have a configuration
 * property "startStop" of type {@link StartStopConfig} that overrides the logic
 * of the {@link StartStoppable#setStartStop(StartStop)} method:
 *
 * <pre>
 * 	&#64;AttributeDefinition(name = "Start/stop behaviour?", description = "Should this Component be forced to start or stop?")
 *	StartStopConfig startStop() default StartStopConfig.AUTO;
 * </pre>
 *
 * <ul>
 * <li>if config is {@link StartStopConfig#START} -> always start
 * <li>if config is {@link StartStopConfig#STOP} -> always stop
 * <li>if config is {@link StartStopConfig#AUTO} -> start
 * {@link StartStop#UNDEFINED} and wait for a call to
 * {@link #setStartStop(StartStop)}
 * </ul>
 */
public interface StartStoppable extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Start/Stop.
		 *
		 * <ul>
		 * <li>Interface: StartStoppable
		 * <li>Type: {@link StartStop}
		 * <li>Range: 0=Undefined, 1=Start, 2=Stop
		 * </ul>
		 */
		START_STOP(Doc.of(StartStop.values()));

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
	 * Used for Modbus/TCP Api Controller. Provides a Modbus table for the Channels
	 * of this Component.
	 *
	 * @param accessMode filters the Modbus-Records that should be shown
	 * @return the {@link ModbusSlaveNatureTable}
	 */
	public static ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
		return ModbusSlaveNatureTable.of(StartStoppable.class, accessMode, 10) //
				.channel(0, ChannelId.START_STOP, ModbusType.UINT16) //
				.build();
	}

	/**
	 * Gets the Channel for {@link ChannelId#START_STOP}.
	 *
	 * @return the Channel
	 */
	public default Channel<StartStop> getStartStopChannel() {
		return this.channel(ChannelId.START_STOP);
	}

	/**
	 * Gets the current {@link StartStop} state of the {@link StartStoppable}
	 * Component. See {@link ChannelId#START_STOP}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default StartStop getStartStop() {
		return this.getStartStopChannel().value().asEnum();
	}

	/**
	 * Is this device or service started?.
	 *
	 * <ul>
	 * <li>true - if (and only if) {@link ChannelId#START_STOP} is
	 * {@link StartStop#START}
	 * <li>false - if {@link ChannelId#START_STOP} is {@link StartStop#STOP} or
	 * {@link StartStop#UNDEFINED}
	 * </ul>
	 *
	 * @return true if started
	 */
	public default boolean isStarted() {
		return this.getStartStop() == StartStop.START;
	}

	/**
	 * Is this device or service stopped?.
	 *
	 * <ul>
	 * <li>true - if (and only if) {@link ChannelId#START_STOP} is
	 * {@link StartStop#STOP}
	 * <li>false - if {@link ChannelId#START_STOP} is {@link StartStop#START} or
	 * {@link StartStop#UNDEFINED}
	 * </ul>
	 *
	 * @return true if stopped
	 */
	public default boolean isStopped() {
		return this.getStartStop() == StartStop.STOP;
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#START_STOP}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setStartStop(StartStop value) {
		this.getStartStopChannel().setNextValue(value);
	}

	/**
	 * Starts or stops the device or service represented by this OpenEMS Component.
	 *
	 * @param value target {@link StartStop} state
	 * @throws OpenemsNamedException on error
	 */
	public void setStartStop(StartStop value) throws OpenemsNamedException;

	/**
	 * Starts the device or service represented by this OpenEMS Component.
	 *
	 * <p>
	 * This calls {@link #setStartStop(StartStop)} with {@link StartStop#START}.
	 *
	 * @throws OpenemsNamedException on error
	 */
	public default void start() throws OpenemsNamedException {
		this.setStartStop(StartStop.START);
	}

	/**
	 * Stops the device or service represented by this OpenEMS Component.
	 *
	 * <p>
	 * This calls {@link #setStartStop(StartStop)} with {@link StartStop#STOP}.
	 *
	 * @throws OpenemsNamedException on error
	 */
	public default void stop() throws OpenemsNamedException {
		this.setStartStop(StartStop.STOP);
	}
}
