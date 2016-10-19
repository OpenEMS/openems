package io.openems.impl.controller.balancing;

import io.openems.api.channel.Channel;
import io.openems.api.channel.IsRequired;
import io.openems.api.channel.WriteableChannel;
import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.api.device.nature.Ess;
import io.openems.api.exception.InvalidValueException;

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

	public EssMap(String thingId) {
		super(thingId);
	}

	public int getUseableSoc() throws InvalidValueException {
		return soc.getValue().subtract(minSoc.getValue()).intValue();
	}
}
