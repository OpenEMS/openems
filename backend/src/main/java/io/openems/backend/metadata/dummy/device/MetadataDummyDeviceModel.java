package io.openems.backend.metadata.dummy.device;

import java.util.ArrayList;
import java.util.List;

import io.openems.backend.metadata.api.device.MetadataDeviceModel;
import io.openems.backend.metadata.api.device.MetadataDevices;
import io.openems.common.exceptions.OpenemsException;

public class MetadataDummyDeviceModel implements MetadataDeviceModel {

	private static int lastId = 0;

	private final List<MetadataDummyDevice> devices = new ArrayList<>();

	public MetadataDummyDeviceModel() {}

	@Override
	public MetadataDevices getDevicesForApikey(String apikey) throws OpenemsException {
		// filter and convert to new list
		MetadataDevices result = new MetadataDevices();
		for (MetadataDummyDevice device : this.devices) {
			if (device.getApikey().equals(apikey)) {
				result.add(device);
			}
		}
		// add device if it was not there yet
		if (result.isEmpty()) {
			MetadataDummyDevice device = addNewDevice(apikey);
			result.add(device);
		}
		return result;
	}

	private MetadataDummyDevice addNewDevice(String apikey) {
		int id = MetadataDummyDeviceModel.lastId++;
		MetadataDummyDevice device = new MetadataDummyDevice("openems" + id, "OpenEMS " + id, "Dummy Product", "admin",
				id, apikey);
		this.devices.add(device);
		return device;
	}

	public List<MetadataDummyDevice> getAllDevices() {
		return this.devices;
	}
}
