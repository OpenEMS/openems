package io.openems.api.channel.thingstate;

public interface FaultEnum {
	int getValue();

	default String getChannelId() {
		return "Fault/"+this.getValue();
	}
}
