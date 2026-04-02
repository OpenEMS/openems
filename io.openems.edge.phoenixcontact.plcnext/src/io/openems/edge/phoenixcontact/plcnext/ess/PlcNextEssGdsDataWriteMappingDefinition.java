package io.openems.edge.phoenixcontact.plcnext.ess;

import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataMappingDefinition;

public enum PlcNextEssGdsDataWriteMappingDefinition implements PlcNextGdsDataMappingDefinition {
	SET_ACTIVE_POWER_EQUALS(ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS, "setPower.SetActivePowerEquals"),
	SET_ACTIVE_POWER_EQUALS_WITH_PID(ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS_WITH_PID,
			"setPower.SetActivePowerEqualsWithPid"),
	SET_REACTIVE_POWER_EQUALS(ManagedSymmetricEss.ChannelId.SET_REACTIVE_POWER_EQUALS,
			"setPower.SetReactivePowerEquals"),
	SET_ACTIVE_POWER_LESS_OR_EQUALS(ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_LESS_OR_EQUALS,
			"setPower.SetActivePowerLessOrEquals"),
	SET_ACTIVE_POWER_GREATER_OR_EQUALS(ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_GREATER_OR_EQUALS,
			"setPower.SetActivePowerGreaterOrEquals"),
	SET_REACTIVE_POWER_LESS_OR_EQUALS(ManagedSymmetricEss.ChannelId.SET_REACTIVE_POWER_LESS_OR_EQUALS,
			"setPower.SetReactivePowerLessOrEquals"),
	SET_REACTIVE_POWER_GREATER_OR_EQUALS(ManagedSymmetricEss.ChannelId.SET_REACTIVE_POWER_GREATER_OR_EQUALS,
			"setPower.SetReactivePowerGreaterOrEquals");

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
