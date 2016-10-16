package io.openems.api.device.nature;

import io.openems.api.channel.Channel;
import io.openems.api.channel.IsChannel;
import io.openems.api.thing.IsConfigParameter;

public interface Ess extends DeviceNature {
	public final int DEFAULT_MINSOC = 10;

	// TODO @IsChannel(id = "ActivePower", address = 0)
	public Channel getActivePower();

	@IsChannel(id = "soc", address = 0)
	public Channel getSoc();

	@IsConfigParameter("minSoc")
	public void setMinSoc(Integer minSoc);
}
