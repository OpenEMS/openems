package io.openems.edge.controller.symmetric.reactivepowervoltagecharacteristic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.openems.edge.common.channel.AbstractReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(ReactivePowerVoltageCharacteristic c) {
		return Stream.of( //
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE:
						return new StateCollectorChannel(c, channelId);
					}
					return null;
				}), //
				Arrays.stream(Controller.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case RUN_FAILED:
						return new StateChannel(c, channelId);
					}
					return null;
				}) //
		).flatMap(channel -> channel);
	}

	public static float getValueOfLine(Map<Float, Float> points, float voltageRatio) {
		float x = voltageRatio;
		List<Float> percentList = new ArrayList<Float>(points.values());
		List<Float> voltageList = new ArrayList<Float>(points.keySet());
		Collections.sort(voltageList, Collections.reverseOrder());
		Collections.sort(percentList, Collections.reverseOrder());
		// find to place of voltage ratio
		Point smaller = getSmallerPoint(points, voltageRatio);
		Point greater = getGreaterPoint(points, voltageRatio);
		float m = (float) ((greater.y - smaller.y) / (greater.x - smaller.x));
		float t = (float) (smaller.y - m * smaller.x);
		return m * x + t;
	}

	public static Point getSmallerPoint(Map<Float, Float> qCharacteristic, float voltageRatio) {
		Point p;
		int i = 0;
		// bubble sort outer loop
		qCharacteristic.put(voltageRatio, (float) 0);
		Comparator<Entry<Float, Float>> valueComparator = (e1, e2) -> e1.getKey().compareTo(e2.getKey());
		Map<Float, Float> Map = qCharacteristic.entrySet().stream().sorted(valueComparator)
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		List<Float> voltageList = new ArrayList<Float>(Map.keySet());
		List<Float> percentList = new ArrayList<Float>(Map.values());
		if (voltageList.get(i) != voltageRatio) {
			i++;
		}
		if (i == 0) {
			p = new Point(voltageList.get(0), percentList.get(0));
			return p;
		}
		p = new Point(voltageList.get(i - 1), percentList.get(i - 1));

		qCharacteristic.remove(voltageRatio, (float) 0);
		return p;
	}

	public static Point getGreaterPoint(Map<Float, Float> qCharacteristic, float voltageRatio) {
		Point p;
		int i = 0;
		// bubble sort outer loop
		// 0 random number, just to fill value
		qCharacteristic.put(voltageRatio, (float) 0);
		Comparator<Entry<Float, Float>> valueComparator = (e1, e2) -> e1.getKey().compareTo(e2.getKey());
		Map<Float, Float> Map = qCharacteristic.entrySet().stream().sorted(valueComparator)
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		List<Float> voltageList = new ArrayList<Float>(Map.keySet());
		List<Float> percentList = new ArrayList<Float>(Map.values());
		if (voltageList.get(i) != voltageRatio) {
			i++;
		}
		if (i > voltageList.size()) {
			p = new Point(voltageList.get(voltageList.size() - 1), percentList.size() - 1);
			return p;
		}
		p = new Point(voltageList.get(i + 1), percentList.get(i + 1));

		qCharacteristic.remove(voltageRatio, (float) 0);
		return p;
	}

	static class Point {

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
