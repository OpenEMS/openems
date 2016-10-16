package io.openems.api.device;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import io.openems.api.device.nature.DeviceNature;
import io.openems.api.device.nature.IsDeviceNature;
import io.openems.api.exception.OpenemsException;
import io.openems.api.thing.Thing;
import io.openems.core.bridge.Bridge;

public abstract class Device implements Thing {
	public final static String THINGID_PREFIX = "_device";
	private static int instanceCounter = 0;
	private Bridge bridge = null;
	private DeviceNature[] deviceNatures = null;
	private final String thingId;

	public Device() throws OpenemsException {
		this.thingId = THINGID_PREFIX + instanceCounter++;
	}

	public Bridge getBridge() {
		return bridge;
	}

	@Override
	public String getThingId() {
		return this.thingId;
	}

	// TODO @IsConfigParameter("bridge")
	public void setBridge(Bridge bridge) {
		this.bridge = bridge;
	}

	protected DeviceNature[] getDeviceNatures() throws OpenemsException {
		if (this.deviceNatures == null) {
			/*
			 * Parse Device for {@link DeviceNature}s and store them in local deviceNatures array
			 */
			List<DeviceNature> deviceNatures = new ArrayList<>();
			for (Field field : this.getClass().getDeclaredFields()) {
				if (field.isAnnotationPresent(IsDeviceNature.class)) {
					try {
						deviceNatures.add((DeviceNature) field.get(this));
					} catch (IllegalArgumentException | IllegalAccessException | ClassCastException e) {
						e.printStackTrace();
						throw new OpenemsException("Unable to parse DeviceNature [" + field + "]: " + e.getMessage());
					}
				}
			}
			this.deviceNatures = deviceNatures.stream().toArray(DeviceNature[]::new);
		}
		return this.deviceNatures;
	}
}
