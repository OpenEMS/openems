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

	private static TreeMap<Float, Float> parseLine(JsonArray lineConfig) throws OpenemsNamedException {
		TreeMap<Float, Float> lineMap = new TreeMap<>();
		for (JsonElement element : lineConfig) {
			Float xCoordValue = JsonUtils.getAsFloat(element, "xCoord");
			Float yCoordValue = JsonUtils.getAsFloat(element, "yCoord");
			lineMap.put(xCoordValue, yCoordValue);
		}
		return lineMap;
	}

	public Float getLineValue(JsonArray lineConfig, float referencePoint) throws OpenemsNamedException {
		TreeMap<Float, Float> lineMap = parseLine(lineConfig);
		Entry<Float, Float> floorEntry = lineMap.floorEntry(referencePoint);
		Entry<Float, Float> ceilingEntry = lineMap.ceilingEntry(referencePoint);
		// In case of referencePoint is smaller than floorEntry key
		try {
			if (floorEntry.getKey().equals(referencePoint)) {
				return floorEntry.getValue().floatValue();
			}
		} catch (NullPointerException e) {
			return ceilingEntry.getValue().floatValue();
		}
		// In case of referencePoint is bigger than ceilingEntry key
		try {
			if (ceilingEntry.getKey().equals(referencePoint)) {
				return ceilingEntry.getValue().floatValue();
			}
		} catch (NullPointerException e) {
			return floorEntry.getValue().floatValue();
		}

		Float m = (ceilingEntry.getValue() - floorEntry.getValue()) / (ceilingEntry.getKey() - floorEntry.getKey());
		Float t = floorEntry.getValue() - m * floorEntry.getKey();
		return m * referencePoint + t;
	}
}
