package io.openems.backend.metadata.file.device;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.openems.backend.metadata.api.device.MetadataDeviceModel;
import io.openems.backend.metadata.api.device.MetadataDevices;
import io.openems.common.exceptions.OpenemsException;

public class MetadataFileDeviceModel implements MetadataDeviceModel {

	private final List<MetadataFileDevice> devices = new ArrayList<>();

	public MetadataFileDeviceModel(File file) throws IOException {
		FileReader fr = new FileReader(file);
		BufferedReader br = new BufferedReader(fr);
		String s;
		while ((s = br.readLine()) != null) {
			String[] parameters = s.split(";");
			MetadataFileDevice device = new MetadataFileDevice(parameters[0], parameters[1], parameters[2],
					parameters[3], Integer.parseInt(parameters[4]), parameters[5]);
			this.devices.add(device);
		}
		fr.close();
	}

	@Override
	public MetadataDevices getDevicesForApikey(String apikey) throws OpenemsException {
		// filter and convert to new list
		MetadataDevices result = new MetadataDevices();
		for (MetadataFileDevice device : this.devices) {
			if (device.getApikey().equals(apikey)) {
				result.add(device);
			}
		}
		return result;
	}

	public List<MetadataFileDevice> getAllDevices() {
		return this.devices;
	}
}
