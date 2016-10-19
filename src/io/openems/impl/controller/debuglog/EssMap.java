package io.openems.impl.controller.debuglog;

import io.openems.api.channel.Channel;
import io.openems.api.channel.IsRequired;
import io.openems.api.channel.WriteableChannel;
import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.api.device.nature.Ess;

@IsThingMap(type = Ess.class)
public class EssMap extends ThingMap {

	@IsRequired(channelId = "ActivePower")
	public Channel activePower;

	@IsRequired(channelId = "AllowedCharge")
	public Channel allowedCharge;

	@IsRequired(channelId = "AllowedDischarge")
	public Channel allowedDischarge;

	@IsRequired(channelId = "MinSoc")
	public Channel minSoc;

	@IsRequired(channelId = "SetActivePower")
	public WriteableChannel setActivePower;

	@IsRequired(channelId = "Soc")
	public Channel soc;

	@IsRequired(channelId = "SystemState")
	public Channel systemState;

	public EssMap(String thingId) {
		super(thingId);
	}

	@Override
	public String toString() {
		return "ESS [soc=" + soc + ", minSoc=" + minSoc + ", activePower=" + activePower + ", allowedCharge="
				+ allowedCharge + ", allowedDischarge=" + allowedDischarge + ", setActivePower=" + setActivePower
				+ ", systemState=" + systemState + "]";
	}

}
