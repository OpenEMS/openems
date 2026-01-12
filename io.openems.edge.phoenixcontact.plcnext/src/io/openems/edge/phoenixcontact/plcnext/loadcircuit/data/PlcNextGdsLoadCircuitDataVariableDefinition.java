package io.openems.edge.phoenixcontact.plcnext.loadcircuit.data;

import java.util.List;

import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataVariableDefinition;
import io.openems.edge.phoenixcontact.plcnext.loadcircuit.PlcNextLoadCircuit;

public enum PlcNextGdsLoadCircuitDataVariableDefinition  implements PlcNextGdsDataVariableDefinition {
	MAX_ACTIVE_POWER_EXPORT("maxPower.MaxPowerExport", List.of(PlcNextLoadCircuit.ChannelId.MAX_ACTIVE_POWER_EXPORT)), //
	MAX_ACTIVE_POWER_IMPORT("maxPower.MaxPowerImport", List.of(PlcNextLoadCircuit.ChannelId.MAX_ACTIVE_POWER_IMPORT)), //
	MAX_REACTIVE_POWER_EXPORT("setPower.ReactivePower", List.of(PlcNextLoadCircuit.ChannelId.MAX_REACTIVE_POWER));

	private final String identifier;
	private final List<ChannelId> openEmsChannelIds;

	private PlcNextGdsLoadCircuitDataVariableDefinition(String identifier, List<ChannelId> openEmsChannelIds) {
		this.identifier = identifier;
		this.openEmsChannelIds = openEmsChannelIds;
	}

	@Override
	public String getIdentifier() {
		return this.identifier;
	}

	@Override
	public List<ChannelId> getOpenEmsChannelIds() {
		return this.openEmsChannelIds;
	}

}
