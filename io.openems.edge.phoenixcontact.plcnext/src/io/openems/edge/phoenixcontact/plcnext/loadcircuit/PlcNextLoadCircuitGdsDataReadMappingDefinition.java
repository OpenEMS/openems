package io.openems.edge.phoenixcontact.plcnext.loadcircuit;

import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataMappingDefinition;

public enum PlcNextLoadCircuitGdsDataReadMappingDefinition implements PlcNextGdsDataMappingDefinition {
	MAX_ACTIVE_POWER_EXPORT("MaxPowerExport", PlcNextLoadCircuit.ChannelId.MAX_ACTIVE_POWER_EXPORT), //
	MAX_ACTIVE_POWER_IMPORT("MaxPowerImport", PlcNextLoadCircuit.ChannelId.MAX_ACTIVE_POWER_IMPORT), //
	MAX_REACTIVE_POWER_EXPORT("MaxReactivePower", PlcNextLoadCircuit.ChannelId.MAX_REACTIVE_POWER);

	private final String identifier;
	private final ChannelId channelId;

	private PlcNextLoadCircuitGdsDataReadMappingDefinition(String identifier, ChannelId channelId) {
		this.identifier = identifier;
		this.channelId = channelId;
	}

	@Override
	public String getIdentifier() {
		return this.identifier;
	}

	@Override
	public ChannelId getChannelId() {
		return this.channelId;
	}

}
