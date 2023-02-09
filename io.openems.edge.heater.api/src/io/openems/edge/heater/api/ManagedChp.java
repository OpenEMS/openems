package io.openems.edge.heater.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;

/**
 * A generalized nature for a managed combined heat and power generator (chp).
 */
@ProviderType
public interface ManagedChp extends Chp, ManagedHeaterByOperationMode {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

	/**
	 * Set point for the generated electrical power.
	 *
	 * <ul>
	 * <li>Interface: Chp
	 * <li>Type: Integer
	 * <li>Unit: Watt
	 * </ul>
	 */
	ELECTRIC_PRODUCTION_POWER_REQUEST(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.WATT) //
		.persistencePriority(PersistencePriority.HIGH) //
		.accessMode(AccessMode.READ_WRITE)), //

	;

	private final Doc doc;

	private ChannelId(Doc doc) {
	    this.doc = doc;
	}

	public Doc doc() {
	    return this.doc;
	}

    }

    /**
     * Gets the Channel for {@link ChannelId#EFFECTIVE_ELECTRIC_POWER}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getElectricProductionPowerRequestChannel() {
	return this.channel(ChannelId.ELECTRIC_PRODUCTION_POWER_REQUEST);
    }

    /**
     * Gets the Write Channel for {@link ChannelId#EFFECTIVE_ELECTRIC_POWER}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getSetElectricProductionPowerRequestChannel() {
	return this.channel(ChannelId.ELECTRIC_PRODUCTION_POWER_REQUEST);
    }

    /**
     * Get the currently generated electric power of the chp in kilowatt. Value
     * contains a double. See {@link ChannelId#EFFECTIVE_ELECTRIC_POWER}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getElectricProductionPowerRequest() {
	return this.getElectricProductionPowerRequestChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on
     * {@link ChannelId#EFFECTIVE_ELECTRIC_POWER} Channel.
     *
     * @param value the next value
     */
    public default void _setElectricProductionPowerRequest(Integer value) {
	this.getElectricProductionPowerRequestChannel().setNextValue(value);
    }

    /**
     * Internal method to set the 'nextValue' on
     * {@link ChannelId#EFFECTIVE_ELECTRIC_POWER} Channel.
     *
     * @param value the next value
     */
    public default void _setElectricProductionPowerRequest(int value) {
	this.getElectricProductionPowerRequestChannel().setNextValue(value);
    }

    /**
     * Internal method to set the 'nextWriteValue' on
     * {@link ChannelId#EFFECTIVE_ELECTRIC_POWER} Channel.
     *
     * @param value the next value
     */
    public default void setSetElecticProductionPowerRequest(int value) throws OpenemsNamedException {
	this.getSetElectricProductionPowerRequestChannel().setNextWriteValue(value);
    }
}
