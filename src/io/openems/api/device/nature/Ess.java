package io.openems.api.device.nature;

import io.openems.api.channel.Channel;
import io.openems.api.channel.IsChannel;
import io.openems.api.channel.WriteableChannel;
import io.openems.api.thing.IsConfig;

public interface Ess extends DeviceNature {
	public final int DEFAULT_MINSOC = 10;

	@IsChannel(id = "ActivePower")
	public Channel activePower();

	@IsChannel(id = "AllowedCharge")
	public Channel allowedCharge();

	@IsChannel(id = "AllowedDischarge")
	public Channel allowedDischarge();

	@IsChannel(id = "ApparentPower")
	public Channel apparentPower();

	@IsChannel(id = "MinSoc")
	public Channel minSoc();

	@IsChannel(id = "RectivePower")
	public Channel reactivePower();

	@IsChannel(id = "SetActivePower")
	public WriteableChannel setActivePower();

	@IsConfig("MinSoc")
	public void setMinSoc(Integer minSoc);

	@IsChannel(id = "SetWorkState")
	public WriteableChannel setWorkState();

	@IsChannel(id = "Soc")
	public Channel soc();

	@IsChannel(id = "SystemState")
	public Channel systemState();
}
