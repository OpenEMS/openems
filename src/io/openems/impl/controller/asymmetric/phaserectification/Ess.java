package io.openems.impl.controller.asymmetric.phaserectification;

import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.api.device.nature.ess.AsymmetricEssNature;

@IsThingMap(type = AsymmetricEssNature.class)
public class Ess extends ThingMap {

	public ReadChannel<Long> soc;
	public ReadChannel<Long> activePowerL1;
	public ReadChannel<Long> activePowerL2;
	public ReadChannel<Long> activePowerL3;
	public ReadChannel<Long> allowedCharge;
	public ReadChannel<Long> allowedDischarge;
	public WriteChannel<Long> setActivePowerL1;
	public WriteChannel<Long> setActivePowerL2;
	public WriteChannel<Long> setActivePowerL3;
	public ReadChannel<Long> allowedApparent;

	public Ess(AsymmetricEssNature thing) {
		super(thing);
		this.soc = thing.soc();
		this.activePowerL1 = thing.activePowerL1();
		this.activePowerL2 = thing.activePowerL2();
		this.activePowerL3 = thing.activePowerL3();
		this.allowedCharge = thing.allowedCharge();
		this.allowedDischarge = thing.allowedDischarge();
		this.setActivePowerL1 = thing.setActivePowerL1();
		this.setActivePowerL2 = thing.setActivePowerL2();
		this.setActivePowerL3 = thing.setActivePowerL3();
		this.allowedApparent = thing.allowedApparent();
	}

}
