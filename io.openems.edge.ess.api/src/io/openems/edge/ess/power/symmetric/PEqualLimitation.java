package io.openems.edge.ess.power.symmetric;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.operation.distance.DistanceOp;
import com.vividsolutions.jts.operation.distance.GeometryLocation;

import io.openems.edge.ess.power.PowerException;

public class PEqualLimitation extends Limitation {

	private final GeometryFactory factory = new GeometryFactory();

	private Geometry line;
	private int p;

	public PEqualLimitation(SymmetricPower power) {
		super(power);
	}

	public void setP(int p) {
		if (p != this.p) {
			Coordinate[] coordinates = new Coordinate[] { new Coordinate(p, power.getMaxApparentPower()),
					new Coordinate(p, power.getMaxApparentPower() * -1) };
			this.line = this.factory.createLineString(coordinates);
			// store P so the line does not need to be recalculated if P is not changed
			this.p = p;
			notifyListeners();
		}
	}

	@Override
	protected Geometry applyLimit(Geometry geometry) throws PowerException {
		if (this.line != null) {
			Geometry newGeometry = geometry.intersection(this.line);
			long maxApparentPower = power.getMaxApparentPower();
			if (newGeometry.isEmpty()) {
				Geometry smallerP = SymmetricPower.intersectRect(geometry, 0, p, maxApparentPower * -1,
						maxApparentPower);
				if (!smallerP.isEmpty()) {
					DistanceOp distance = new DistanceOp(smallerP, this.line);
					GeometryLocation[] locations = distance.nearestLocations();
					long maxP = 0;
					for (GeometryLocation location : locations) {
						if (!location.getGeometryComponent().equals(this.line)) {
							maxP = (long) location.getCoordinate().x;
							break;
						}
					}
					// apply new temporary line
					Coordinate[] coordinates = new Coordinate[] { new Coordinate(maxP, maxApparentPower),
							new Coordinate(maxP, maxApparentPower * -1) };
					return geometry.intersection(this.factory.createLineString(coordinates));
				} else {
					DistanceOp distance = new DistanceOp(geometry, this.line);
					GeometryLocation[] locations = distance.nearestLocations();
					for (GeometryLocation location : locations) {
						if (!location.getGeometryComponent().equals(this.line)) {
							Coordinate[] coordinates = new Coordinate[] {
									new Coordinate(location.getCoordinate().x, maxApparentPower),
									new Coordinate(location.getCoordinate().x, maxApparentPower * -1) };
							// apply new temporary line
							return geometry.intersection(this.factory.createLineString(coordinates));
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
		return "PEqualLimitation [p=" + p + "]";
	}
}
