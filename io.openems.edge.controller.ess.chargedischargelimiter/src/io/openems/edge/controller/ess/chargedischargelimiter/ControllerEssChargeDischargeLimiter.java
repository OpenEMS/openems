package io.openems.edge.controller.ess.chargedischargelimiter;

import static io.openems.common.channel.PersistencePriority.HIGH;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public interface ControllerEssChargeDischargeLimiter extends Controller, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		STATE_MACHINE(Doc.of(State.values()) //
				.text("Current State of State-Machine").persistencePriority(HIGH)), //

		AWAITING_HYSTERESIS(Doc.of(Level.INFO) //
				.text("Would change State, but hysteresis is active")),
		/**
		 * Holds the minimum SoC value configured.
		 */
		FORCE_CHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).persistencePriority(HIGH).accessMode(AccessMode.READ_WRITE)), // ), // Priority high
		BALANCING_REMAINING_SECONDS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS).persistencePriority(HIGH)), //
		CHARGED_ENERGY(Doc.of(OpenemsType.INTEGER) // change name
				.unit(Unit.WATT_HOURS).persistencePriority(HIGH)),
		BALANCING_SOC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT).persistencePriority(HIGH)), //
		MIN_SOC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT).persistencePriority(HIGH)), //
		MAX_SOC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT).persistencePriority(HIGH)); //

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
	public default StateChannel getAwaitingHysteresisChannel() {
		return this.channel(ChannelId.AWAITING_HYSTERESIS);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#AWAITING_HYSTERESIS} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setAwaitingHysteresisValue(boolean value) {
		this.getAwaitingHysteresisChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CHARGED_ENERGY}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getChargedEnergyChannel() {
		return this.channel(ChannelId.CHARGED_ENERGY);
	}

	/**
	 * Gets the Channel for {@link ChannelId#FORCE_CHARGE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getForceChargePowerChannel() {
		return this.channel(ChannelId.FORCE_CHARGE_POWER);
	}

	/**
	 * Gets the Channel for {@link ChannelId#FORCE_CHARGE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getBalancingSocChannel() {
		return this.channel(ChannelId.BALANCING_SOC);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MIN_SOC}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMinSocChannel() {
		return this.channel(ChannelId.MIN_SOC);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#MIN_SOC} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMinSoc(Integer value) {
		this.getMinSocChannel().setNextValue(value);
	}

	/**
	 * Gets the minimum SoC value configured. See {@link ChannelId#MIN_SOC}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getMinSoc() {
		return this.getMinSocChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAX_SOC}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMaxSocChannel() {
		return this.channel(ChannelId.MAX_SOC);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#MAX_SOC} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMaxSoc(Integer value) {
		this.getMaxSocChannel().setNextValue(value);
	}

	/**
	 * Gets the maximum SoC value configured. See {@link ChannelId#MAX_SOC}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getMaxSoc() {
		return this.getMaxSocChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#CHARGED_ENERGY}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setChargedEnergy(Integer value) {
		this.getChargedEnergyChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#CHARGED_ENERGY}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setChargedEnergy(int value) {
		this.getChargedEnergyChannel().setNextValue(value);
	}

	/**
	 * ToDo {@link ChannelId#CHARGED_ENERGY}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getChargedEnergy() {
		return this.getChargedEnergyChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#FORCE_CHARGE_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setForceChargePower(Integer value) {
		this.getForceChargePowerChannel().setNextValue(value);
	}

	/**
	 * Gets the maximum SoC value configured. See
	 * {@link ChannelId#FORCE_CHARGE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getForceChargePower() {
		return this.getForceChargePowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#BALANCING_SOC}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setBalancingSoc(Integer value) {
		this.getBalancingSocChannel().setNextValue(value);
	}

	/**
	 * Gets the maximum SoC value configured. See {@link ChannelId#BALANCING_SOC}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBalancingSoc() {
		return this.getBalancingSocChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#BALANCING_REMAINING_SECONDS}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getBalancingRemainingSecondsChannel() {
		return this.channel(ChannelId.BALANCING_REMAINING_SECONDS);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#BALANCING_REMAINING_SECONDS} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setBalancingRemainingSeconds(Integer value) {
		this.getBalancingRemainingSecondsChannel().setNextValue(value);
	}

	/**
	 * Gets the SoC value if Reserve SoC is enabled and returns null otherwise. See
	 * {@link ChannelId#BALANCING_REMAINING_SECONDS}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBalancingRemainingSeconds() {
		return this.getBalancingRemainingSecondsChannel().value();
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

	public String getEssId();

}
