package io.openems.impl.controller.offGridIndication;

import io.openems.api.channel.ReadChannel;
import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.api.device.nature.meter.SymmetricMeterNature;

@IsThingMap(type = SymmetricMeterNature.class)
public class Meter extends ThingMap {

	public final ReadChannel<Long> voltage;

	public Meter(SymmetricMeterNature meter) {
		super(meter);
		voltage = meter.voltage().required();
	}

}
