package io.openems.edge.controller.asymmetric.powercharacteristic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.component.AbstractOpenemsComponent;

public abstract class AbstractPowerCharacteristic extends AbstractOpenemsComponent {
	public Map<Float, Float> voltagePowerMap = new HashMap<>();

	protected AbstractPowerCharacteristic(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	public float getValueOfLine(Map<Float, Float> qCharacteristic, float gridVoltageRatio) {
		float m = 0, t = 0;
		// find to place of grid voltage ratio
		Comparator<Entry<Float, Float>> valueComparator = (e1, e2) -> e1.getKey().compareTo(e2.getKey());
		Map<Float, Float> map = qCharacteristic.entrySet().stream().sorted(valueComparator)
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		System.out.println(map);
		List<Float> voltageList = new ArrayList<Float>(map.keySet());
		List<Float> powerList = new ArrayList<Float>(map.values());
		// if the grid voltage ratio in the list, return that point
		for (int i = 0; i < voltageList.size(); i++) {
			if (voltageList.get(i) == gridVoltageRatio) {
				int power = (int) powerList.get(i).intValue();
				return power;
			}
		}
		Point smaller = getSmallerPoint(qCharacteristic, gridVoltageRatio);
		Point greater = getGreaterPoint(qCharacteristic, gridVoltageRatio);
		if (greater.x == smaller.x && greater.y == smaller.y) {
			return smaller.y;
		}
		m = (float) ((greater.y - smaller.y) / (greater.x - smaller.x));
		t = (float) (smaller.y - m * smaller.x);
		return m * gridVoltageRatio + t;
	}

	protected class Point {
		private final float x;
		private final float y;

		public Point(float x, float y) {
			this.x = x;
			this.y = y;
		}

		public float getX() {
			return x;
		}

		public float getY() {
			return y;
		}
	}

	public Point getSmallerPoint(Map<Float, Float> qCharacteristic, float gridVoltageRatio) {
		Point p;
		int i = 0;
		// bubble sort outer loop
		qCharacteristic.put(gridVoltageRatio, (float) 0);
		Comparator<Entry<Float, Float>> valueComparator = (e1, e2) -> e1.getKey().compareTo(e2.getKey());
		Map<Float, Float> map = qCharacteristic.entrySet().stream().sorted(valueComparator)
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		List<Float> voltageList = new ArrayList<Float>(map.keySet());
		List<Float> percentList = new ArrayList<Float>(map.values());
		if (voltageList.get(2) == gridVoltageRatio) {
			p = new Point(gridVoltageRatio, 0);
			qCharacteristic.remove(gridVoltageRatio, (float) 0);
			return p;
		}
		while (voltageList.get(i) != gridVoltageRatio) {
			i++;
		}
		qCharacteristic.remove(gridVoltageRatio, (float) 0);
		// if its the first element; it will be equal to "0"
		if (i == 0) {
			p = new Point(voltageList.get(i + 1), percentList.get(i + 1));
			return p;
		}
		p = new Point(voltageList.get(i - 1), percentList.get(i - 1));
		return p;
	}

	public Point getGreaterPoint(Map<Float, Float> qCharacteristic, float gridVoltageRatio) {
		Point p;
		int i = 0;
		// bubble sort outer loop
		// 0 random number, just to fill value
		qCharacteristic.put(gridVoltageRatio, (float) 0);
		Comparator<Entry<Float, Float>> valueComparator = (e1, e2) -> e1.getKey().compareTo(e2.getKey());
		Map<Float, Float> map = qCharacteristic.entrySet().stream().sorted(valueComparator)
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		List<Float> voltageList = new ArrayList<Float>(map.keySet());
		List<Float> percentList = new ArrayList<Float>(map.values());
		if (voltageList.get(2) == gridVoltageRatio) {
			qCharacteristic.remove(gridVoltageRatio, (float) 0);
			p = new Point(gridVoltageRatio, 0);
			return p;
		}
		while (voltageList.get(i) != gridVoltageRatio) {
			i++;
		}

		// if its the last element it will be equal the size of list
		if ((i + 1) >= voltageList.size()) {
			qCharacteristic.remove(gridVoltageRatio, (float) 0);
			p = new Point(voltageList.get(voltageList.size() - 2), percentList.get((percentList.size() - 2)));
			return p;
		}
		qCharacteristic.remove(gridVoltageRatio, (float) 0);
		p = new Point(voltageList.get(i + 1), percentList.get(i + 1));
		return p;
	}

	public Map<Float, Float> initialize(String powerVoltConf) throws OpenemsNamedException {
		try {
			JsonArray jpowerV = JsonUtils.getAsJsonArray(JsonUtils.parse(powerVoltConf));
			for (JsonElement element : jpowerV) {
				Float powerConf = (float) JsonUtils.getAsInt(element, "power");
				float voltageRatioConf = JsonUtils.getAsFloat(element, "voltageRatio");
				this.voltagePowerMap.put(voltageRatioConf, powerConf);
			}
		} catch (NullPointerException e) {
			throw new OpenemsException("Unable to set values [" + powerVoltConf + "] " + e.getMessage());
		}
		return voltagePowerMap;
	}
}
