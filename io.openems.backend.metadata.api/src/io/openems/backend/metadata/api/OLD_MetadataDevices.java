package io.openems.backend.metadata.api;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonArray;

public class OLD_MetadataDevices implements Iterable<OLD_MetadataDevice> {
	private List<OLD_MetadataDevice> devices = new ArrayList<>();

	public Set<String> getNames() {
		Set<String> names = new HashSet<>();
		for (OLD_MetadataDevice device : this.devices) {
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

	public void add(OLD_MetadataDevice device) {
		this.devices.add(device);
	}

	@Override
	public Iterator<OLD_MetadataDevice> iterator() {
		return this.devices.iterator();
	}

	public JsonArray toJson() {
		JsonArray j = new JsonArray();
		for (OLD_MetadataDevice device : this.devices) {
			j.add(device.toJsonObject());
		}
		return j;
	}
}
