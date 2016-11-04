package io.openems.impl.controller.balancingThreePhase;

import io.openems.api.channel.IsRequired;
import io.openems.api.channel.numeric.NumericChannel;
import io.openems.api.channel.numeric.WriteableNumericChannel;
import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.api.exception.InvalidValueException;
import io.openems.impl.device.pro.FeneconProEss;

@IsThingMap(type = FeneconProEss.class)
public class Ess extends ThingMap {

	@IsRequired(channelId = "ActivePowerPhaseA")
	public NumericChannel activePowerPhaseA;
	@IsRequired(channelId = "ActivePowerPhaseB")
	public NumericChannel activePowerPhaseB;
	@IsRequired(channelId = "ActivePowerPhaseC")
	public NumericChannel activePowerPhaseC;

	@IsRequired(channelId = "AllowedCharge")
	public NumericChannel allowedCharge;

	@IsRequired(channelId = "AllowedDischarge")
	public NumericChannel allowedDischarge;

	@IsRequired(channelId = "AllowedApparent")
	public NumericChannel allowedApparent;

	@IsRequired(channelId = "SetActivePowerPhaseA")
	public WriteableNumericChannel setActivePowerPhaseA;

	@IsRequired(channelId = "SetReactivePowerPhaseA")
	public WriteableNumericChannel setReactivePowerPhaseA;

	@IsRequired(channelId = "SetActivePowerPhaseB")
	public WriteableNumericChannel setActivePowerPhaseB;

	@IsRequired(channelId = "SetReactivePowerPhaseB")
	public WriteableNumericChannel setReactivePowerPhaseB;

	@IsRequired(channelId = "SetActivePowerPhaseC")
	public WriteableNumericChannel setActivePowerPhaseC;

	@IsRequired(channelId = "SetReactivePowerPhaseC")
	public WriteableNumericChannel setReactivePowerPhaseC;

	@IsRequired(channelId = "MinSoc")
	public NumericChannel minSoc;

	@IsRequired(channelId = "Soc")
	public NumericChannel soc;

	@IsRequired(channelId = "SetWorkState")
	public WriteableNumericChannel setWorkState;

	public Ess(String thingId) {
		super(thingId);
		// TODO Auto-generated constructor stub
	}

	public long useableSoc() throws InvalidValueException {
		return soc.getValue() - minSoc.getValue();
	}

}
