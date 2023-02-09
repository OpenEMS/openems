package io.openems.edge.heater.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;

/**
 * A generalized nature for fetching information about a heatpump.
 */
@ProviderType
public interface Heatpump extends Heater {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

	/**
	 * Electric consumption power used by the Heatpump at the moment.
	 *
	 * <ul>
	 * <li>Interface: Heatpump
	 * <li>Type: Integer
	 * <li>Unit: Watt
	 * </ul>
	 */
	ELECTRIC_CONSUMPTION_POWER(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.WATT) //
		.persistencePriority(PersistencePriority.HIGH) //
		.accessMode(AccessMode.READ_ONLY)), //

	/**
	 * Max electric power the CHP can consume.
	 *
	 * <ul>
	 * <li>Interface: Heatpump
	 * <li>Type: Integer
	 * <li>Unit: Watt
	 * </ul>
	 */
	MAX_ELECTRIC_CONSUMPTION_POWER(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.WATT) //
		.persistencePriority(PersistencePriority.LOW) //
		.accessMode(AccessMode.READ_ONLY)), //

	/**
	 * Min electric power the CHP needs to run.
	 *
	 * <ul>
	 * <li>Interface: Heatpump
	 * <li>Type: Integer
	 * <li>Unit: Watt
	 * </ul>
	 */
	MIN_ELECTRIC_CONSUMPTION_POWER(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.WATT) //
		.persistencePriority(PersistencePriority.LOW) //
		.accessMode(AccessMode.READ_ONLY)) //

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
     * Gets the Channel for {@link ChannelId#ELECTRIC_CONSUMPTION_POWER}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getElectricConsumptionPowerChannel() {
	return this.channel(ChannelId.ELECTRIC_CONSUMPTION_POWER);
    }

    /**
     * Gets the electric consumption power in W. See
     * {@link ChannelId#ELECTRIC_CONSUMPTION_POWER}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getElectricConsumptionPower() {
	return this.getElectricConsumptionPowerChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on
     * {@link ChannelId#ELECTRIC_CONSUMPTION_POWER} Channel.
     *
     * @param value the next value
     */
    default void _setElectricConsumptionPower(Integer value) {
	this.getElectricConsumptionPowerChannel().setNextValue(value);
    }

    /**
     * Internal method to set the 'nextValue' on
     * {@link ChannelId#ELECTRIC_CONSUMPTION_POWER} Channel.
     *
     * @param value the next value
     */
    default void _setElectricConsumptionPower(int value) {
	this.getElectricConsumptionPowerChannel().setNextValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#MAX_ELECTRIC_CONSUMPTION_POWER}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getMaxElectricConsumptionPowerChannel() {
	return this.channel(ChannelId.MAX_ELECTRIC_CONSUMPTION_POWER);
    }

    /**
     * Gets the electric consumption power in W. See
     * {@link ChannelId#MAX_ELECTRIC_CONSUMPTION_POWER}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getMaxElectricConsumptionPower() {
	return this.getMaxElectricConsumptionPowerChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on
     * {@link ChannelId#MAX_ELECTRIC_CONSUMPTION_POWER} Channel.
     *
     * @param value the next value
     */
    default void _setMaxElectricConsumptionPower(Integer value) {
	this.getMaxElectricConsumptionPowerChannel().setNextValue(value);
    }

    /**
     * Internal method to set the 'nextValue' on
     * {@link ChannelId#MAX_ELECTRIC_CONSUMPTION_POWER} Channel.
     *
     * @param value the next value
     */
    default void _setMaxElectricConsumptionPower(int value) {
	this.getMaxElectricConsumptionPowerChannel().setNextValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#MIN_ELECTRIC_CONSUMPTION_POWER}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getMinElectricConsumptionPowerChannel() {
	return this.channel(ChannelId.MIN_ELECTRIC_CONSUMPTION_POWER);
    }

    /**
     * Gets the electric consumption power in W. See
     * {@link ChannelId#MIN_ELECTRIC_CONSUMPTION_POWER}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getMinElectricConsumptionPower() {
	return this.getMinElectricConsumptionPowerChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on
     * {@link ChannelId#MIN_ELECTRIC_CONSUMPTION_POWER} Channel.
     *
     * @param value the next value
     */
    default void _setMinElectricConsumptionPower(Integer value) {
	this.getMinElectricConsumptionPowerChannel().setNextValue(value);
    }

    /**
     * Internal method to set the 'nextValue' on
     * {@link ChannelId#MIN_ELECTRIC_CONSUMPTION_POWER} Channel.
     *
     * @param value the next value
     */
    default void _setMinElectricConsumptionPower(int value) {
	this.getMinElectricConsumptionPowerChannel().setNextValue(value);
    }

}
