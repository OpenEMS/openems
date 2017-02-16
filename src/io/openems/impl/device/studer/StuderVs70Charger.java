package io.openems.impl.device.studer;

import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.StaticValueChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.device.nature.charger.ChargerNature;
import io.openems.api.exception.ConfigException;
import io.openems.impl.protocol.studer.StuderDeviceNature;
import io.openems.impl.protocol.studer.internal.StuderProtocol;
import io.openems.impl.protocol.studer.internal.object.FloatParameterObject;
import io.openems.impl.protocol.studer.internal.object.FloatUserinfoObject;
import io.openems.impl.protocol.studer.internal.object.IntParameterObject;

public class StuderVs70Charger extends StuderDeviceNature implements ChargerNature {

	public StuderVs70Charger(String thingId) throws ConfigException {
		super(thingId);
	}

	public WriteChannel<Float> batteryChargeCurrentValue;
	public WriteChannel<Float> batteryChargeCurrentUnsavedValue;
	public WriteChannel<Integer> setStart;
	public WriteChannel<Integer> setStop;
	public ReadChannel<Float> batteryVoltage;
	public ReadChannel<Float> nominalCurrent = new StaticValueChannel<Float>("nominalCurrent", this, 70f).unit("A");

	@Override
	protected StuderProtocol defineStuderProtocol() throws ConfigException {
		StuderProtocol p = new StuderProtocol();

		FloatParameterObject batteryChargeCurrent = new FloatParameterObject(14217, "batteryChargeCurrent", "Adc",
				this);
		p.addObject(batteryChargeCurrent);
		batteryChargeCurrentValue = batteryChargeCurrent.value().channel();
		batteryChargeCurrentUnsavedValue = batteryChargeCurrent.unsavedValue().channel();
		IntParameterObject start = new IntParameterObject(14038, "start", "", this);
		p.addObject(start);
		setStart = start.value().channel();
		IntParameterObject stop = new IntParameterObject(14039, "stop", "", this);
		p.addObject(stop);
		setStop = stop.value().channel();
		FloatUserinfoObject vBatt = new FloatUserinfoObject(15000, "BatteryVoltage", "V", this);
		p.addObject(vBatt);
		batteryVoltage = (ReadChannel<Float>) vBatt.value().channel();
		return p;
	}

	@Override
	public WriteChannel<Float> setMaxCurrent() {
		return batteryChargeCurrentUnsavedValue;
	}

	@Override
	public ReadChannel<Float> getBatteryVoltage() {
		return batteryVoltage;
	}

	@Override
	public ReadChannel<Float> getNominalCurrent() {
		return nominalCurrent;
	}

}
