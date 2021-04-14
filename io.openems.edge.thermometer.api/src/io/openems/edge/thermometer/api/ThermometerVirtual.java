package io.openems.edge.thermometer.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;

import java.util.Optional;

public interface ThermometerVirtual extends Thermometer {
    enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        VIRTUAL_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE));


        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }

    default WriteChannel<Integer> getVirtualTemperatureChannel() {
        return this.channel(ChannelId.VIRTUAL_TEMPERATURE);
    }

    default Optional<Integer> getVirtualTemperature() {
        return this.getVirtualTemperatureChannel().getNextWriteValueAndReset();
    }

    default void setVirtualTemperature(int virtualTemperature) throws OpenemsError.OpenemsNamedException {
        this.getVirtualTemperatureChannel().setNextWriteValue(virtualTemperature);
    }

}
