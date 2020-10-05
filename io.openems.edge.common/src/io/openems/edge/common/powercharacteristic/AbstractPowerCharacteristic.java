package io.openems.edge.common.powercharacteristic;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.osgi.service.component.ComponentContext;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

public abstract class AbstractPowerCharacteristic extends AbstractOpenemsComponent implements OpenemsComponent {

	public final Clock clock;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	protected AbstractPowerCharacteristic() {
		this(Clock.systemDefaultZone());
	}

	protected AbstractPowerCharacteristic(Clock clock) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ChannelId.values()//
		);
		this.clock = clock;
	}

	protected void activate(ComponentContext context, String id, String alias) {
		throw new IllegalArgumentException("Use the other activate method");
	}

	/**
	 * Abstract activator.
	 * 
	 * @param context         the Bundle context
	 * @param id              the Component-ID
	 * @param alias           the Component Alias
	 * @param enabled         is the Component enabled?
	 * @param meterId         the Meter-ID
	 * @param noOfBufferHours the number of buffer hours to make sure the battery
	 *                        still charges full, even on prediction errors
	 */
	protected void activate(ComponentContext context, String id, String alias, boolean enabled) {
		super.activate(context, id, alias, enabled);
	}

	/**
	 * Initialize the P by U characteristics.
	 *
	 * <p>
	 * Parsing JSON then putting the point variables into pByUCharacteristicEquation
	 *
	 * <pre>
	 * [
	 *  { "voltageRatio": 0.9, 		"power" : -4000 }},
	 *  { "voltageRatio": 0.93,		"power": -1000 }},
	 *  { "voltageRatio": 1.07,		"power": 0}},
	 *  { "voltageRatio": 1.1, 		"power": 1000 } }
	 * ]
	 * </pre>
	 * 
	 * @param percentQ the configured Percent-by-Q values
	 * @return
	 * 
	 * @throws OpenemsNamedException on error
	 */

	private Map<Float, Float> initialize(String powerConfig) throws OpenemsNamedException {
		Map<Float, Float> voltagePowerMap = new HashMap<>();
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

		Map<Float, Float> voltagePowerMap = this.initialize(powerConfig);
		float linePowerValue = this.getValueOfLine(voltagePowerMap, ratio);

		return (int) linePowerValue;
	}

	private float getValueOfLine(Map<Float, Float> qCharacteristic, float gridVoltageRatio) {
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

	private Point getSmallerPoint(Map<Float, Float> qCharacteristic, float gridVoltageRatio) {
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

	private Point getGreaterPoint(Map<Float, Float> qCharacteristic, float gridVoltageRatio) {
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
