package io.openems.edge.battery.fenecon.f2b.cluster.parallel;

import io.openems.common.channel.Level;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.fenecon.f2b.cluster.common.statemachine.StateMachine.State;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStoppable;

public interface BatteryFeneconF2bClusterParallel extends Battery, OpenemsComponent, StartStoppable {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		STATE_MACHINE(Doc.of(State.values()) //
				.text("Current State of State-Machine")), //
		RUN_FAILED(Doc.of(Level.FAULT) //
				.text("Running the Logic failed")), //
		VOLTAGE_DIFFERENCE_HIGH(Doc.of(Level.FAULT) //
				.text("Cluster voltage difference too high!")), //
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
	public default Channel<StateChannel> getVoltageDifferenceHighChannel() {
		return this.channel(ChannelId.VOLTAGE_DIFFERENCE_HIGH);
	}

	/**
	 * Gets {@link ChannelId#VOLTAGE_DIFFERENCE_HIGH} Value.
	 * 
	 * @return the Channel
	 */
	public default StateChannel getVoltageDifferenceHigh() {
		return this.getVoltageDifferenceHighChannel().value().asEnum();
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
}
