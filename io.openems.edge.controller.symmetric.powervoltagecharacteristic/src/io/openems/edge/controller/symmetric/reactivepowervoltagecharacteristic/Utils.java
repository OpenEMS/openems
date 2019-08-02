package io.openems.edge.controller.symmetric.reactivepowervoltagecharacteristic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class Utils {

	public static float getValueOfLine(Map<Float, Float> qCharacteristic, float gridVoltageRatio) {
		float m = 0, t = 0;
		Comparator<Entry<Float, Float>> valueComparator = (e1, e2) -> e1.getKey().compareTo(e2.getKey());
		Map<Float, Float> map = qCharacteristic.entrySet().stream().sorted(valueComparator)
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		List<Float> voltageList = new ArrayList<Float>(map.keySet());
		List<Float> percentOrPowerList = new ArrayList<Float>(map.values());
		// find to place of grid voltage ratio
		for (int i = 0; i < voltageList.size(); i++) {
			if (voltageList.get(i) == gridVoltageRatio) {
				int percentOrPower = (int) percentOrPowerList.get(i).intValue();
				return percentOrPower;
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

	public static Point getSmallerPoint(Map<Float, Float> qCharacteristic, float gridVoltageRatio) {
		Point p;
		int i = 0;

		// bubble sort outer loop
		qCharacteristic.put(gridVoltageRatio, (float) 0);
		Comparator<Entry<Float, Float>> valueComparator = (e1, e2) -> e1.getKey().compareTo(e2.getKey());
		Map<Float, Float> map = qCharacteristic.entrySet().stream().sorted(valueComparator)
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		List<Float> voltageList = new ArrayList<Float>(map.keySet());
		List<Float> percentList = new ArrayList<Float>(map.values());
		while (voltageList.get(i) != gridVoltageRatio) {
			i++;
		}
		qCharacteristic.remove(gridVoltageRatio, (float) 0);

		// if its the first element; i will be equal to s"0"
		if (i == 0) {
			p = new Point(voltageList.get(i + 1), percentList.get(i + 1));
			return p;
		}
		p = new Point(voltageList.get(i - 1), percentList.get(i - 1));
		return p;
	}

	public static Point getGreaterPoint(Map<Float, Float> qCharacteristic, float voltageRatio) {
		Point p;
		int i = 0;
		// bubble sort outer loop
		// 0 random number, just to fill value
		qCharacteristic.put(voltageRatio, (float) 0);
		Comparator<Entry<Float, Float>> valueComparator = (e1, e2) -> e1.getKey().compareTo(e2.getKey());
		Map<Float, Float> map = qCharacteristic.entrySet().stream().sorted(valueComparator)
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		List<Float> voltageList = new ArrayList<Float>(map.keySet());
		List<Float> percentList = new ArrayList<Float>(map.values());
		while (voltageList.get(i) != voltageRatio) {
			i++;
		}
		// if its the last element it will be equal the size of list
		if ((i + 1) >= voltageList.size()) {
			p = new Point(voltageList.get(voltageList.size() - 2), percentList.get((percentList.size() - 2)));
			return p;
		}
		qCharacteristic.remove(voltageRatio, (float) 0);
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
