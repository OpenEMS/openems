package io.openems.edge.ess.power.symmetric;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.operation.distance.DistanceOp;
import com.vividsolutions.jts.operation.distance.GeometryLocation;

import io.openems.edge.ess.power.PowerException;

/**
 * Active- and ReactivePower have to be on cosPhi
 * 
 */
public class CosPhiLineLimitation extends Limitation {

	private Geometry line = null;
	private Double cosPhi = null;
	private Boolean capacitive = null;
	private Integer xNull = null;
	private Integer yNull = null;

	public CosPhiLineLimitation(SymmetricPower power) {
		super(power);
	}

	/**
	 * Defines the limitation
	 * 
	 * @param cosPhi
	 *            the cosPhi
	 * @param capacitive
	 *            is 'inductive' or 'capacitive'?
	 * @param xNull
	 *            base x coordinate
	 * @param yNull
	 *            base y coordinate
	 * @return
	 */
	public CosPhiLineLimitation setCosPhi(Double cosPhi, Boolean capacitive, Integer xNull, Integer yNull) {
		if (this.cosPhi != cosPhi || this.capacitive != capacitive || this.xNull != xNull || this.yNull != yNull) {
			if (cosPhi != null && capacitive != null && xNull != null && yNull != null) {
				long maxApparentPower = power.getMaxApparentPower();
				double m1 = Math.tan(Math.acos(Math.abs(cosPhi)));
				if (capacitive) {
					m1 *= -1;
				}
				double m2 = m1 * -1;
				double t1 = yNull - m1 * xNull;
				double t2 = yNull - m2 * xNull;
				double y1 = m1 * maxApparentPower + t1;
				double y2 = m2 * maxApparentPower * -1 + t2;
				Coordinate[] coordinates = new Coordinate[] { new Coordinate(maxApparentPower, y1),
						new Coordinate(xNull, yNull), new Coordinate(maxApparentPower * -1, y2) };
				line = Utils.FACTORY.createLineString(coordinates);
			} else {
				line = null;
			}
			this.cosPhi = cosPhi;
			this.capacitive = capacitive;
			this.xNull = xNull;
			this.yNull = yNull;
			this.emitOnChangeEvent();
		}
		return this;
	}

	@Override
	protected Geometry applyLimit(Geometry geometry) throws PowerException {
		if (this.line != null) {
			Geometry newGeometry = geometry.intersection(this.line);
			if (newGeometry.isEmpty()) {
				DistanceOp distance = new DistanceOp(geometry, this.line);
				GeometryLocation[] locations = distance.nearestLocations();
				for (GeometryLocation location : locations) {
					if (!location.getGeometryComponent().equals(this.line)) {
						return Utils.FACTORY.createPoint(location.getCoordinate());
					}
				}
			} else {
				return newGeometry;
			}
		}
		return geometry;
	}

	@Override
	public String toString() {
		return "CosPhiLineLimitation [cosPhi=" + cosPhi + " " + (capacitive ? "inductive" : "capacitive") + ", x="
				+ xNull + ", y=" + yNull + "]";
	}
}
