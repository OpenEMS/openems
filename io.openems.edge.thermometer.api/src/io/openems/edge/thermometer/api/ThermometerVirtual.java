package io.openems.edge.thermometer.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;

import java.util.Optional;

/**
 * This Nature is an expansion of the Thermometer-Nature.
 * It allows a VirtualThermometer to receive a virtual Temperature, updates it into the Thermometer: Temperature Channel
 * and other components sees the virtual Thermometer as a normal Thermometer.
 */
public interface ThermometerVirtual extends Thermometer {
    enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        VIRTUAL_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE));


        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }

    /**
     * Gets the Virutal Temperature Channel.
     *
     * @return the Channel.
     */
    default WriteChannel<Integer> getVirtualTemperatureChannel() {
        return this.channel(ChannelId.VIRTUAL_TEMPERATURE);
    }

    /**
     * Gets the Virtual Temperature Value as an Optional Integer and reset the value.
     *
     * @return the ChannelValue as Optional Int
     */

    default Optional<Integer> getVirtualTemperature() {
        return this.getVirtualTemperatureChannel().getNextWriteValueAndReset();
    }

    /**
     * Sets the Virutal Temperature. Can be called by other Components.
     *
     * @param virtualTemperature the virtual Temperature that will be applied.
     * @throws OpenemsError.OpenemsNamedException if Channel cannot be found. (Shouldn't occur)
     */
    default void setVirtualTemperature(int virtualTemperature) throws OpenemsError.OpenemsNamedException {
        this.getVirtualTemperatureChannel().setNextWriteValue(virtualTemperature);
    }

}
