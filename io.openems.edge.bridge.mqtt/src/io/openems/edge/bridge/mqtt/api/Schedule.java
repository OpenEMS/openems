package io.openems.edge.bridge.mqtt.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StringWriteChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * This will be an extra Nature for Components containing a Schedule controlled by the MQTT Cloud -->
 * When to run certain Power
 */
public interface Schedule extends OpenemsComponent {
    enum ChannelId implements io.openems.edge.common.channel.ChannelId {


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
    default WriteChannel<String> getScheduleChannel(){
        return this.channel(ChannelId.SCHEDULE);
    }
}
