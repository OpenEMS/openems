package io.openems.edge.phoenixcontact.plcnext.ess;

import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataMappingDefinition;

public enum PlcNextEssGdsDataWriteMappingDefinition implements PlcNextGdsDataMappingDefinition {
	SET_ACTIVE_POWER_EQUALS(ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS, "SetActivePowerEquals"),
	SET_REACTIVE_POWER_EQUALS(ManagedSymmetricEss.ChannelId.SET_REACTIVE_POWER_EQUALS,
			"SetReactivePowerEquals"),
	SET_ACTIVE_POWER_LESS_OR_EQUALS(ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_LESS_OR_EQUALS,
			"SetActivePowerLessOrEquals"),
	SET_ACTIVE_POWER_GREATER_OR_EQUALS(ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_GREATER_OR_EQUALS,
			"SetActivePowerGreaterOrEquals"),
	SET_REACTIVE_POWER_LESS_OR_EQUALS(ManagedSymmetricEss.ChannelId.SET_REACTIVE_POWER_LESS_OR_EQUALS,
			"SetReactivePowerLessOrEquals"),
	SET_REACTIVE_POWER_GREATER_OR_EQUALS(ManagedSymmetricEss.ChannelId.SET_REACTIVE_POWER_GREATER_OR_EQUALS,
			"SetReactivePowerGreaterOrEquals");

	private final ChannelId channelId;
	private final String identifier;

	private PlcNextEssGdsDataWriteMappingDefinition(ChannelId channelId, String identifier) {
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
