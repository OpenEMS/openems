package io.openems.api.channel.thingstate;

public interface FaultEnum extends ThingStateEnum {

	@Override
	default String getChannelId() {
		return "Fault/" + this.getValue();
	}

}
