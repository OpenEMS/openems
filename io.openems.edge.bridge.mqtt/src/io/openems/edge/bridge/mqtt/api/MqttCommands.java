package io.openems.edge.bridge.mqtt.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StringWriteChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * This Nature needs to be implemented by Components, that want to react to MQTT Commands.
 */

public interface MqttCommands extends OpenemsComponent {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * EXAMPLE COMMANDS
         */
        SET_TEMPERATURE(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> ((StringWriteChannel) channel).onSetNextWrite(channel::setNextValue))),
        SET_SCHEDULE(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> ((StringWriteChannel) channel).onSetNextWrite(channel::setNextValue))),
        SET_PERFORMANCE(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> ((StringWriteChannel) channel).onSetNextWrite(channel::setNextValue))),
        SET_POWER(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_WRITE).onInit(
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

    default WriteChannel<String> getSetTemperatureChannel() {
        return this.channel(ChannelId.SET_TEMPERATURE);
    }

    default WriteChannel<String> getSetScheduleChannel() {
        return this.channel(ChannelId.SET_SCHEDULE);
    }

    default WriteChannel<String> getSetPerformanceChannel() {
        return this.channel(ChannelId.SET_PERFORMANCE);
    }

    default WriteChannel<String> getSetPowerChannel() {
        return this.channel(ChannelId.SET_POWER);
    }

}
