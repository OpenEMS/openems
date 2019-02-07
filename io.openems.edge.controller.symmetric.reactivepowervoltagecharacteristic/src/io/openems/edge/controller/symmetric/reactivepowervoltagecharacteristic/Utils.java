package io.openems.edge.controller.symmetric.reactivepowervoltagecharacteristic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class Utils {

	public static double getValueOfLine(Map<Double, Double> points, double voltageRatio) {
		double x = voltageRatio;
		List<Double> percentList = new ArrayList<Double>(points.values());
		List<Double> powerList = new ArrayList<Double>(points.keySet());
		Collections.sort(powerList, Collections.reverseOrder());
		Collections.sort(percentList, Collections.reverseOrder());
		// find to place of voltage ratio
		Point smaller = getSmallerPoint(points, voltageRatio);
		Point greater = getGreaterPoint(points, voltageRatio);
		double m = (greater.y - smaller.y) / (greater.x - smaller.x);
		double t = smaller.y - m * smaller.x;
		return m * x + t;
	}

	public static Point getSmallerPoint(Map<Double, Double> qCharacteristic, double voltageRatio) {
		Point p;
		int i = 0;
		// bubble sort outer loop
		qCharacteristic.put(voltageRatio, (double) 250);
		Comparator<Entry<Double, Double>> valueComparator = (e1, e2) -> e1.getKey().compareTo(e2.getKey());
		Map<Double, Double> powerMap = qCharacteristic.entrySet().stream().sorted(valueComparator)
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		Map<Double, Double> percentMap = qCharacteristic.entrySet().stream().sorted(valueComparator)
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		List<Double> powerList = new ArrayList<Double>(powerMap.keySet());
		List<Double> percentList = new ArrayList<Double>(percentMap.values());
		if (powerList.get(i) != voltageRatio) {
			i++;
		}
		if (i == 0) {
			p = new Point(powerList.get(0), percentList.get(0));
			return p;
		}
		p = new Point(powerList.get(i - 1), percentList.get(i - 1));
		return p;
	}

	public static Point getGreaterPoint(Map<Double, Double> qCharacteristic, double voltageRatio) {
		Point p;
		int i = 0;
		// bubble sort outer loop
		qCharacteristic.put(voltageRatio, (double) 250);
		Comparator<Entry<Double, Double>> valueComparator = (e1, e2) -> e1.getKey().compareTo(e2.getKey());
		Map<Double, Double> powerMap = qCharacteristic.entrySet().stream().sorted(valueComparator)
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		Map<Double, Double> percentMap = qCharacteristic.entrySet().stream().sorted(valueComparator)
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		List<Double> powerList = new ArrayList<Double>(powerMap.keySet());
		List<Double> percentList = new ArrayList<Double>(percentMap.values());
		if (powerList.get(i) != voltageRatio) {
			i++;
		}
		if (i > powerList.size()) {
			p = new Point(powerList.get(powerList.size() - 1), percentList.size() - 1);
			return p;
		}
		p = new Point(powerList.get(i + 1), percentList.get(i + 1));
		return p;
	}

	static class Point {

		private final double x;
		private final double y;

		public Point(double x, double y) {
			this.x = x;
			this.y = y;
		}

		public double getX() {
			return x;
		}

		public double getY() {
			return y;
		}
	}
}
