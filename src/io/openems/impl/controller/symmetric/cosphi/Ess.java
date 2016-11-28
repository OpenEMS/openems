package io.openems.impl.controller.symmetric.cosphi;

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
	public final String id;
	public final ReadChannel<Long> allowedCharge;
	public final ReadChannel<Long> allowedDischarge;
	public final Power power;

	public Ess(SymmetricEssNature ess) {
		super(ess);
		setActivePower = ess.setActivePower();
		setReactivePower = ess.setReactivePower();
		id = ess.id();
		allowedCharge = ess.allowedCharge();
		allowedDischarge = ess.allowedDischarge();
		this.power = new Power(ess.allowedDischarge().required(), ess.allowedCharge().required(),
				ess.allowedApparent().required(), ess.setActivePower().required(), ess.setReactivePower().required());
	}

}
