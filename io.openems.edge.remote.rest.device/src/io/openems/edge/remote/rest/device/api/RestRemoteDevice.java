package io.openems.edge.remote.rest.device.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StringWriteChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * This Nature allows RestRemoteDevice to Work.
 * The ValueRead channel is for Read Remote Devices. If you work with Remote OpenEMS applications and you need a Channel.
 * Configure the Channel in Config and read the Value from ValueRead.
 * Same goes for WriteChannel. If you want to Write remotely to one Channel. Use the WriteChannel to write your values into this one.+
 * If you want  to interrupt the WriteChannel. Just write false into allow Request.
 */
public interface RestRemoteDevice extends OpenemsComponent {

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
        VALUE_WRITE(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_WRITE).onInit(channel ->
                ((StringWriteChannel) channel).onSetNextWrite(channel::setNextValue))),
        /**
         * WhatTypeSet: Is the Device a Read or Write / Get and Post request.
         *
         * <ul>
         * <li>Interface: RestRemoteChannel
         * <li>Type: String
         * <li>States if the RemoteDevice is For Read or Write.
         * </ul>
         */
        WHAT_TYPE_SET(Doc.of(OpenemsType.STRING)),
        /**
         * Allow Request. Should the request be handled.
         * <ul>
         *     <li>Interface: RestRemoteChannel
         *     <li>Type: String
         *     <li> Set this Channel to true/false if the Component should be handled.
         *
         * </ul>
         */
        ALLOW_REQUEST(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE).onInit(channel ->
                ((BooleanWriteChannel) channel).onSetNextWrite(channel::setNextValue)
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

    /**
     * Get the TypeChannel (Read or Write).
     *
     * @return the channel.
     */
    default Channel<String> getTypeSetChannel() {
        return this.channel(ChannelId.WHAT_TYPE_SET);
    }

    /**
     * get the AllowRequestChannel.
     *
     * @return the channel.
     */
    default WriteChannel<Boolean> getAllowRequestChannel() {
        return this.channel(ChannelId.ALLOW_REQUEST);
    }

    /**
     * Sets the Value of the Device if it's type is Write /allows to write.
     *
     * @param value the Value that the Remote Device will be set to and therefore the Remote Device too.
     */
    void setValue(String value);

    /**
     * Returns the Value as a String. Depending on the Write/Read Type.
     *
     * @return the ValueString.
     */

    String getValue();

    /**
     * Get the Unique Id.
     *
     * @return the Id.
     */

    String getId();

    /**
     * Check if this Device is a Write Remote Device.
     *
     * @return a boolean.
     */
    boolean isWrite();

    /**
     * Checks if this Device is a Read Remote Device.
     *
     * @return a boolean.
     */
    boolean isRead();

    /**
     * Checks/Asks if the Connection via Rest is ok.
     *
     * @return a boolean.
     */
    boolean connectionOk();
}
