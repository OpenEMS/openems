package io.openems.edge.common.test;

import java.util.Map;
import java.util.TreeMap;

import com.google.gson.JsonElement;

import io.openems.edge.common.serialnumber.SerialNumberStorage;

public class DummySerialNumberStorage implements SerialNumberStorage {

	private final Map<String, Map<String, JsonElement>> values = new TreeMap<>();

	@Override
	public void put(String componentId, String channelId, JsonElement value) {
		this.values.computeIfAbsent(componentId, t -> new TreeMap<>()).put(channelId, value);
	}

}
