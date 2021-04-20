package io.openems.edge.remote.rest.device.simulator;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface RestRemoteTestDevice extends OpenemsComponent {
    enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * Value Read, This Channel is for Read Devices -> GET Request.
         *
         * <ul>
         * <li>Interface: RestRemoteChannel
         * <li>Type: String
         * <li>Will be Set if the RestRemoteDevice is set to Read.
         * </ul>
         */
        VALUE_READ(Doc.of(OpenemsType.STRING)),
        /**
         * Value Write; This Channel is for Write Devices -> POST Request.
         *
         * <ul>
         * <li>Interface: RestRemoteChannel
         * <li>Type: String
         * <li>Will be Set if the RestRemoteDevice is set to Write
         * </ul>
         */
        VALUE_WRITE(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_WRITE));
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
     * Get The ReadValueChannel.
     *
     * @return the Channel.
     */
    default Channel<String> getReadValueChannel() {
        return this.channel(ChannelId.VALUE_READ);
    }

    /**
     * Get the WriteValueChannel.
     *
     * @return the Channel.
     */
    default WriteChannel<String> getWriteValueChannel() {
        return this.channel(ChannelId.VALUE_WRITE);
    }

}
