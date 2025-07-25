package io.openems.edge.controller.chp.costoptimization;

import static io.openems.common.channel.PersistencePriority.HIGH;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.DoubleReadChannel;
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
		
		ENERGY_COSTS(Doc.of(OpenemsType.DOUBLE) //
				.accessMode(AccessMode.READ_WRITE)
				.persistencePriority(PersistencePriority.HIGH)),
		
		ENERGY_COSTS_WITHOUT_CHP(Doc.of(OpenemsType.DOUBLE) //
				.accessMode(AccessMode.READ_WRITE)
				.persistencePriority(PersistencePriority.HIGH)),		

		AWAITING_TRANSITION_HYSTERESIS(Doc.of(Level.INFO) //
				.text("Would change State, but hysteresis is active").persistencePriority(PersistencePriority.MEDIUM)),
		
		AWAITING_DEVICE_HYSTERESIS(Doc.of(Level.INFO) //
				.text("Would change value but device is still busy").persistencePriority(PersistencePriority.MEDIUM)),		

		AWAITING_START_HYSTERESIS(Doc.of(Level.INFO) //
				.text("Would stop chp, but hysteresis is active").persistencePriority(PersistencePriority.MEDIUM)),	
		
		AWAITING_STOP_HYSTERESIS(Doc.of(Level.INFO) //
				.text("Would start chp, but hysteresis is active")
				.persistencePriority(PersistencePriority.HIGH).persistencePriority(PersistencePriority.MEDIUM)), 				
		
		ACTIVE_POWER_TARGET(new IntegerDoc() //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)), //
		
		CHP_ACTIVE_POWER(new IntegerDoc() //
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
	
	/**
	 * Gets the Channel for {@link ChannelId#AWAITING_HYSTERESIS}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getAwaitingDeviceHysteresisChannel() {
		return this.channel(ChannelId.AWAITING_DEVICE_HYSTERESIS);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#AWAITING_HYSTERESIS} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setAwaitingDeviceHysteresis(boolean value) {
		this.getAwaitingDeviceHysteresisChannel().setNextValue(value);
	}		
	
	public default Value<Boolean> getAwaitingDeviceHysteresis() {
		return this.getAwaitingDeviceHysteresisChannel().value();
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
	public default DoubleReadChannel getEnergyCostsWithoutChpChannel() {
		return this.channel(ChannelId.ENERGY_COSTS_WITHOUT_CHP);
	}

	public default void _setEnergyCostsWithoutChp(Double value) {
		this.getEnergyCostsWithoutChpChannel().setNextValue(value);
	}		
	
	public default Value<Double> getEnergyCostsWithoutChp() {
		return this.getEnergyCostsWithoutChpChannel().value();
	}	
	
	
	//
	public default DoubleReadChannel getEnergyCostsChannel() {
		return this.channel(ChannelId.ENERGY_COSTS);
	}

	public default void _setEnergyCosts(Double value) {
		this.getEnergyCostsChannel().setNextValue(value);
	}		
	
	public default Value<Double> getEnergyCosts() {
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
	//
	public default IntegerReadChannel getChpActivePowerChannel() {
		return this.channel(ChannelId.CHP_ACTIVE_POWER);
	}

	public default Value<Integer> getChpActivePower() {
		return this.getChpActivePowerChannel().value();
	}

	public default void _setChpActivePower(Integer value) {
		this.getChpActivePowerChannel().setNextValue(value);
	}


	public default void _setChpActivePower(int value) {
		this.getChpActivePowerChannel().setNextValue(value);
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
