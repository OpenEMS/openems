package io.openems.core.utilities.power.symmetric;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.operation.distance.DistanceOp;
import com.vividsolutions.jts.operation.distance.GeometryLocation;

public class QEqualLimitation extends Limitation {

	protected final GeometryFactory factory = new GeometryFactory();
	private Geometry line;
	private Long q;

	public QEqualLimitation(SymmetricPower power) {
		super(power);
	}

	public void setQ(Long q) {
		if (q != this.q) {
			if (q != null) {
				Coordinate[] coordinates = new Coordinate[] { new Coordinate(power.getMaxApparentPower(), q),
						new Coordinate(power.getMaxApparentPower() * -1, q) };
				this.line = factory.createLineString(coordinates);
			} else {
				this.line = null;
			}
			this.q = q;
			notifyListeners();
		}
	}

	@Override
	protected Geometry applyLimit(Geometry geometry) throws PowerException {
		if (this.line != null) {
			Geometry newGeometry = geometry.intersection(this.line);
			long maxApparentPower = power.getMaxApparentPower();
			if (newGeometry.isEmpty()) {
				Geometry smallerQ = SymmetricPowerImpl.intersectRect(geometry, maxApparentPower * -1, maxApparentPower, 0, q);
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
					return geometry.intersection(factory.createLineString(coordinates));
				} else {
					DistanceOp distance = new DistanceOp(geometry, this.line);
					GeometryLocation[] locations = distance.nearestLocations();
					for (GeometryLocation location : locations) {
						if (!location.getGeometryComponent().equals(this.line)) {
							Coordinate[] coordinates = new Coordinate[] { new Coordinate(maxApparentPower, location.getCoordinate().y),
									new Coordinate(maxApparentPower * -1, location.getCoordinate().y) };
							// apply new temporary line
							return geometry.intersection(factory.createLineString(coordinates));
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
		return "ReactivePower has to be "+q+" Var";
	}

}
