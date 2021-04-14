package io.openems.edge.rest.remote.device.general.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface RestRemoteChannel extends OpenemsComponent {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * Value Read.
         *
         * <ul>
         * <li>Interface: RestRemoteChannel
         * <li>Type: String
         * <li>Will be Set and Get if the RestRemoteDevice is set to Read
         * </ul>
         */
        VALUE_READ(Doc.of(OpenemsType.STRING)),
        /**
         * Value Write.
         *
         * <ul>
         * <li>Interface: RestRemoteChannel
         * <li>Type: String
         * <li>Will be Set and Get if the RestRemoteDevice is set to Write
         * </ul>
         */
        VALUE_WRITE(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_WRITE)),
        /**
         * WhatTypeSet.
         *
         * <ul>
         * <li>Interface: RestRemoteChannel
         * <li>Type: String
         * <li>States if the RemoteDevice is For Read or Write.
         * </ul>
         */
        WHAT_TYPE_SET(Doc.of(OpenemsType.STRING)),
        ALLOW_REQUEST(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE).onInit(channel -> {
            ((BooleanWriteChannel) channel).onSetNextWrite(channel::setNextValue);
        })),
        UNIT(Doc.of(OpenemsType.STRING)),
        IS_INVERSE(Doc.of(OpenemsType.BOOLEAN));


        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }

    default Channel<String> getReadValue() {
        return this.channel(ChannelId.VALUE_READ);
    }

    default WriteChannel<String> getWriteValue() {
        return this.channel(ChannelId.VALUE_WRITE);
    }

    default Channel<String> getTypeSet() {
        return this.channel(ChannelId.WHAT_TYPE_SET);
    }

    default WriteChannel<Boolean> getAllowRequest() {
        return this.channel(ChannelId.ALLOW_REQUEST);
    }

    default Channel<Boolean> getIsInverse() {
        return this.channel(ChannelId.IS_INVERSE);
    }

    default Channel<String> getUnit() {
        return this.channel(ChannelId.UNIT);
    }
}