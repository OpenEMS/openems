package io.openems.impl.controller.symmetric.balancingsurplus;

import io.openems.api.channel.ReadChannel;
import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.api.device.nature.charger.ChargerNature;

@IsThingMap(type = ChargerNature.class)
public class Charger extends ThingMap {

	public final ReadChannel<Long> power;
	public final ReadChannel<Long> inputVoltage;

	public Charger(ChargerNature thing) {
		super(thing);
		this.power = thing.getActualPower().required();
		this.inputVoltage = thing.getInputVoltage().required();
	}

}
