package io.openems.impl.controller.balancing;

import io.openems.api.channel.IsRequired;
import io.openems.api.channel.WriteableChannel;
import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.api.device.nature.Meter;

@IsThingMap(type = Meter.class)
public class MeterMap extends ThingMap {

	@IsRequired(channelId = "ActivePower")
	public WriteableChannel activePower;

	public MeterMap(String thingId) {
		super(thingId);
	}
}
