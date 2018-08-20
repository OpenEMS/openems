package io.openems.impl.controller.asymmetric.phaserectification;

import io.openems.api.channel.ReadChannel;
import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.api.device.nature.meter.AsymmetricMeterNature;

@IsThingMap(type = AsymmetricMeterNature.class)
public class Meter extends ThingMap {

	public ReadChannel<Long> activePowerL1;
	public ReadChannel<Long> activePowerL2;
	public ReadChannel<Long> activePowerL3;
	public ReadChannel<Long> reactivePowerL1;
	public ReadChannel<Long> reactivePowerL2;
	public ReadChannel<Long> reactivePowerL3;

	public Meter(AsymmetricMeterNature meter) {
		super(meter);
		activePowerL1 = meter.activePowerL1().required();
		activePowerL2 = meter.activePowerL2().required();
		activePowerL3 = meter.activePowerL3().required();
		reactivePowerL1 = meter.reactivePowerL1().required();
		reactivePowerL2 = meter.reactivePowerL2().required();
		reactivePowerL3 = meter.reactivePowerL3().required();
	}

}