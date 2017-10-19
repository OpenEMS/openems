package io.openems.backend.metadata.api.device;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonArray;

public class MetadataDevices implements Iterable<MetadataDevice> {
	private List<MetadataDevice> devices = new ArrayList<>();

	public Set<String> getNames() {
		Set<String> names = new HashSet<>();
		for (MetadataDevice device : this.devices) {
			names.add(device.getName());
		}
		return names;
	}

	public String getNamesString() {
		return String.join(",", this.getNames());
	}

	public boolean isEmpty() {
		return this.devices.isEmpty();
	}

	public void add(MetadataDevice device) {
		this.devices.add(device);
	}

	@Override
	public Iterator<MetadataDevice> iterator() {
		return this.devices.iterator();
	}

	public JsonArray toJson() {
		JsonArray j = new JsonArray();
		for (MetadataDevice device : this.devices) {
			j.add(device.toJsonObject());
		}
		return j;
	}
}
