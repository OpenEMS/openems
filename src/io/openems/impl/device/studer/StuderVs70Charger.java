package io.openems.impl.device.studer;

import io.openems.api.channel.WriteChannel;
import io.openems.api.device.nature.charger.ChargerNature;
import io.openems.api.exception.ConfigException;
import io.openems.impl.protocol.studer.StuderDeviceNature;
import io.openems.impl.protocol.studer.internal.StuderProtocol;
import io.openems.impl.protocol.studer.internal.object.FloatParameterObject;

public class StuderVs70Charger extends StuderDeviceNature implements ChargerNature {

	public StuderVs70Charger(String thingId) throws ConfigException {
		super(thingId);
	}

	public WriteChannel<Float> batteryChargeCurrentValue;
	public WriteChannel<Float> batteryChargeCurrentUnsavedValue;

	@Override
	protected StuderProtocol defineStuderProtocol() throws ConfigException {
		StuderProtocol p = new StuderProtocol();

		FloatParameterObject batteryChargeCurrent = new FloatParameterObject(14217, "batteryChargeCurrent", "Adc",
				this);
		p.addObject(batteryChargeCurrent);
		batteryChargeCurrentValue = batteryChargeCurrent.value().channel();
		batteryChargeCurrentUnsavedValue = batteryChargeCurrent.unsavedValue().channel();

		return p;
	}

}
