package io.openems.edge.heater.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import org.osgi.annotation.versioning.ProviderType;

/**
 * A generalized nature for a heater. Provides channels that should be available
 * on all heaters, allowing a vendor agnostic implementation. Vendor specific
 * interfaces should extend this interface.
 */
@ProviderType
public interface Heater extends OpenemsComponent {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

	/**
	 * Temperature value of the outgoing water flow.
	 *
	 * <ul>
	 * <li>Interface: Heater
	 * <li>Type: Integer
	 * <li>Unit: Decidegree Celsius
	 * </ul>
	 */
	FLOW_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.DEZIDEGREE_CELSIUS) //
		.persistencePriority(PersistencePriority.MEDIUM) //
		.accessMode(AccessMode.READ_ONLY)), //

	/**
	 * Temperature value of the water return flow.
	 *
	 * <ul>
	 * <li>Interface: Heater
	 * <li>Type: Integer
	 * <li>Unit: Decidegree Celsius
	 * </ul>
	 */
	RETURN_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.DEZIDEGREE_CELSIUS) //
		.persistencePriority(PersistencePriority.MEDIUM) //
		.accessMode(AccessMode.READ_ONLY)), //

	/**
	 * Output Heating power.
	 *
	 * <ul>
	 * <li>Interface: Heater
	 * <li>Type: Integer
	 * <li>Unit: Watt
	 * </ul>
	 */
	HEATING_POWER(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.WATT) //
		.persistencePriority(PersistencePriority.MEDIUM) //
		.accessMode(AccessMode.READ_ONLY)), //

	/**
	 * Output Heating energy.
	 *
	 * <ul>
	 * <li>Interface: Heater
	 * <li>Type: Integer
	 * <li>Unit: KilowattHours
	 * </ul>
	 */
	HEATING_ENERGY(Doc.of(OpenemsType.LONG) //
		.unit(Unit.KILOWATT) //
		.persistencePriority(PersistencePriority.MEDIUM) //
		.accessMode(AccessMode.READ_ONLY)), //

	/**
	 * Possible states of the heater.
	 *
	 * <ul>
	 * <li>Interface: Heater, @see {HeaterState}
	 * <li>Type: Integer
	 * <li>Possible values: -1 ... 4
	 * <li>State -1: UNDEFINED - Undefined
	 * <li>State 0: BLOCKED_OR_ERROR - Heater operation is blocked by something
	 * <li>State 1: OFF - Off
	 * <li>State 2: STANDBY - Standby, waiting for commands
	 * <li>State 3: STARTING_UP_OR_PREHEAT - Command to heat received, preparing to
	 * start heating
	 * <li>State 4: RUNNING - Heater is heating
	 * </ul>
	 */
	HEATER_STATE(Doc.of(HeaterState.values()) //
		.persistencePriority(PersistencePriority.HIGH) //
		.accessMode(AccessMode.READ_ONLY)), //

	/**
	 * Failed state channel for a failed communication to the Heater.
	 *
	 * <ul>
	 * <li>Interface: Heater
	 * <li>Readable
	 * <li>Level: FAULT
	 * </ul>
	 */
	COMMUNICATION_FAILED(Doc.of(Level.FAULT) //
		.accessMode(AccessMode.READ_ONLY) //
		.persistencePriority(PersistencePriority.HIGH)), //

	/**
	 * all kinds of Heater errors.
	 *
	 * <ul>
	 * <li>Interface: Heater
	 * <li>Readable
	 * <li>Level: FAULT
	 * </ul>
	 */
	HEATER_ERROR(Doc.of(Level.FAULT) //
		.accessMode(AccessMode.READ_ONLY) //
		.persistencePriority(PersistencePriority.HIGH)) //

	;

	private final Doc doc;

	ChannelId(Doc doc) {
	    this.doc = doc;
	}

	public Doc doc() {
	    return this.doc;
	}

    }

    /**
     * Gets the Channel for {@link ChannelId#FLOW_TEMPERATURE}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getFlowTemperatureChannel() {
	return this.channel(ChannelId.FLOW_TEMPERATURE);
    }

    /**
     * Gets the temperature value of the outgoing hot water in dezidegree Celsius.
     * See {@link ChannelId#FLOW_TEMPERATURE}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getFlowTemperature() {
	return this.getFlowTemperatureChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#FLOW_TEMPERATURE}
     * Channel.
     *
     * @param value the next value
     */
    default void _setFlowTemperature(Integer value) {
	this.getFlowTemperatureChannel().setNextValue(value);
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#FLOW_TEMPERATURE}
     * Channel.
     *
     * @param value the next value
     */
    default void _setFlowTemperature(int value) {
	this.getFlowTemperatureChannel().setNextValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#RETURN_TEMPERATURE}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getReturnTemperatureChannel() {
	return this.channel(ChannelId.RETURN_TEMPERATURE);
    }

    /**
     * Get the return temperature in dezidegree Celsius. See
     * {@link ChannelId#RETURN_TEMPERATURE}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getReturnTemperature() {
	return this.getReturnTemperatureChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on
     * {@link ChannelId#RETURN_TEMPERATURE} Channel.
     *
     * @param value the next value
     */
    default void _setReturnTemperature(Integer value) {
	this.getReturnTemperatureChannel().setNextValue(value);
    }

    /**
     * Internal method to set the 'nextValue' on
     * {@link ChannelId#RETURN_TEMPERATURE} Channel.
     *
     * @param value the next value
     */
    default void _setReturnTemperature(int value) {
	this.getReturnTemperatureChannel().setNextValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#HEATING_POWER}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getHeatingPowerChannel() {
	return this.channel(ChannelId.HEATING_POWER);
    }

    /**
     * Get the heating power in W. See {@link ChannelId#HEATING_POWER}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getHeatingPower() {
	return this.getHeatingPowerChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#HEATING_POWER}
     * Channel.
     *
     * @param value the next value
     */
    default void _setHeatingPower(Integer value) {
	this.getHeatingPowerChannel().setNextValue(value);
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#HEATING_POWER}
     * Channel.
     *
     * @param value the next value
     */
    default void _setHeatingPower(int value) {
	this.getHeatingPowerChannel().setNextValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#HEATING_ENERGY}.
     *
     * @return the Channel
     */
    default LongReadChannel getHeatingEnergyChannel() {
	return this.channel(ChannelId.HEATING_ENERGY);
    }

    /**
     * Gets the value of the electric production energy in Wh. See
     * {@link ChannelId#HEATING_ENERGY}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Long> getHeatingEnergy() {
	return this.getHeatingEnergyChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#HEATING_ENERGY}
     * Channel.
     *
     * @param value the next value
     */
    default void _setHeatingEnergy(Long value) {
	this.getHeatingEnergyChannel().setNextValue(value);
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#HEATING_ENERGY}
     * Channel.
     *
     * @param value the next value
     */
    default void _setHeatingEnergy(long value) {
	this.getHeatingEnergyChannel().setNextValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#HEATER_STATE}.
     *
     * @return the Channel
     */
    default Channel<HeaterState> getHeaterStateChannel() {
	return this.channel(ChannelId.HEATER_STATE);
    }

    /**
     * Gets the state of the heater. Use ’getHeaterState().asEnum()’ to get the
     * enum.
     *
     * <ul>
     * <li>Type: Integer
     * <li>Possible values: -1 ... 4
     * <li>State -1: UNDEFINED - Undefined
     * <li>State 0: BLOCKED_OR_ERROR - Heater operation is blocked by something
     * <li>State 1: OFF - Off
     * <li>State 2: STANDBY - Standby, waiting for commands
     * <li>State 3: STARTING_UP_OR_PREHEAT - Command to heat received, preparing to
     * start heating
     * <li>State 4: RUNNING - Heater is heating
     * </ul>
     * See {@link ChannelId#HEATER_STATE}.
     *
     * @return the Channel {@link Value}
     */
    default HeaterState getHeaterState() {
	return this.getHeaterStateChannel().value().asEnum();
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#HEATER_STATE}
     * Channel.
     *
     * @param state the next value
     */
    default void _setHeaterState(HeaterState state) {
	if (state != null) {
	    this.getHeaterStateChannel().setNextValue(state.getValue());
	}
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#HEATER_STATE}
     * Channel.
     *
     * @param value the next value
     */
    public default void _setHeaterState(int value) {
	this.getHeaterStateChannel().setNextValue(value);
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#HEATER_STATE}
     * Channel.
     *
     * @param value the next value
     */
    default void _setHeaterState(Integer value) {
	this.getHeaterStateChannel().setNextValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#COMMUNICATION_FAILED}.
     *
     * @return the Channel
     */
    public default StateChannel getCommunicationFailedChannel() {
	return this.channel(ChannelId.COMMUNICATION_FAILED);
    }

    /**
     * Gets the Failed state channel for a failed communication. See
     * {@link ChannelId#COMMUNICATION_FAILED}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Boolean> getCommunicationFailed() {
	return this.getCommunicationFailedChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on
     * {@link ChannelId#COMMUNICATION_FAILED} Channel.
     *
     * @param value the next value
     */
    public default void _setCommunicationFailed(boolean value) {
	this.getCommunicationFailedChannel().setNextValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#HEATER_ERROR}.
     *
     * @return the Channel
     */
    public default StateChannel getHeaterErrorChannel() {
	return this.channel(ChannelId.HEATER_ERROR);
    }

    /**
     * Gets the Heater Error state. See {@link ChannelId#HEATER_ERROR}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Boolean> getHeaterError() {
	return this.getHeaterErrorChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#HEATER_ERROR}
     * Channel.
     *
     * @param value the next value
     */
    public default void _setHeaterError(boolean value) {
	this.getHeaterErrorChannel().setNextValue(value);
    }

}
