package io.openems.edge.common.linecharacteristic;

import java.util.Map.Entry;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;

/**
 * Defines a polyline built of multiple points defined by a JsonArray.
 * 
 * <p>
 * This class can be used e.g. to build Q-by-U characteristics Controllers.
 */
public class PolyLine {

	private final TreeMap<Float, Float> points;

	/**
	 * Creates a PolyLine from two points.
	 * 
	 * @param x1 'x' value of point 1
	 * @param y1 'y' value of point 1
	 * @param x2 'x' value of point 2
	 * @param y2 'y' value of point 2
	 * @throws OpenemsNamedException on error
	 */
	public PolyLine(Float x1, Float y1, Float x2, Float y2) throws OpenemsNamedException {
		TreeMap<Float, Float> points = new TreeMap<>();
		points.put(x1, y1);
		points.put(x2, y2);
		this.points = points;
	}

	/**
	 * Creates a PolyLine from a JSON line configuration.
	 * 
	 * @param x          the name of the 'x' value inside the Json-Array
	 * @param y          the name of the 'y' value inside the Json-Array
	 * @param lineConfig the configured x and y coordinates values; parsed to a
	 *                   Json-Array
	 * @throws OpenemsNamedException on error
	 */
	public PolyLine(String x, String y, String lineConfig) throws OpenemsNamedException {
		this(x, y, JsonUtils.getAsJsonArray(JsonUtils.parse(lineConfig)));
	}

	/**
	 * Creates a PolyLine from a JSON line configuration.
	 *
	 * <p>
	 * Parse the given JSON line format to x and y parameters.
	 *
	 * <pre>
	 * [
	 *  { "x": 0.9,  "y":-4000 },
	 *  { "x": 0.93, "y":-1000 },
	 *  { "x": 1.07, "y":1000 },
	 *  { "x": 1.1,  "y":4000 }
	 * ]
	 * </pre>
	 *
	 * @param x          the name of the 'x' value inside the Json-Array
	 * @param y          the name of the 'y' value inside the Json-Array
	 * @param lineConfig the configured x and y coordinates values
	 * @throws OpenemsNamedException on error
	 */
	public PolyLine(String x, String y, JsonArray lineConfig) throws OpenemsNamedException {
		TreeMap<Float, Float> points = new TreeMap<>();
		for (JsonElement element : lineConfig) {
			Float xValue = JsonUtils.getAsFloat(element, x);
			Float yValue = JsonUtils.getAsFloat(element, y);
			points.put(xValue, yValue);
		}
		this.points = points;
	}

	/**
	 * Gets the Y-value for the given X.
	 * 
	 * @param x the 'x' value
	 * @return the 'y' value
	 * @throws OpenemsNamedException on error
	 */
	public Float getValue(float x) throws OpenemsNamedException {
		Entry<Float, Float> floorEntry = this.points.floorEntry(x);
		Entry<Float, Float> ceilingEntry = this.points.ceilingEntry(x);

		if (floorEntry == null && ceilingEntry == null) {
			return null;

		} else if (floorEntry == null) {
			return ceilingEntry.getValue();

		} else if (ceilingEntry == null) {
			return floorEntry.getValue();

		} else if (floorEntry.equals(ceilingEntry)) {
			return floorEntry.getValue();

		} else {
			Float m = (ceilingEntry.getValue() - floorEntry.getValue()) / (ceilingEntry.getKey() - floorEntry.getKey());
			Float t = floorEntry.getValue() - m * floorEntry.getKey();
			return m * x + t;
		}
	}
}
