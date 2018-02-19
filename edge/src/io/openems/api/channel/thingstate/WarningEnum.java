package io.openems.api.channel.thingstate;

public interface WarningEnum extends ThingStateEnum {

	@Override
	default String getChannelId() {
		return "Warning/" + this.getValue();
	}

}
