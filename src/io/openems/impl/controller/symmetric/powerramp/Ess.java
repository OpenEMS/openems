package io.openems.impl.controller.symmetric.powerramp;

import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.api.device.nature.ess.SymmetricEssNature;
import io.openems.core.utilities.Power;

@IsThingMap(type = SymmetricEssNature.class)
public class Ess extends ThingMap {

	public final WriteChannel<Long> setActivePower;
	public final WriteChannel<Long> setReactivePower;
	public final ReadChannel<Long> activePower;
	public final String id;
	public final Power power;
	public final ReadChannel<Long> gridMode;

	public Ess(SymmetricEssNature ess) {
		super(ess);
		setActivePower = ess.setActivePower();
		setReactivePower = ess.setReactivePower();
		id = ess.id();
		activePower = ess.activePower().required();
		this.power = new Power(ess.allowedDischarge().required(), ess.allowedCharge().required(),
				ess.allowedApparent().required(), ess.setActivePower().required(), ess.setReactivePower().required(),
				1);
		this.gridMode = ess.gridMode();
	}

}
