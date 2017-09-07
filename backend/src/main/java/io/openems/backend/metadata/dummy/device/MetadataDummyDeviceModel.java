package io.openems.backend.metadata.dummy.device;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.openems.backend.metadata.api.device.MetadataDevice;
import io.openems.backend.metadata.api.device.MetadataDeviceModel;
import io.openems.common.exceptions.OpenemsException;

public class MetadataDummyDeviceModel implements MetadataDeviceModel {

	private static int lastId = 0;

	private final List<MetadataDummyDevice> devices = new ArrayList<>();

	public MetadataDummyDeviceModel() {}

	@Override
	public Optional<MetadataDevice> getDeviceForApikey(String apikey) throws OpenemsException {
		for (MetadataDummyDevice device : this.devices) {
			if (device.getApikey().equals(apikey)) {
				return Optional.of(device);
			}
		}
		return Optional.of(addNewDevice(apikey));
	}

	private MetadataDevice addNewDevice(String apikey) {
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
