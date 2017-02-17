package io.openems.impl.controller.chargerlimitation;

import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.api.device.nature.charger.ChargerNature;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;

@IsThingMap(type = ChargerNature.class)
public class Charger extends ThingMap {

	private WriteChannel<Float> current;
	private ReadChannel<Float> voltage;
	public ReadChannel<Float> nominalCurrent;

	public Charger(ChargerNature thing) {
		super(thing);
		current = thing.setMaxCurrent();
		voltage = thing.getBatteryVoltage();
		nominalCurrent = thing.getNominalCurrent();
	}

	public void setPower(float power) throws WriteChannelException, InvalidValueException {
		float calculatedCurrent = power / voltage.value();
		if (calculatedCurrent > nominalCurrent.value()) {
			calculatedCurrent = nominalCurrent.value();
		}
		log.info("Set " + calculatedCurrent + " for Charger.");
		current.pushWrite(calculatedCurrent);
	}

}
