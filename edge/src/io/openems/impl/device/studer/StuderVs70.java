package io.openems.impl.device.studer;

import java.util.HashSet;
import java.util.Set;

import io.openems.api.bridge.Bridge;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.OpenemsException;
import io.openems.impl.protocol.studer.StuderDevice;

@ThingInfo(title = "Studer VS-70")
public class StuderVs70 extends StuderDevice {

	/*
	 * Constructors
	 */
	public StuderVs70(Bridge parent) throws OpenemsException {
		super(parent);
	}

	/*
	 * Config
	 */
	@ChannelInfo(title = "Charger", description = "Sets the charger nature.", type = StuderVs70Charger.class)
	public final ConfigChannel<StuderVs70Charger> charger = new ConfigChannel<>("charger", this);

	/*
	 * Methods
	 */
	@Override
	public String toString() {
		return "StuderVs70 [charger=" + charger + ", getThingId()=" + id() + "]";
	}

	@Override
	protected Set<DeviceNature> getDeviceNatures() {
		Set<DeviceNature> natures = new HashSet<>();
		if (charger.valueOptional().isPresent()) {
			natures.add(charger.valueOptional().get());
		}
		return natures;
	}
}
