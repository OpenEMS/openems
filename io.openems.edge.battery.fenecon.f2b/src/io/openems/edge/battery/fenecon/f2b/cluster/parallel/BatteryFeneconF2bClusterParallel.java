package io.openems.edge.battery.fenecon.f2b.cluster.parallel;

import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.Level;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.fenecon.f2b.BatteryFeneconF2b;
import io.openems.edge.battery.fenecon.f2b.cluster.common.BatteryFeneconF2bCluster;
import io.openems.edge.battery.fenecon.f2b.cluster.common.statemachine.StateMachine.State;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStoppable;

public interface BatteryFeneconF2bClusterParallel
		extends BatteryFeneconF2bCluster, BatteryFeneconF2b, Battery, OpenemsComponent, EventHandler, StartStoppable {
	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		STATE_MACHINE(Doc.of(State.values()) //
				.text("Current State of State-Machine")), //
		RUN_FAILED(Doc.of(Level.FAULT) //
				.text("Running the Logic failed")), //
		VOLTAGE_DIFFERENCE_HIGH(Doc.of(Level.WARNING) //
				.text("Voltage difference between cluster strings higher than 4V!")), //
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
	 * Gets the Channel for {@link ChannelId#STATE_MACHINE}.
	 *
	 * @return the Channel
	 */
	public default Channel<State> getStateMachineChannel() {
		return this.channel(ChannelId.STATE_MACHINE);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#STATE_MACHINE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setStateMachine(State value) {
		this.getStateMachineChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#RUN_FAILED}.
	 *
	 * @return the Channel
	 */
	public default Channel<Boolean> getRunFailedChannel() {
		return this.channel(ChannelId.RUN_FAILED);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#RUN_FAILED}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setRunFailed(boolean value) {
		this.getRunFailedChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#VOLTAGE_DIFFERENCE_HIGH}.
	 *
	 * @return the Channel
	 */
	public default Channel<Boolean> getVoltageDifferenceHighChannel() {
		return this.channel(ChannelId.VOLTAGE_DIFFERENCE_HIGH);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#VOLTAGE_DIFFERENCE_HIGH} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setVoltageDifferenceHigh(boolean value) {
		this.getVoltageDifferenceHighChannel().setNextValue(value);
	}

	/**
	 * Gets InternalVoltageBatteryMap is a {@link Map} collection that contains
	 * {@link Value#Integer} as key and {@link BatteryFeneconF2b} as value.
	 * 
	 * @return A map where the keys are {@link Value#Integer} and values are
	 *         {@link BatteryFeneconF2b}
	 */
	public Map<Value<Integer>, BatteryFeneconF2b> getInternalVoltageBatteryMap();

	/**
	 * Gets the minimum {@link BatteryFeneconF2b.ChannelId#INTERNAL_VOLTAGE} of all
	 * batteries.
	 * 
	 * @return the minimum {@link BatteryFeneconF2b.ChannelId#INTERNAL_VOLTAGE}
	 */
	public OptionalInt getMinInternalVoltage();

	/**
	 * Gets the maximum {@link BatteryFeneconF2b.ChannelId#INTERNAL_VOLTAGE} of all
	 * batteries.
	 * 
	 * @return the maximum {@link BatteryFeneconF2b.ChannelId#INTERNAL_VOLTAGE}
	 */
	public OptionalInt getMaxInternalVoltage();

	/**
	 * Gets the batteries which are allowed to start according to the difference of
	 * minimum and maximum {@link Battery.ChannelId#VOLTAGE}s.
	 * 
	 * @return The {@link Battery Batteries} that can start
	 */
	public List<BatteryFeneconF2b> getStartableBatteries();

	/**
	 * Gets the batteries which are 'not' allowed to start according to the
	 * difference of minimum and maximum {@link Battery.ChannelId#VOLTAGE}s.
	 * 
	 * @return The {@link Battery Batteries} that can 'not' start
	 */
	public List<BatteryFeneconF2b> getNotStartableBatteries();

	/**
	 * Stops the {@link Battery Batteries} that can 'not' start.
	 */
	public void stopNotStartableBatteries();

	/**
	 * Starts the {@link Battery Batteries} that can start.
	 */
	public void startStartableBatteries();

	/**
	 * True on all start-able batteries are started, false on one of the battery not
	 * started.
	 * 
	 * @return boolean true if all started
	 */
	public boolean areStartableBatteriesStarted();
}
