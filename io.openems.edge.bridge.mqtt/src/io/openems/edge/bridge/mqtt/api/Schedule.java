package io.openems.edge.bridge.mqtt.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StringWriteChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * This interface provides the Nature for a Schedule.
 * This Nature is for a more complex schedule of Components (Not just a simple method: SetSchedule value: foo expiration: bar Schedule)
 * The Schedule needs to be extracted manually.
 * Here a complex Schedule can be stored and validated by another component.
 */
public interface Schedule extends OpenemsComponent {
    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        /**
         * The Schedule Channel. It contains a schedule for a Component when to do something with a certain value.
         * R.g. a Chp runs at 12:15 pm with a flow temperature of 450dC and at 1:00 pm 750 dC etc etc.
         * The Extraction of the Schedule needs an extra Controller.
         * <ul>
         * <li> Interface: Schedule
         * <li> Type: String
         * </ul>
         */
        SCHEDULE(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> ((StringWriteChannel) channel).onSetNextWrite(channel::setNextValue)));


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
     * Get The Schedule Channel.
     *
     * @return the channel.
     */
    default WriteChannel<String> getSchedule() {
        return this.channel(ChannelId.SCHEDULE);
    }
}
