package io.openems.edge.phoenixcontact.plcnext.pvinverter;

import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataMappingDefinition;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

public enum PlcNextPvInverterGdsDataWriteMappingDefinition implements PlcNextGdsDataMappingDefinition {
	SET_ACTIVE_POWER(ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, "setPower.ActivePower");

	private final ChannelId channelId;
	private final String identifier;

	private PlcNextPvInverterGdsDataWriteMappingDefinition(ChannelId channelId, String identifier) {
		this.channelId = channelId;
		this.identifier = identifier;
	}

	@Override
	public ChannelId getChannelId() {
		return this.channelId;
	}

	@Override
	public String getIdentifier() {
		return this.identifier;
	}
}
