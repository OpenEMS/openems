package io.openems.api.channel.thingstate;

public interface WarningEnum {
	int getValue();

	default String getChannelId() {
		return "Warning/"+this.getValue();
	}
}
