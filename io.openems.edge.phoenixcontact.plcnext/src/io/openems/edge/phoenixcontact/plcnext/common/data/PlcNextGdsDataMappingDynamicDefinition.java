package io.openems.edge.phoenixcontact.plcnext.common.data;

import io.openems.edge.common.channel.ChannelId;

/**
 * Generic mapping definition
 */
public record PlcNextGdsDataMappingDynamicDefinition(String varIdentifier, ChannelId channelId)
		implements PlcNextGdsDataMappingDefinition {

	@Override
	public ChannelId getChannelId() {
		return channelId;
	}

	@Override
	public String getIdentifier() {
		return varIdentifier;
	}

}
