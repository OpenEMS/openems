package io.openems.core.utilities.power.symmetric;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.operation.distance.DistanceOp;
import com.vividsolutions.jts.operation.distance.GeometryLocation;

public class CosPhiLineLimitation extends Limitation {

	protected final GeometryFactory factory = new GeometryFactory();

	private Double cosPhi;
	private Boolean capacitive;
	private Long xNull;
	private Long yNull;
	private Geometry line;

	public CosPhiLineLimitation(SymmetricPower power) {
		super(power);
	}

	public void setCosPhi(Double cosPhi, Boolean capacitive, Long xNull, Long yNull) {
		if (this.cosPhi != cosPhi || this.capacitive != capacitive || this.xNull != xNull || this.yNull != yNull) {
			if (cosPhi != null && capacitive != null && xNull != null && yNull != null) {
				long maxApparentPower = power.getMaxApparentPower();
				double m1 = Math.tan(Math.acos(Math.abs(cosPhi)));
				if(capacitive) {
					m1*=-1;
				}
				double m2 = m1 *-1;
				double t1 = yNull - m1 * xNull;
				double t2 = yNull - m2 * xNull;
				double y1 = m1 * maxApparentPower + t1;
				double y2 = m2 * maxApparentPower*-1 + t2;
				Coordinate[] coordinates = new Coordinate[] { new Coordinate(maxApparentPower, y1),
						new Coordinate(xNull, yNull), new Coordinate(maxApparentPower*-1, y2) };
				this.line = factory.createLineString(coordinates);
			}else {
				this.line = null;
			}
			this.cosPhi = cosPhi;
			this.capacitive = capacitive;
			this.xNull = xNull;
			this.yNull = yNull;
			notifyListeners();
		}
	}

	@Override
	protected Geometry applyLimit(Geometry geometry) throws PowerException {
		if(this.line != null) {
			Geometry newGeometry = geometry.intersection(this.line);
			if (newGeometry.isEmpty()) {
				DistanceOp distance = new DistanceOp(geometry, this.line);
				GeometryLocation[] locations = distance.nearestLocations();
				for (GeometryLocation location : locations) {
					if (!location.getGeometryComponent().equals(this.line)) {
						return factory.createPoint(location.getCoordinate());
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
		return "Active- and reactivepower has to be on cosPhi "+cosPhi+" "+(capacitive?"inductive":"capacitive")+" with Zero at X: "+xNull+" Y: "+yNull;
	}

}
