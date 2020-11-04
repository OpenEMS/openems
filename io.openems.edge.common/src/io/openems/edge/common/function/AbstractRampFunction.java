package io.openems.edge.common.function;

import java.util.Map.Entry;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.component.AbstractOpenemsComponent;

public abstract class AbstractRampFunction extends AbstractOpenemsComponent {

	protected AbstractRampFunction(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	private static TreeMap<Float, Float> parseLine(JsonArray powerConfig) throws OpenemsNamedException {
		TreeMap<Float, Float> voltagePowerMap = new TreeMap<>();
		for (JsonElement element : powerConfig) {
			Float powerConfValue = (float) JsonUtils.getAsInt(element, "power");
			float voltageRatioConfValue = JsonUtils.getAsFloat(element, "voltageRatio");
			voltagePowerMap.put(voltageRatioConfValue, powerConfValue);
		}
		return voltagePowerMap;
	}

	public Float getLineValue(JsonArray powerConfig, float ratio) throws OpenemsNamedException {
		TreeMap<Float, Float> voltagePowerMap = parseLine(powerConfig);
		Entry<Float, Float> floorEntry = voltagePowerMap.floorEntry(ratio);
		Entry<Float, Float> ceilingEntry = voltagePowerMap.ceilingEntry(ratio);
		// In case of ratio is smaller than floorEntry key
		try {
			if (floorEntry.getKey().equals(ratio)) {
				return floorEntry.getValue().floatValue();
			}
		} catch (NullPointerException e) {
			return ceilingEntry.getValue().floatValue();
		}
		// In case of ratio is bigger than ceilingEntry key
		try {
			if (ceilingEntry.getKey().equals(ratio)) {
				return ceilingEntry.getValue().floatValue();
			}
		} catch (NullPointerException e) {
			return floorEntry.getValue().floatValue();
		}

		Float m = (ceilingEntry.getValue() - floorEntry.getValue()) / (ceilingEntry.getKey() - floorEntry.getKey());
		Float t = floorEntry.getValue() - m * floorEntry.getKey();
		return m * ratio + t;
	}
}
