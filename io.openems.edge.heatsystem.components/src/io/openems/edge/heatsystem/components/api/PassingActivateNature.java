package io.openems.edge.heatsystem.components.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface PassingActivateNature extends OpenemsComponent {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        /**
         * ActivateOrNot.
         * <ul>
         * <li>Interface: ControllerPassingChannel
         * <li>Type: boolean
         * <li>Unit: ON_OFF
         * </ul>
         */

        ON_OFF(Doc.of(OpenemsType.BOOLEAN).unit(Unit.ON_OFF).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> ((BooleanWriteChannel) channel).onSetNextWrite(channel::setNextValue)
        )); //

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
     * Activate/Deactivate the Passing Station.
     *
     * @return the Channel
     */
    default WriteChannel<Boolean> getOnOff() {
        return this.channel(ChannelId.ON_OFF);
    }
}