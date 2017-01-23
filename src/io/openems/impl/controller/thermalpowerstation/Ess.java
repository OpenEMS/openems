package io.openems.impl.controller.thermalpowerstation;

import io.openems.api.channel.ReadChannel;
import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.api.device.nature.ess.EssNature;

@IsThingMap(type = EssNature.class)
public class Ess extends ThingMap {

	public ReadChannel<Long> soc;

	public Ess(EssNature thing) {
		super(thing);
		this.soc = thing.soc().required();
	}

}
