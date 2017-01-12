package io.openems.impl.controller.emergencygenerator;

import io.openems.api.channel.ReadChannel;
import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.impl.device.pro.FeneconProEss;

@IsThingMap(type = FeneconProEss.class)
public class Ess extends ThingMap {

	public ReadChannel<Long> gridMode;
	public ReadChannel<Long> soc;

	public Ess(FeneconProEss ess) {
		super(ess);
		gridMode = ess.gridMode().required();
		soc = ess.soc().required();
	}

}
