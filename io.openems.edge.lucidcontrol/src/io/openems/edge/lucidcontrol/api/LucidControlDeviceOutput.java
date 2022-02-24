package io.openems.edge.lucidcontrol.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.DoubleWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * The Nature for a LucidControlOutput. IRL it's attached to a LucidControlModule and you can write a PercentValue
 * Depending on the max Voltage the Output will be adapted.
 */
public interface LucidControlDeviceOutput extends OpenemsComponent {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * The Percentage Channel. This is used to setup a Output for a LucidControlDevice
         * <ul>
         *     <li>Interface: LucidControlDeviceOutput
         *     <li>Unit: Percent
         * </ul>
         */
        PERCENTAGE(Doc.of(OpenemsType.DOUBLE).unit(Unit.PERCENT).accessMode(AccessMode.READ_WRITE).onInit(
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

    /**
     * Get the Percent Channel of this Device.
     *
     * @return the Channel.
     */
    default Channel<Double> getPercentageChannel() {
        return this.channel(ChannelId.PERCENTAGE);
    }

    /**
     * Get the Value wrapper of the Percentage Channel.
     *
     * @return the ValueWrapper.
     */
    default Value<Double> getPercentageValue() {
        return this.getPercentageChannel().value();
    }

}
