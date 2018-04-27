package io.openems.edge.ess.power.symmetric;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.operation.distance.DistanceOp;
import com.vividsolutions.jts.operation.distance.GeometryLocation;

import io.openems.edge.ess.power.PowerException;

public class QEqualLimitation extends Limitation {

	private Geometry line = null;
	private Integer q = null;

	public QEqualLimitation(SymmetricPower power) {
		super(power);
	}

	public QEqualLimitation setQ(Integer q) {
		if (q != this.q) {
			if (q != null) {
				Coordinate[] coordinates = new Coordinate[] { new Coordinate(power.getMaxApparentPower(), q),
						new Coordinate(power.getMaxApparentPower() * -1, q) };
				this.line = Utils.FACTORY.createLineString(coordinates);
			} else {
				this.line = null;
			}
			this.q = q;
			this.emitOnChangeEvent();
		}
		return this;
	}

	@Override
	protected Geometry applyLimit(Geometry geometry) throws PowerException {
		if (this.line != null) {
			Geometry newGeometry = geometry.intersection(this.line);
			long maxApparentPower = power.getMaxApparentPower();
			if (newGeometry.isEmpty()) {
				Geometry smallerQ = Utils.intersectRect(geometry, maxApparentPower * -1, maxApparentPower, 0, q);
				if (!smallerQ.isEmpty()) {
					DistanceOp distance = new DistanceOp(smallerQ, this.line);
					GeometryLocation[] locations = distance.nearestLocations();
					long maxQ = 0;
					for (GeometryLocation location : locations) {
						if (!location.getGeometryComponent().equals(this.line)) {
							maxQ = (long) location.getCoordinate().y;
							break;
						}
					}
					Coordinate[] coordinates = new Coordinate[] { new Coordinate(maxApparentPower, maxQ),
							new Coordinate(maxApparentPower * -1, maxQ) };
					// apply new temporary line
					return geometry.intersection(Utils.FACTORY.createLineString(coordinates));
				} else {
					DistanceOp distance = new DistanceOp(geometry, this.line);
					GeometryLocation[] locations = distance.nearestLocations();
					for (GeometryLocation location : locations) {
						if (!location.getGeometryComponent().equals(this.line)) {
							Coordinate[] coordinates = new Coordinate[] {
									new Coordinate(maxApparentPower, location.getCoordinate().y),
									new Coordinate(maxApparentPower * -1, location.getCoordinate().y) };
							// apply new temporary line
							return geometry.intersection(Utils.FACTORY.createLineString(coordinates));
						}
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
		return "QEqualLimitation [q=" + q + "]";
	}

}
