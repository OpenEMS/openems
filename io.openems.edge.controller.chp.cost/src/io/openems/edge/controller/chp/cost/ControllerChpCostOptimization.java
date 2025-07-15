package io.openems.edge.controller.chp.cost;

import static io.openems.common.channel.PersistencePriority.HIGH;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;



public interface ControllerChpCostOptimization extends Controller, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		STATE_MACHINE(Doc.of(State.values()) //
				.text("Current State of State-Machine").persistencePriority(HIGH)), //
		
		ENERGY_COSTS(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)),		

		AWAITING_TRANSITION_HYSTERESIS(Doc.of(Level.INFO) //
				.text("Would change State, but hysteresis is active")),

		AWAITING_START_HYSTERESIS(Doc.of(Level.INFO) //
				.text("Would stop chp, but hysteresis is active")),	
		
		AWAITING_STOP_HYSTERESIS(Doc.of(Level.INFO) //
				.text("Would start chp, but hysteresis is active")),				
		
		ACTIVE_POWER_TARGET(new IntegerDoc() //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)), //		
		
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
	 * Gets the Channel for {@link ChannelId#AWAITING_HYSTERESIS}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getAwaitingTransistionHysteresisChannel() {
		return this.channel(ChannelId.AWAITING_TRANSITION_HYSTERESIS);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#AWAITING_HYSTERESIS} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setAwaitingTransitionHysteresis(boolean value) {
		this.getAwaitingTransistionHysteresisChannel().setNextValue(value);
	}	
	
	//
	/**
	 * Gets the Channel for {@link ChannelId#AWAITING_START_HYSTERESIS}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getAwaitingStartHysteresisChannel() {
		return this.channel(ChannelId.AWAITING_START_HYSTERESIS);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#AWAITING_START_HYSTERESIS} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setAwaitingStartHysteresis(boolean value) {
		this.getAwaitingStartHysteresisChannel().setNextValue(value);
	}	
	
	
	//
	/**
	 * Gets the Channel for {@link ChannelId#AWAITING_STOP_HYSTERESIS}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getAwaitingStopHysteresisChannel() {
		return this.channel(ChannelId.AWAITING_STOP_HYSTERESIS);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#AWAITING_STOP_HYSTERESIS} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setAwaitingStopHysteresis(boolean value) {
		this.getAwaitingStopHysteresisChannel().setNextValue(value);
	}		

	
	//
	/**
	 * Gets the Channel for {@link ChannelId#AWAITING_STOP_HYSTERESIS}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getEnergyCostsChannel() {
		return this.channel(ChannelId.ENERGY_COSTS);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#AWAITING_STOP_HYSTERESIS} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setEnergyCosts(Integer value) {
		this.getEnergyCostsChannel().setNextValue(value);
	}		
	
	public default Value<Integer> getEnergyCosts() {
		return this.getEnergyCostsChannel().value();
	}	
	
	
	//
	public default IntegerReadChannel getActivePowerTargetChannel() {
		return this.channel(ChannelId.ACTIVE_POWER_TARGET);
	}

	public default Value<Integer> getActivePowerTarget() {
		return this.getActivePowerTargetChannel().value();
	}

	public default void _setActivePowerTarget(Integer value) {
		this.getActivePowerTargetChannel().setNextValue(value);
	}


	public default void _setActivePowerTarget(int value) {
		this.getActivePowerTargetChannel().setNextValue(value);
	}
	
	//
	/**
	 * Gets the Channel for {@link ChannelId#STATE_MACHINE}.
	 *
	 * @return the Channel
	 */
	public default Channel<State> getStateMachineChannel() {
		return this.channel(ChannelId.STATE_MACHINE);
	}

	/**
	 * Gets current state of the {@link StateMachine}. See
	 * {@link ChannelId#STATE_MACHINE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default State getStateMachine() {
		return this.getStateMachineChannel().value().asEnum();
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
	
}
