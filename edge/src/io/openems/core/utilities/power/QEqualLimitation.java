package io.openems.core.utilities.power;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.operation.distance.DistanceOp;
import com.vividsolutions.jts.operation.distance.GeometryLocation;

public class QEqualLimitation extends Limitation {

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
				line = SymmetricPowerImpl.getFactory().createLineString(coordinates);
			} else {
				line = null;
			}
			this.q = q;
			notifyListeners();
		}
	}

	@Override
	protected Geometry applyLimit(Geometry geometry) throws PowerException {
		if (line != null) {
			Geometry newGeometry = geometry.intersection(line);
			long maxApparentPower = power.getMaxApparentPower();
			if (newGeometry.isEmpty()) {
				Geometry smallerQ = SymmetricPowerImpl.intersectRect(geometry, maxApparentPower * -1, maxApparentPower, 0, q);
				if (!smallerQ.isEmpty()) {
					DistanceOp distance = new DistanceOp(smallerQ, line);
					GeometryLocation[] locations = distance.nearestLocations();
					long maxQ = 0;
					for (GeometryLocation location : locations) {
						if (!location.getGeometryComponent().equals(line)) {
							maxQ = (long) location.getCoordinate().y;
							break;
						}
					}
					Coordinate[] coordinates = new Coordinate[] { new Coordinate(maxApparentPower, maxQ),
							new Coordinate(maxApparentPower * -1, maxQ) };
					line = SymmetricPowerImpl.getFactory().createLineString(coordinates);
					return geometry.intersection(line);
				} else {
					DistanceOp distance = new DistanceOp(geometry, line);
					GeometryLocation[] locations = distance.nearestLocations();
					for (GeometryLocation location : locations) {
						if (!location.getGeometryComponent().equals(line)) {
							Coordinate[] coordinates = new Coordinate[] { new Coordinate(maxApparentPower, location.getCoordinate().y),
									new Coordinate(maxApparentPower * -1, location.getCoordinate().y) };
							line = SymmetricPowerImpl.getFactory().createLineString(coordinates);
							return geometry.intersection(line);
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
