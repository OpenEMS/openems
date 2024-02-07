package io.openems.edge.system.fenecon.industrial.s;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.system.fenecon.industrial.s.coolingunit.statemachine.CoolingUnitStateMachine.CoolingUnitState;
import io.openems.edge.system.fenecon.industrial.s.statemachine.StateMachine.State;

public interface SystemFeneconIndustrialS extends OpenemsComponent, EventHandler, StartStoppable {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		STATE_MACHINE(Doc.of(State.values()) //
				.text("Current State of State-Machine")), //
		RUN_FAILED(Doc.of(Level.FAULT) //
				.text("Running the Logic failed")), //
		COOLING_UNIT_ERROR_STATE(Doc.of(Level.WARNING) //
				.text("Cooling unit has an error!")), //
		EMERGENCY_STOP_STATE(Doc.of(Level.WARNING) //
				.text("Emergency Stop State is active")), //
		SPD_TRIPPED(Doc.of(Level.WARNING) //
				.text("SPD Tripped: Possible SPD damage")), //
		FUSE_TRIPPED(Doc.of(Level.WARNING)//
				.text("Fuse Tripped: At least one 24V fuse is off")), //
		COOLING_UNIT_STATE_MACHINE(Doc.of(CoolingUnitState.values()) //
				.text("Current State of Cooling-Unit-State-Machine")), //
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
	 * Gets the Channel for {@link ChannelId#COOLING_UNIT_STATE_MACHINE}.
	 *
	 * @return the Channel
	 */
	public default Channel<CoolingUnitState> getCoolingUnitStateMachineChannel() {
		return this.channel(ChannelId.COOLING_UNIT_STATE_MACHINE);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#COOLING_UNIT_STATE_MACHINE} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setCoolingUnitStateMachine(CoolingUnitState value) {
		this.getCoolingUnitStateMachineChannel().setNextValue(value);
	}
}
