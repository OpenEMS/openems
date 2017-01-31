package io.openems.impl.controller.thermalpowerstation;

import io.openems.api.channel.ReadChannel;
import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.api.device.nature.meter.SymmetricMeterNature;

@IsThingMap(type = SymmetricMeterNature.class)
public class Meter extends ThingMap {

	public ReadChannel<Long> power;

	public Meter(SymmetricMeterNature meter) {
		super(meter);
		power = meter.activePower().required();
	}

}
