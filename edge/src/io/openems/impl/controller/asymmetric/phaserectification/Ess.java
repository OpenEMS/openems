package io.openems.impl.controller.asymmetric.phaserectification;

import io.openems.api.channel.ReadChannel;
import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.api.device.nature.ess.AsymmetricEssNature;
import io.openems.core.utilities.AsymmetricPower;

@IsThingMap(type = AsymmetricEssNature.class)
public class Ess extends ThingMap {

	public ReadChannel<Long> soc;
	public ReadChannel<Long> activePowerL1;
	public ReadChannel<Long> activePowerL2;
	public ReadChannel<Long> activePowerL3;
	public ReadChannel<Long> reactivePowerL1;
	public ReadChannel<Long> reactivePowerL2;
	public ReadChannel<Long> reactivePowerL3;
	public AsymmetricPower power;

	public Ess(AsymmetricEssNature ess) {
		super(ess);
		this.soc = ess.soc();
		this.activePowerL1 = ess.activePowerL1().required();
		this.activePowerL2 = ess.activePowerL2().required();
		this.activePowerL3 = ess.activePowerL3().required();
		this.reactivePowerL1 = ess.reactivePowerL1().required();
		this.reactivePowerL2 = ess.reactivePowerL2().required();
		this.reactivePowerL3 = ess.reactivePowerL3().required();
		power = new AsymmetricPower(ess.allowedDischarge().required(), ess.allowedCharge().required(),
				ess.allowedApparent().required(), ess.setActivePowerL1().required(), ess.setActivePowerL2().required(),
				ess.setActivePowerL3().required(), ess.setReactivePowerL1().required(),
				ess.setReactivePowerL2().required(), ess.setReactivePowerL3().required());
	}

}
