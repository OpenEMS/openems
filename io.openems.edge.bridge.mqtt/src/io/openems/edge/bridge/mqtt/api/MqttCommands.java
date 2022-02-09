package io.openems.edge.bridge.mqtt.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StringWriteChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * This Nature allows the CommandComponent to write the CommandValues into corresponding channel.
 * Any Device that should react to commands, can implement this nature and react if any channel has anything written into.
 * Note: If you want more commands, simply expand the Enum {@link MqttCommandType} and integrate the Channel here and tell the
 * CommandComponent to write the Value into the corresponding channel.
 * An OpenEMS Component should check the Channel, and react to those values.
 * If you want to map certain values to OpenEMS Channel, use the telemetry component and subscribe to a payload.
 * And map the Keys from the payload to certain channel.
 */
public interface MqttCommands extends OpenemsComponent {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * Set Temperature Command Channel.
         * Can set Temperature of e.g. HeatSystemComponents etc.
         * <ul>
         *     <li>Interface: MqttCommands
         *     <li>Type: String
         *     <li> See MqttCommandTypes for available commands.
         * </ul>
         */
        SET_TEMPERATURE(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> ((StringWriteChannel) channel).onSetNextWrite(channel::setNextValue))),
        /**
         * Set the Schedule of a Component.
         * <ul>
         *     <li>Interface: MqttCommands
         *     <li>Type: String
         *     <li> See MqttCommandTypes for available commands.
         * </ul>
         */
        SET_SCHEDULE(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> ((StringWriteChannel) channel).onSetNextWrite(channel::setNextValue))),
        /**
         * Set the Performance of a Component.
         * <ul>
         *     <li>Interface: MqttCommands
         *     <li>Type: String
         *     <li> See MqttCommandTypes for available commands.
         * </ul>
         */
        SET_PERFORMANCE(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> ((StringWriteChannel) channel).onSetNextWrite(channel::setNextValue))),
        /**
         * Set the Power of a Component.
         * <ul>
         *     <li>Interface: MqttCommands
         *     <li>Type: String
         *     <li> See MqttCommandTypes for available commands.
         * </ul>
         */
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

    /**
     * Get the SetTemperature Command Channel.
     *
     * @return the channel.
     */
    default WriteChannel<String> getSetTemperature() {
        return this.channel(ChannelId.SET_TEMPERATURE);
    }

    /**
     * Get the Set Schedule Command Channel.
     *
     * @return the Channel.
     */
    default WriteChannel<String> getSetSchedule() {
        return this.channel(ChannelId.SET_SCHEDULE);
    }

    /**
     * Get the SetPerformance Command Channel.
     *
     * @return the channel.
     */
    default WriteChannel<String> getSetPerformance() {
        return this.channel(ChannelId.SET_PERFORMANCE);
    }

    /**
     * Get the SetPerformance Command Channel.
     *
     * @return the Channel.
     */
    default WriteChannel<String> getSetPower() {
        return this.channel(ChannelId.SET_POWER);
    }

}
