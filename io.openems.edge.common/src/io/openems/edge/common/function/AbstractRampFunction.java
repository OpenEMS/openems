package io.openems.edge.common.function;

import java.util.ArrayList;
import java.util.List;
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

	private TreeMap<Float, Float> initialize(String powerConfig) throws OpenemsNamedException {
		TreeMap<Float, Float> voltagePowerMap = new TreeMap<>();
		try {
			JsonArray jpowerV = JsonUtils.getAsJsonArray(JsonUtils.parse(powerConfig));
			for (JsonElement element : jpowerV) {
				Float powerConfValue = (float) JsonUtils.getAsInt(element, "power");
				float voltageRatioConfValue = JsonUtils.getAsFloat(element, "voltageRatio");
				voltagePowerMap.put(voltageRatioConfValue, powerConfValue);
			}
		} catch (NullPointerException e) {
			throw new OpenemsException("Unable to set values [" + powerConfig + "] " + e.getMessage());
		}
		return voltagePowerMap;
	}

	public Integer getPowerLine(String powerConfig, float ratio) throws OpenemsNamedException {
		TreeMap<Float, Float> voltagePowerMap = this.initialize(powerConfig);
		float linePowerValue = this.getValueOfLine(voltagePowerMap, ratio);
		return (int) linePowerValue;
	}

	private float getValueOfLine(TreeMap<Float, Float> qCharacteristic, float gridVoltageRatio) {
		float m = 0, t = 0;
		List<Float> voltageList = new ArrayList<Float>(qCharacteristic.keySet());
		List<Float> powerList = new ArrayList<Float>(qCharacteristic.values());
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

	private Point getSmallerPoint(TreeMap<Float, Float> qCharacteristic, float gridVoltageRatio) {
		Point p;
		int i = 0;
		qCharacteristic.put(gridVoltageRatio, (float) 0);
		List<Float> voltageList = new ArrayList<Float>(qCharacteristic.keySet());
		List<Float> percentList = new ArrayList<Float>(qCharacteristic.values());
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

	private Point getGreaterPoint(TreeMap<Float, Float> qCharacteristic, float gridVoltageRatio) {
		Point p;
		int i = 0;
		// 0 random number, just to fill value
		qCharacteristic.put(gridVoltageRatio, (float) 0);
		List<Float> voltageList = new ArrayList<Float>(qCharacteristic.keySet());
		List<Float> percentList = new ArrayList<Float>(qCharacteristic.values());
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

	protected static class Point {
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
}
