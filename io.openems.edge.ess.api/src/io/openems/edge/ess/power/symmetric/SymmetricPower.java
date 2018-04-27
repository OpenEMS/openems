package io.openems.edge.ess.power.symmetric;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

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

import io.openems.edge.ess.power.PowerException;
import io.openems.edge.ess.power.SVGWriter;

public class SymmetricPower {
	/*
	 * Statics
	 */
	private static final SVGWriter writer = new SVGWriter();
	private static final Color[] COLORS = new Color[] { Color.GREEN, Color.BLUE, Color.MAGENTA, Color.YELLOW,
			Color.ORANGE, Color.RED };
	protected static final Coordinate ZERO = new Coordinate(0, 0);

	/**
	 * Creates a Rect with the coordinates pMin, pMax, qMin, qMax and intersects the
	 * rect with the base geometry.
	 *
	 * @param base
	 * @param pMin
	 * @param pMax
	 * @param qMin
	 * @param qMax
	 * @return resulting polygon after the intersection
	 */
	public static Geometry intersectRect(Geometry base, double pMin, double pMax, double qMin, double qMax) {
		Coordinate[] coordinates = new Coordinate[] { new Coordinate(pMin, qMax), new Coordinate(pMin, qMin),
				new Coordinate(pMax, qMin), new Coordinate(pMax, qMax), new Coordinate(pMin, qMax) };
		Geometry rect = new GeometryFactory().createPolygon(coordinates);
		return base.intersection(rect);
	}

	/*
	 * This Object
	 */
	private final Logger log = LoggerFactory.getLogger(SymmetricPower.class);
	private final List<Geometry> geometries;
	private final GeometryFactory FACTORY = new GeometryFactory();
	private final GeometricShapeFactory SHAPEFACTORY = new GeometricShapeFactory(FACTORY);

	private final BiConsumer<Integer, Integer> onWriteListener;

	private Geometry geometry;
	private Optional<Long> minP;
	private Optional<Long> maxP;
	private Optional<Long> minQ;
	private Optional<Long> maxQ;
	private int maxApparentPower = 0;

	/**
	 * Returns the GeometryFactory used for the Polygon creation
	 *
	 * @return
	 */
	public GeometryFactory getFactory() {
		return FACTORY;
	}

	/**
	 * Returns the GeometricShapeFactory used for the creation of Circles
	 *
	 * @return
	 */
	public GeometricShapeFactory getShapefactory() {
		return SHAPEFACTORY;
	}

	/*
	 * Fields
	 */
	// private final List<Consumer<Geometry>> onResetListeners;
	// private final List<Consumer<Geometry>> onChangeListeners;
	// TODO private final List<BeforePowerWriteListener> beforeWriteListeners;

	public SymmetricPower(int maxApparentPower, BiConsumer<Integer, Integer> onWriteListener) {
		if (maxApparentPower < 0) {
			throw new IllegalArgumentException("MaxApprentPower [" + maxApparentPower + "] must be positive!");
		}
		this.maxApparentPower = maxApparentPower;
		this.geometries = new ArrayList<>();
		this.onWriteListener = onWriteListener;
		// this.onResetListeners = Collections.synchronizedList(new ArrayList<>());
		// this.onChangeListeners = Collections.synchronizedList(new ArrayList<>());
		// this.beforeWriteListeners = Collections.synchronizedList(new ArrayList<>());

		createBaseGeometry();
		reset();
	}

	/**
	 * Returns the maximum possible ApparentPower
	 *
	 * @return
	 */
	public int getMaxApparentPower() {
		return this.maxApparentPower;
	}

	public Geometry getGeometry() {
		return this.geometry;
	}

	/**
	 * updates the geometrie, calculates the min and max power and notifies all
	 * powerChangedListener
	 *
	 * @param g
	 * @throws PowerException
	 */
	protected void setGeometry(Geometry g) throws PowerException {
		if (g.isEmpty()) {
			throw new PowerException("Geometry is Empty!");
		}
		this.geometry = g;
		this.geometries.add(g);
		this.calculateMinMax();
		// for (Consumer<Geometry> listener : this.onChangeListeners) {
		// listener.accept(g);
		// }
	}

	/**
	 * Calculates the min and max active and reactivePower possible in the geometry.
	 */
	private void calculateMinMax() {
		this.maxP = Optional.ofNullable(getClosestP(maxApparentPower));
		this.minP = Optional.ofNullable(getClosestP(maxApparentPower * -1));
		this.maxQ = Optional.ofNullable(getClosestQ(maxApparentPower));
		this.minQ = Optional.ofNullable(getClosestQ(maxApparentPower * -1));
	}

	/**
	 * Calculates the colses activepower point according to the parameter p
	 *
	 * @param p
	 * @return
	 */
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

	/**
	 * Calculates the colses reactivepower point according to the parameter q
	 *
	 * @param p
	 * @return
	 */
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

	// /**
	// * Add OnResetListener
	// *
	// * @param listener
	// */
	// public void addOnResetListener(Consumer<Geometry> listener) {
	// this.onResetListeners.add(listener);
	// }
	//
	// /**
	// * Remove OnResetListener
	// *
	// * @param listener
	// */
	// public void removeOnResetListener(Consumer<Geometry> listener) {
	// this.onResetListeners.remove(listener);
	// }
	//
	// /**
	// * Add OnChangeListener
	// *
	// * @param listener
	// */
	// public void addOnChangeListener(Consumer<Geometry> listener) {
	// this.onChangeListeners.add(listener);
	// }
	//
	// /**
	// * Remove OnChangeListener
	// *
	// * @param listener
	// */
	// public void removeOnChangeListener(Consumer<Geometry> listener) {
	// this.onChangeListeners.remove(listener);
	// }

	// /**
	// * Add OnWriteListener
	// *
	// * @param listener
	// */
	// public void addBeforePowerWriteListener(BeforePowerWriteListener listener) {
	// this.beforeWriteListeners.add(listener);
	// }
	//
	// /**
	// * Remove BeforePowerWriteListener
	// *
	// * @param listener
	// */
	// public void removeBeforePowerWriteListener(BeforePowerWriteListener listener)
	// {
	// this.beforeWriteListeners.add(listener);
	// }

	/**
	 * Returns the max activepower, after all limitations applied.
	 *
	 * @return
	 */
	public Optional<Long> getMaxP() {
		return this.maxP;
	}

	/**
	 * Returns the min activepower, after all limitations applied.
	 *
	 * @return
	 */
	public Optional<Long> getMinP() {
		return this.minP;
	}

	/**
	 * Returns the max reactivepower, after all limitations applied.
	 *
	 * @return
	 */
	public Optional<Long> getMaxQ() {
		return this.maxQ;
	}

	/**
	 * Returns the min reactivepower, after all limitations applied.
	 *
	 * @return
	 */
	public Optional<Long> getMinQ() {
		return this.minQ;
	}

	protected void reset() {
		this.dynamicLimitations.clear();
		try {
			this.setGeometry(baseGeometry);
		} catch (PowerException e) {
			log.error("BaseGeometry is Empty!");
		}
		this.geometries.clear();
		this.geometries.add(getGeometry());
		// TODO for (PowerResetListener listener : this.resetListeners) {
		// Geometry g = listener.afterPowerReset(getGeometry());
		// try {
		// setGeometry(g);
		// } catch (PowerException e) {
		// log.debug("Geometry of AfterPowerResetListener is Empty!" +
		// listener.toString());
		// }
		// }
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
		text.append("<svg width='400' height='400' transform='scale(1,-1)' viewBox='" + viewBox
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

	private Geometry baseGeometry;
	private final List<Limitation> staticLimitations = new ArrayList<>();
	private final List<Limitation> dynamicLimitations = new ArrayList<>();
	private int lastActivePower = 0;
	private int lastReactivePower = 0;

	// public class SymmetricPowerImpl extends SymmetricPower implements
	// LimitationChangedListener, BridgeEventListener {

	public void addStaticLimitation(Limitation limit) {
		this.staticLimitations.add(limit);
		// TODO limit.addListener(this);
		createBaseGeometry();
	}

	public void removeStaticLimitation(Limitation limit) {
		// TODO limit.removeListener(this);
		this.staticLimitations.remove(limit);
		createBaseGeometry();
	}

	/**
	 * Applies a limit to the current power representing polygon.
	 *
	 * @param limit
	 *            the Limitation implementation to apply
	 * @throws PowerException
	 *             this Exception is thrown if the result is empty
	 */
	public void applyLimitation(Limitation limit) throws PowerException {
		Geometry limitedPower = limit.applyLimit(getGeometry());
		if (!limitedPower.isEmpty()) {
			setGeometry(limitedPower);
			this.dynamicLimitations.add(limit);
		} else {
			throw new PowerException("No possible Power after applying Limit. Limit is not applied!");
		}
	}

	protected void writePower() {
		// TODO for (BeforePowerWriteListener listener : this.beforeWriteListeners) {
		// listener.beforeWritePower();
		// }
		if (dynamicLimitations.size() > 0) {
			Point p = reduceToZero();
			Coordinate c = p.getCoordinate();
			try {
				setGeometry(p);
			} catch (PowerException e1) {
			}
			/*
			 * Avoid extreme changes in active/reactive power
			 *
			 * calculate the delta between last set power and current calculation and apply
			 * it only partly
			 */
			int activePowerDelta = (int) c.x - this.lastActivePower + 1 /* add 1 to avoid rounding issues */;
			int reactivePowerDelta = (int) c.y - this.lastReactivePower + 1 /* add 1 to avoid rounding issues */;
			this.lastActivePower += activePowerDelta / 2;
			this.lastReactivePower += reactivePowerDelta / 2;
			// call listener
			this.onWriteListener.accept(this.lastActivePower, lastReactivePower);
		}
	}

	private void createBaseGeometry() {
		SHAPEFACTORY.setCentre(ZERO);
		SHAPEFACTORY.setSize(getMaxApparentPower() * 2);
		SHAPEFACTORY.setNumPoints(20);
		this.baseGeometry = SHAPEFACTORY.createCircle();
		for (Limitation limit : this.staticLimitations) {
			try {
				Geometry limitedPower = limit.applyLimit(this.baseGeometry);
				if (!limitedPower.isEmpty()) {
					this.baseGeometry = limitedPower;
				} else {
					log.error("Power is empty after applying Limit. " + limit.toString());
				}
			} catch (PowerException e) {
				log.error("Failed to limit Power: " + e.getMessage());
			}
		}
	}

	/**
	 * This is executed by {@link EssSymmetricPowerManager} on
	 * TOPIC_CYCLE_BEFORE_WRITE event
	 */
	protected void onTopicCycleBeforeWrite() {
		this.writePower();
	}

	/**
	 * This is executed by {@link EssSymmetricPowerManager} on
	 * TOPIC_CYCLE_AFTER_WRITE event
	 */
	protected void onTopicCycleAfterWrite() {
		this.reset();
	}

	// TODO @Override
	// public void onLimitationChange(Limitation sender) {
	// if (staticLimitations.contains(sender)) {
	// createBaseGeometry();
	// }
	// }

}
