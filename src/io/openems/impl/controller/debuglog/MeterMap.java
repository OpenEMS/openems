package io.openems.impl.controller.debuglog;

import io.openems.api.channel.Channel;
import io.openems.api.channel.IsRequired;
import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.api.device.nature.Meter;

@IsThingMap(type = Meter.class)
public class MeterMap extends ThingMap {

	@IsRequired(channelId = "ActiveNegativeEnergy")
	public Channel activeNegativeEnergy;

	@IsRequired(channelId = "ActivePositiveEnergy")
	public Channel activePositiveEnergy;

	@IsRequired(channelId = "ActivePower")
	public Channel activePower;

	@IsRequired(channelId = "ApparentEnergy")
	public Channel apparentEnergy;

	@IsRequired(channelId = "ApparentPower")
	public Channel apparentPower;

	@IsRequired(channelId = "ReactiveNegativeEnergy")
	public Channel reactiveNegativeEnergy;

	@IsRequired(channelId = "ReactivePositiveEnergy")
	public Channel reactivePositiveEnergy;

	@IsRequired(channelId = "ReactivePower")
	public Channel reactivePower;

	public MeterMap(String thingId) {
		super(thingId);
	}

	@Override
	public String toString() {
		return "Meter [activePower=" + activePower + ", reactivePower=" + reactivePower + ", apparentPower="
				+ apparentPower + ", activePositiveEnergy=" + activePositiveEnergy + ", activeNegativeEnergy="
				+ activeNegativeEnergy + ", reactivePositiveEnergy=" + reactivePositiveEnergy
				+ ", reactiveNegativeEnergy=" + reactiveNegativeEnergy + ", apparentEnergy=" + apparentEnergy + "]";
	}
}
