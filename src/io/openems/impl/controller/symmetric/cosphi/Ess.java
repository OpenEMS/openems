package io.openems.impl.controller.symmetric.cosphi;

import io.openems.api.channel.WriteChannel;
import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.api.device.nature.ess.SymmetricEssNature;

@IsThingMap(type = SymmetricEssNature.class)
public class Ess extends ThingMap {

	public final WriteChannel<Long> setActivePower;
	public final WriteChannel<Long> setReactivePower;
	public final String id;

	public Ess(SymmetricEssNature ess) {
		super(ess);
		setActivePower = ess.setActivePower();
		setReactivePower = ess.setReactivePower();
		id = ess.id();
	}

}
