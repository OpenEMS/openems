package io.openems.edge.ess.power.symmetric;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.operation.distance.DistanceOp;
import com.vividsolutions.jts.operation.distance.GeometryLocation;
import com.vividsolutions.jts.util.GeometricShapeFactory;

import io.openems.common.utils.IntUtils;
import io.openems.common.utils.IntUtils.Round;
import io.openems.edge.ess.power.PowerException;
import io.openems.edge.ess.symmetric.api.SymmetricEss;

public class SymmetricPower {

	private final Logger log = LoggerFactory.getLogger(SymmetricPower.class);
	private final SymmetricEss parent;
	private final int powerPrecision;
	private final List<Geometry> geometries;
	private final BiConsumer<Integer, Integer> onWriteListener;

	private Geometry geometry;
	private Optional<Integer> minP;
	private Optional<Integer> maxP;
	private Optional<Integer> minQ;
	private Optional<Integer> maxQ;
	private int maxApparentPower = 0;

	/**
	 * Initialises the SymmetricPower Object
	 * 
	 * @param parent
	 *            the OpenemsComponent that implements SymmetricEss
	 * @param maxApparentPower
	 *            max apparent power in [VA] of the battery inverter
	 * @param powerPrecision
	 *            the precision of power that can be handled by the inverter.
	 *            Example: if this parameter is '100' it shows that the inverter is
	 *            only capable to handle a precision of 100 W - and not 50 W. The
	 *            onWriteListener is called with an appropriately rounded value for
	 *            active and reactive power.
	 * @param onWriteListener
	 *            callback for setting active and reactive power
	 */
	public SymmetricPower(SymmetricEss parent, int maxApparentPower, int powerPrecision,
			BiConsumer<Integer, Integer> onWriteListener) {
		if (maxApparentPower < 0) {
			throw new IllegalArgumentException("MaxApprentPower [" + maxApparentPower + "] must be positive!");
		}
		this.parent = parent;
		this.powerPrecision = powerPrecision;
		this.maxApparentPower = maxApparentPower;
		this.geometries = new ArrayList<>();
		this.onWriteListener = onWriteListener;

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
	 * updates the geometry, calculates the min and max power and notifies all
	 * powerChangedListener
	 *
	 * @param g
	 * @throws PowerException
	 */
	private void setGeometry(Geometry g) throws PowerException {
		if (g.isEmpty()) {
			throw new PowerException("Geometry is Empty!");
		}
		this.geometry = g;
		this.geometries.add(g);
		this.calculateMinMax();
	}

	/**
	 * Calculates the min and max active and reactivePower possible in the geometry.
	 */
	private void calculateMinMax() {
		this.maxP = getClosestP(maxApparentPower);
		this.minP = getClosestP(maxApparentPower * -1);
		this.maxQ = getClosestQ(maxApparentPower);
		this.minQ = getClosestQ(maxApparentPower * -1);
	}

	/**
	 * Calculates the closest ActivePower point to the parameter p
	 *
	 * @param p
	 * @return
	 */
	private Optional<Integer> getClosestP(int p) {
		Coordinate[] coordinates = new Coordinate[] { new Coordinate(p, this.maxApparentPower),
				new Coordinate(p, this.maxApparentPower * -1) };
		LineString line = Utils.FACTORY.createLineString(coordinates);
		DistanceOp distance = new DistanceOp(this.geometry, line);
		GeometryLocation[] locations = distance.nearestLocations();
		for (GeometryLocation location : locations) {
			if (!location.getGeometryComponent().equals(line)) {
				return Optional.of((int) Math.round(location.getCoordinate().x));
			}
		}
		return Optional.empty();
	}

	/**
	 * Calculates the closest ReactivePower point to the parameter q
	 *
	 * @param p
	 * @return
	 */
	private Optional<Integer> getClosestQ(int q) {
		Coordinate[] coordinates = new Coordinate[] { new Coordinate(this.maxApparentPower, q),
				new Coordinate(this.maxApparentPower * -1, q) };
		LineString line = Utils.FACTORY.createLineString(coordinates);
		DistanceOp distance = new DistanceOp(this.geometry, line);
		GeometryLocation[] locations = distance.nearestLocations();
		for (GeometryLocation location : locations) {
			if (!location.getGeometryComponent().equals(line)) {
				return Optional.of((int) Math.round(location.getCoordinate().y));
			}
		}
		return Optional.empty();
	}

	/**
	 * Returns the max ActivePower, after all limitations applied.
	 *
	 * @return
	 */
	public Optional<Integer> getMaxP() {
		return this.maxP;
	}

	/**
	 * Returns the min ActivePower, after all limitations applied.
	 *
	 * @return
	 */
	public Optional<Integer> getMinP() {
		return this.minP;
	}

	/**
	 * Returns the max ReactivePower, after all limitations applied.
	 *
	 * @return
	 */
	public Optional<Integer> getMaxQ() {
		return this.maxQ;
	}

	/**
	 * Returns the min RectivePower, after all limitations applied.
	 *
	 * @return
	 */
	public Optional<Integer> getMinQ() {
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
		this.geometries.add(this.geometry);
	}

	protected Point reduceToZero() {
		if (this.geometries instanceof Point) {
			return (Point) this.geometry;
		}
		Point pZero = Utils.FACTORY.createPoint(Utils.ZERO);
		DistanceOp distance = new DistanceOp(this.geometry, pZero);
		GeometryLocation[] locations = distance.nearestLocations();
		Point result = pZero;
		for (GeometryLocation location : locations) {
			Geometry g = location.getGeometryComponent();
			if (!g.equals(pZero)) {
				result = Utils.FACTORY.createPoint(location.getCoordinate());
				break;
			}
		}
		return result;
	}

	private Geometry baseGeometry;
	private final List<Limitation> staticLimitations = new ArrayList<>();
	private final List<Limitation> dynamicLimitations = new ArrayList<>();
	private int lastActivePower = 0;
	private int lastReactivePower = 0;

	public void addStaticLimitation(Limitation limit) {
		this.staticLimitations.add(limit);
		limit.onChange(() -> {
			// recalculate base geometry on change of static limitation
			this.createBaseGeometry();
		});
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
		Geometry limitedPower = limit.applyLimit(this.geometry);
		if (!limitedPower.isEmpty()) {
			setGeometry(limitedPower);
			this.dynamicLimitations.add(limit);
		} else {
			throw new PowerException("No possible Power after applying Limit. Limit is not applied!");
		}
	}

	protected void writePower() {
		// }
		if (!this.dynamicLimitations.isEmpty()) {
			Point p = this.reduceToZero();
			Coordinate c = p.getCoordinate();
			try {
				this.setGeometry(p);
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
			int activePower = this.lastActivePower + activePowerDelta / 2;
			int reactivePower = this.lastReactivePower + reactivePowerDelta / 2;
			/**
			 * round values to required accuracy by inverter; following this logic:
			 * 
			 * On Discharge (Power > 0)
			 * 
			 * <ul>
			 * <li>if SoC > 50 %: round up (more discharge)
			 * <li>if SoC < 50 %: round down (less discharge)
			 * </ul>
			 * 
			 * On Charge (Power < 0)
			 * 
			 * <ul>
			 * <li>if SoC > 50 %: round down (less charge)
			 * <li>if SoC < 50 %: round up (more discharge)
			 * </ul>
			 */
			Round round = Round.DOWN;
			Optional<Integer> socOpt = this.parent.getSoc().value().asOptional();
			if (socOpt.isPresent()) {
				int soc = socOpt.get();
				if (activePower > 0 && soc > 50 || activePower < 0 && soc < 50) {
					round = Round.UP;
				}
			}
			activePower = IntUtils.roundToPrecision(activePower, round, powerPrecision);
			reactivePower = IntUtils.roundToPrecision(reactivePower, round, powerPrecision);

			// set debug channels on parent
			this.parent.channel(SymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER).setNextValue(activePower);
			this.parent.channel(SymmetricEss.ChannelId.DEBUG_SET_REACTIVE_POWER).setNextValue(reactivePower);

			// call listener
			this.onWriteListener.accept(activePower, reactivePower);

			// store for next call
			this.lastActivePower = activePower;
			this.lastReactivePower = reactivePower;
		}
	}

	private void createBaseGeometry() {
		GeometricShapeFactory shapeFactory = new GeometricShapeFactory(Utils.FACTORY);
		shapeFactory.setCentre(Utils.ZERO);
		shapeFactory.setSize(this.maxApparentPower * 2);
		shapeFactory.setNumPoints(20);
		this.baseGeometry = shapeFactory.createCircle();
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

	/**
	 * Output the geometries as SVG
	 * 
	 * @return
	 */
	public String getAsSVG() {
		return Utils.getAsSVG(this.maxApparentPower * -1, this.maxApparentPower * -1, this.maxApparentPower,
				this.maxApparentPower, this.geometries.toArray(new Geometry[this.geometries.size()]));
	}
}
