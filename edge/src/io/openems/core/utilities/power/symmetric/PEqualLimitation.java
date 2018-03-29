package io.openems.core.utilities.power.symmetric;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.operation.distance.DistanceOp;
import com.vividsolutions.jts.operation.distance.GeometryLocation;

public class PEqualLimitation extends Limitation {

	protected final GeometryFactory factory = new GeometryFactory();
	private Geometry line;
	private Long p;

	public PEqualLimitation(SymmetricPower power) {
		super(power);
	}

	public void setP(Long p) {
		if (p != this.p) {
			if (p != null) {
				Coordinate[] coordinates = new Coordinate[] { new Coordinate(p, power.getMaxApparentPower()),
						new Coordinate(p, power.getMaxApparentPower() * -1) };
				line = factory.createLineString(coordinates);
			} else {
				line = null;
			}
			this.p = p;
			notifyListeners();
		}
	}

	@Override
	protected Geometry applyLimit(Geometry geometry) throws PowerException {
		if (line != null) {
			Geometry newGeometry = geometry.intersection(line);
			long maxApparentPower = power.getMaxApparentPower();
			if (newGeometry.isEmpty()) {
				Geometry smallerP = SymmetricPower.intersectRect(geometry, 0, p, maxApparentPower * -1,
						maxApparentPower);
				if (!smallerP.isEmpty()) {
					DistanceOp distance = new DistanceOp(smallerP, line);
					GeometryLocation[] locations = distance.nearestLocations();
					long maxP = 0;
					for (GeometryLocation location : locations) {
						if (!location.getGeometryComponent().equals(line)) {
							maxP = (long) location.getCoordinate().x;
							break;
						}
					}
					Coordinate[] coordinates = new Coordinate[] { new Coordinate(maxP, maxApparentPower),
							new Coordinate(maxP, maxApparentPower * -1) };
					line = factory.createLineString(coordinates);
					return geometry.intersection(line);
				} else {
					DistanceOp distance = new DistanceOp(geometry, line);
					GeometryLocation[] locations = distance.nearestLocations();
					for (GeometryLocation location : locations) {
						if (!location.getGeometryComponent().equals(line)) {
							Coordinate[] coordinates = new Coordinate[] {
									new Coordinate(location.getCoordinate().x, maxApparentPower),
									new Coordinate(location.getCoordinate().x, maxApparentPower * -1) };
							line = factory.createLineString(coordinates);
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
		return "ActivePower has to be "+p+" W";
	}

}
