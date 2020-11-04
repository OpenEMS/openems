package io.openems.edge.common.function;

import java.util.Map.Entry;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.component.AbstractOpenemsComponent;

public abstract class AbstractRampFunction extends AbstractOpenemsComponent {

	protected AbstractRampFunction(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	private static TreeMap<Float, Float> parseLine(JsonArray powerConfig) throws OpenemsNamedException {
		TreeMap<Float, Float> voltagePowerMap = new TreeMap<>();
		try {
			for (JsonElement element : powerConfig) {
				Float powerConfValue = (float) JsonUtils.getAsInt(element, "power");
				float voltageRatioConfValue = JsonUtils.getAsFloat(element, "voltageRatio");
				voltagePowerMap.put(voltageRatioConfValue, powerConfValue);
			}
		} catch (NullPointerException e) {
			throw new OpenemsException("Unable to set values [" + powerConfig + "] " + e.getMessage());
		}
		return voltagePowerMap;
	}

	public Integer getLineValue(JsonArray powerConfig, float ratio) throws OpenemsNamedException {
		TreeMap<Float, Float> voltagePowerMap = parseLine(powerConfig);
		Entry<Float, Float> floorEntry = voltagePowerMap.floorEntry(ratio);
		Entry<Float, Float> ceilingEntry = voltagePowerMap.ceilingEntry(ratio);
		// In case of ratio is smaller than floorEntry key 
		try {
			if (floorEntry.getKey().equals(ratio)) {
				return floorEntry.getValue().intValue();
			}
		} catch (NullPointerException e) {
			return ceilingEntry.getValue().intValue();
		}
		//In case of ratio is bigger than ceilingEntry key
		try {
			if (ceilingEntry.getKey().equals(ratio)) {
				return ceilingEntry.getValue().intValue();
			}
		} catch (NullPointerException e) {
			return floorEntry.getValue().intValue();
		}

		float m = (float) ((ceilingEntry.getValue() - floorEntry.getValue())
				/ (ceilingEntry.getKey() - floorEntry.getKey()));
		float t = (float) (floorEntry.getValue() - m * floorEntry.getKey());
		return (int) (m * ratio + t);
	}
}
