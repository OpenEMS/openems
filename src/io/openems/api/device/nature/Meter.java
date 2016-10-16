package io.openems.api.device.nature;

import io.openems.api.channel.Channel;
import io.openems.api.channel.IsChannel;

public interface Meter extends DeviceNature {

	@IsChannel(id = "ActivePower", address = 1)
	public Channel getActivePower();

}
