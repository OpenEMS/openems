package io.openems.edge.phoenixcontact.plcnext.pvinverter;

import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataMappingDefinition;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

public enum PlcNextPvInverterGdsDataWriteMappingDefinition implements PlcNextGdsDataMappingDefinition {
	SET_ACTIVE_POWER(ManagedSymmetricPvInverter.ChannelId.MAX_ACTIVE_POWER, "setPower.ActivePower"),
	SET_APPARENT_POWER(ManagedSymmetricPvInverter.ChannelId.MAX_APPARENT_POWER,
			"setPower.ApparentPower"),
	SET_REACTIVE_POWER(ManagedSymmetricPvInverter.ChannelId.MAX_REACTIVE_POWER,
			"setPower.ReactivePower");

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
