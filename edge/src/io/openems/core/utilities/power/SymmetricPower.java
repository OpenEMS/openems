package io.openems.core.utilities.power;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.operation.distance.DistanceOp;
import com.vividsolutions.jts.operation.distance.GeometryLocation;
import com.vividsolutions.jts.util.GeometricShapeFactory;

public abstract class SymmetricPower {
	/*
	 * static
	 */
	protected static final GeometryFactory FACTORY = new GeometryFactory();
	protected static final GeometricShapeFactory SHAPEFACTORY = new GeometricShapeFactory(FACTORY);
	private static final SVGWriter writer = new SVGWriter();
	private static final Color[] COLORS = new Color[] { Color.GREEN, Color.BLUE, Color.MAGENTA, Color.YELLOW,
			Color.ORANGE, Color.RED };
	protected static final Coordinate ZERO = new Coordinate(0,0);

	public static GeometryFactory getFactory() {
		return FACTORY;
	}

	public static GeometricShapeFactory getShapefactory() {
		return SHAPEFACTORY;
	}

	public static Geometry intersectRect(Geometry base, double pMin, double pMax, double qMin, double qMax) {
		Coordinate[] coordinates = new Coordinate[] { new Coordinate(pMin, qMax), new Coordinate(pMin, qMin),
				new Coordinate(pMax, qMin), new Coordinate(pMax, qMax), new Coordinate(pMin, qMax) };
		Geometry rect = FACTORY.createPolygon(coordinates);
		return base.intersection(rect);
	}

	/*
	 * Fields
	 */
	protected final Logger log = LoggerFactory.getLogger(this.getClass());
	private Geometry geometry;
	private final List<Geometry> geometries;
	private Optional<Long> minP;
	private Optional<Long> maxP;
	private Optional<Long> minQ;
	private Optional<Long> maxQ;
	private long maxApparentPower = 0;
	private final List<PowerResetListener> resetListeners;
	private final List<PowerChangeListener> changeListeners;

	public SymmetricPower() {
		this.geometries = new ArrayList<>();
		this.resetListeners = new ArrayList<>();
		this.changeListeners = new ArrayList<>();
	}

	/*
	 * Methods
	 */

	public long getMaxApparentPower() {
		return maxApparentPower;
	}

	protected void setMaxApparentPower(long power) {
		this.maxApparentPower = Math.abs(power);
	}

	public Geometry getGeometry() {
		//		synchronized (this.geometry) {
		return this.geometry;
		//		}
	}

	protected void setGeometry(Geometry g) {
		//		synchronized (this.geometry) {
		this.geometry = g;
		this.geometries.add(g);
		this.calculateMinMax();
		for (PowerChangeListener listener : this.changeListeners) {
			listener.powerChanged(g);
		}
		//		}
	}

	private void calculateMinMax() {
		this.maxP = Optional.ofNullable(getClosestP(maxApparentPower));
		this.minP = Optional.ofNullable(getClosestP(maxApparentPower * -1));
		this.maxQ = Optional.ofNullable(getClosestQ(maxApparentPower));
		this.minQ = Optional.ofNullable(getClosestQ(maxApparentPower * -1));
	}

	private Long getClosestP(long p) {
		Coordinate[] coordinates = new Coordinate[] { new Coordinate(p, maxApparentPower),
				new Coordinate(p, maxApparentPower * -1) };
		LineString line = FACTORY.createLineString(coordinates);
		DistanceOp distance = new DistanceOp(geometry, line);
		GeometryLocation[] locations = distance.nearestLocations();
		for (GeometryLocation location : locations) {
			if (!location.getGeometryComponent().equals(line)) {
				return (long) location.getCoordinate().x;
			}
		}
		return null;
	}

	private Long getClosestQ(long q) {
		Coordinate[] coordinates = new Coordinate[] { new Coordinate(maxApparentPower, q),
				new Coordinate(maxApparentPower * -1, q) };
		LineString line = FACTORY.createLineString(coordinates);
		DistanceOp distance = new DistanceOp(geometry, line);
		GeometryLocation[] locations = distance.nearestLocations();
		for (GeometryLocation location : locations) {
			if (!location.getGeometryComponent().equals(line)) {
				return (long) location.getCoordinate().y;
			}
		}
		return null;
	}

	public void addListener(PowerResetListener listener) {
		this.resetListeners.add(listener);
	}

	public void removeListener(PowerResetListener listener) {
		this.resetListeners.add(listener);
	}

	public void addListener(PowerChangeListener listener) {
		this.changeListeners.add(listener);
	}

	public void removeListener(PowerChangeListener listener) {
		this.changeListeners.add(listener);
	}

	public abstract void applyLimitation(Limitation limit) throws PowerException;

	public Optional<Long> getMaxP() {
		return this.maxP;
	}

	public Optional<Long> getMinP() {
		return this.minP;
	}

	public Optional<Long> getMaxQ() {
		return this.maxQ;
	}

	public Optional<Long> getMinQ() {
		return this.minQ;
	}

	protected void reset() {
		this.geometries.clear();
		for (PowerResetListener listener : this.resetListeners) {
			Geometry g = listener.afterPowerReset(getGeometry());
			if (!g.isEmpty()) {
				setGeometry(g);
			}
		}
	}

	protected Point reduceToZero() {
		if (getGeometry() instanceof Point) {
			return (Point) getGeometry();
		}
		Point pZero = FACTORY.createPoint(ZERO);
		DistanceOp distance = new DistanceOp(getGeometry(), pZero);
		GeometryLocation[] locations = distance.nearestLocations();
		Point result = pZero;
		for (GeometryLocation location : locations) {
			Geometry g = location.getGeometryComponent();
			if (!g.equals(pZero)) {
				result = FACTORY.createPoint(location.getCoordinate());
				break;
			}
		}
		return result;
	}

	public String getAsSVG() {
		StringBuffer text = new StringBuffer();

		String viewBox = getMaxApparentPower() * -1 + " " + getMaxApparentPower() * -1 + " " + getMaxApparentPower() * 2
				+ " " + getMaxApparentPower() * 2;

		text.append("<?xml version='1.0' standalone='no'?>\n");
		text.append(
				"<!DOCTYPE svg PUBLIC '-//W3C//DTD SVG 1.1//EN' 'http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd'>\n");
		text.append("<svg width='400' height='400' viewBox='" + viewBox
				+ "'  version='1.1' xmlns='http://www.w3.org/2000/svg' xmlns:xlink='http://www.w3.org/1999/xlink'>\n");
		int i = 0;
		for (Geometry geo : geometries) {
			String color = "#" + Integer.toHexString(COLORS[i].getRGB()).substring(2);
			String a = writeGeometryStyled(geo, color, color);
			text.append(a + "\n");
			text.append("\n");
			i++;
			i %= COLORS.length;
		}
		text.append("</svg>\n");
		return text.toString();
	}

	public static String getAsSVG(Geometry geometrie) {
		StringBuffer text = new StringBuffer();

		Envelope env = geometrie.getEnvelopeInternal();
		String viewBox = env.getMinX() + " " + env.getMinY() + " " + env.getMaxX() * 2 + " " + env.getMaxY() * 2;

		text.append("<?xml version='1.0' standalone='no'?>\n");
		text.append(
				"<!DOCTYPE svg PUBLIC '-//W3C//DTD SVG 1.1//EN' 'http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd'>\n");
		text.append("<svg width='400' height='400' viewBox='" + viewBox
				+ "'  version='1.1' xmlns='http://www.w3.org/2000/svg' xmlns:xlink='http://www.w3.org/1999/xlink'>\n");
		String color = "#" + Integer.toHexString(COLORS[0].getRGB()).substring(2);
		String a = writeGeometryStyled(geometrie, color, color);
		text.append(a + "\n");
		text.append("\n");
		text.append("</svg>\n");
		return text.toString();
	}

	protected static String writeGeometryStyled(Geometry g, String fillClr, String strokeClr) {
		String s = "<g fill='" + fillClr + "' stroke='" + strokeClr + "' >\n";
		s += write(g);
		s += "</g>";
		return s;
	}

	private static String write(Geometry geometry) {
		if (geometry == null) {
			return "";
		}
		return writer.write(geometry);
	}

}
