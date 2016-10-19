package io.openems.api.device.nature;

import io.openems.api.channel.Channel;
import io.openems.api.channel.IsChannel;

public interface Meter extends DeviceNature {

	@IsChannel(id = "ActiveNegativeEnergy")
	public Channel activeNegativeEnergy();

	@IsChannel(id = "ActivePositiveEnergy")
	public Channel activePositiveEnergy();

	@IsChannel(id = "ActivePower")
	public Channel activePower();

	@IsChannel(id = "ApparentEnergy")
	public Channel apparentEnergy();

	@IsChannel(id = "ApparentPower")
	public Channel apparentPower();

	@IsChannel(id = "ReactiveNegativeEnergy")
	public Channel reactiveNegativeEnergy();

	@IsChannel(id = "ReactivePositiveEnergy")
	public Channel reactivePositiveEnergy();

	@IsChannel(id = "ReactivePower")
	public Channel reactivePower();
}
