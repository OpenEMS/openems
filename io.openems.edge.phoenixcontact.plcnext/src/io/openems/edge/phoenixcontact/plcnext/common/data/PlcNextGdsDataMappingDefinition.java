package io.openems.edge.phoenixcontact.plcnext.common.data;

import io.openems.edge.common.channel.ChannelId;

/**
 * Defines an OpenEMS channelId to PLCnext variable identifier mapping, to write values from
 * OpenEMS channels to PLCnext REST-API.
 */
public interface PlcNextGdsDataMappingDefinition {

    /**
     * Returns the ID of the OpenEMS channel .
     *
     * @return  mapped channel ID
     */
    ChannelId getChannelId();

    /**
     * Returns the identifier/key to access value in PLCnext value object.
     *
     * @return mapped PLCnext identifier/key to access value
     */
    String getIdentifier();

}