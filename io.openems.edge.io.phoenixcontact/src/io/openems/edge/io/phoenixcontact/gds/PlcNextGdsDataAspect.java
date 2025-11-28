package io.openems.edge.io.phoenixcontact.gds;

import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.meter.api.ElectricityMeter;

public enum PlcNextGdsDataAspect {
	READ_TEST_VALUE("read_test_value", PlcNextAspectType.READ, ElectricityMeter.ChannelId.ACTIVE_POWER);
	
	// WRITE_TEST_VALUE("wride_test_value", PlcNextAspectType.WRITE,
	// PlcNextGdsDataAspect.WRITE_TEST_VALUE);
	
	private final String identifier;
	private final PlcNextAspectType type;
	private final ChannelId channelId;
	
	private PlcNextGdsDataAspect(String identifier, PlcNextAspectType type, ChannelId channelId) {
		this.identifier = identifier;
		this.type = type;
		this.channelId = channelId;
	}
	
	public String getIdentifier() {
		return identifier;
	}

	public PlcNextAspectType getType() {
		return type;
	}

	public ChannelId getChannelId() {
		return channelId;
	}
}
