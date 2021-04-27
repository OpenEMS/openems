package io.openems.edge.lucidcontrol.device.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.DoubleWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public interface LucidControlDeviceOutput extends OpenemsComponent {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        PERCENTAGE(Doc.of(OpenemsType.DOUBLE).unit(Unit.BAR).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> ((DoubleWriteChannel) channel).onSetNextWrite(channel::setNextValue)
        ));

        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }

    }

    default Channel<Double> getPercentageChannel() {
        return this.channel(ChannelId.PERCENTAGE);
    }

    default Value<Double> getPercentageValue() {
        return this.getPercentageChannel().value();
    }

}
