package io.openems.edge.common.linecharacteristic;

import java.security.PolicySpi;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.type.TypeUtils;

/**
 * Defines a polyline built of multiple points defined by a JsonArray.
 *
 * <p>
 * This class can be used e.g. to build Q-by-U characteristics Controllers.
 */
public class PolyLine {

	private final TreeMap<Double, Double> points;

	public static class Builder {
		private final TreeMap<Double, Double> points = new TreeMap<>();

		private Builder() {
		}

		/**
		 * Add a point to the {@link PolyLine} {@link Builder}.
		 * 
		 * @param x the x value
		 * @param y the y value
		 * @return myself
		 */
		public Builder addPoint(double x, Double y) {
			this.points.put(x, y);
			return this;
		}

		/**
		 * Add a point to the {@link PolyLine} {@link Builder}.
		 * 
		 * @param x the x value
		 * @param y the y value
		 * @return myself
		 */
		public Builder addPoint(double x, double y) {
			this.points.put(x, y);
			return this;
		}

		public PolyLine build() {
			return new PolyLine(this.points);
		}
	}

	/**
	 * Create a PolyLine builder.
	 *
	 * @return a {@link Builder}
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Create a PolyLine that returns null for every 'x'.
	 *
	 * @return a {@link PolyLine}
	 */
	public static PolyLine empty() {
		return new PolyLine((Double) null);
	}

	/**
	 * Creates a static PolyLine, i.e. the 'y' value is the same for each 'x'.
	 *
	 * @param y 'y' value
	 */
	public PolyLine(Double y) {
		var points = new TreeMap<Double, Double>();
		points.put(0D, y);
		this.points = points;
	}

	/**
	 * Creates a PolyLine from two points.
	 *
	 * @param x1 'x' value of point 1
	 * @param y1 'y' value of point 1
	 * @param x2 'x' value of point 2
	 * @param y2 'y' value of point 2
	 */
	public PolyLine(double x1, Double y1, double x2, Double y2) {
		var points = new TreeMap<Double, Double>();
		points.put(x1, y1);
		points.put(x2, y2);
		this.points = points;
	}

	/**
	 * Creates a PolyLine from a map of points.
	 *
	 * @param points a map of points
	 */
	public PolyLine(TreeMap<Double, Double> points) {
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
		var points = new TreeMap<Double, Double>();
		for (JsonElement element : lineConfig) {
			Double xValue = JsonUtils.getAsDouble(element, x);
			Double yValue = JsonUtils.getAsDouble(element, y);
			points.put(xValue, yValue);
		}
		this.points = points;
	}

	/**
	 * Gets the Y-value for the given X.
	 *
	 * @param x the 'x' value, possibly null
	 * @return the 'y' value, possibly null
	 */
	public Double getValue(Double x) {
		if (x == null) {
			return null;
		}

		var floorEntry = this.points.floorEntry(x);
		var ceilingEntry = this.points.ceilingEntry(x);

		if (floorEntry == null && ceilingEntry == null) {
			return null;

		}
		if (floorEntry == null) {
			return ceilingEntry.getValue();

		} else if (ceilingEntry == null) {
			return floorEntry.getValue();

		} else if (floorEntry.equals(ceilingEntry)) {
			return floorEntry.getValue();

		} else {
			var m = (ceilingEntry.getValue() - floorEntry.getValue()) / (ceilingEntry.getKey() - floorEntry.getKey());
			var t = floorEntry.getValue() - m * floorEntry.getKey();
			return m * x + t;
		}
	}

	/**
	 * Gets the Y-value for the given X. Convenience method that internally converts
	 * the Float to a Double.
	 *
	 * @param x the 'x' value, possibly null
	 * @return the 'y' value, possibly null
	 */
	public Double getValue(Float x) {
		return this.getValue(TypeUtils.toDouble(x));
	}

	/**
	 * Gets the Y-value for the given X. Convenience method that internally converts
	 * the Integer to a Double.
	 *
	 * @param x the 'x' value, possibly null
	 * @return the 'y' value, possibly null
	 */
	public Double getValue(Integer x) {
		return this.getValue(TypeUtils.toDouble(x));
	}

	/**
	 * Prints a {@link PolyLine} in CSV format.
	 * 
	 * <p>
	 * Use this method to visualize the {@link PolyLine} in a spreadsheet.
	 * 
	 * @param polyLine the {@link PolicySpi}
	 */
	public static void printAsCsv(PolyLine polyLine) {
		System.out.println("x;y");
		for (Entry<Double, Double> point : polyLine.points.entrySet()) {
			System.out.println(point.getKey() + ";" + point.getValue());
		}
	}
}
