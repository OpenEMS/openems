package io.openems.edge.phoenixcontact.plcnext.common.data;

/**
 * Defines an OpenEMS channelId to PLCnext variable identifier mapping, to write values from 
 * OpenEMS channels to PLCnext REST-API
 */
import io.openems.edge.common.channel.ChannelId;

public interface PlcNextGdsDataMappingDefinition {

	ChannelId getChannelId();

	String getIdentifier();

}