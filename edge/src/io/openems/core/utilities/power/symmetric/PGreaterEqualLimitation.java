package io.openems.core.utilities.power.symmetric;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

public class PGreaterEqualLimitation extends Limitation {

	protected final GeometryFactory factory = new GeometryFactory();
	private Geometry rect;
	private Long p;

	public PGreaterEqualLimitation(SymmetricPower power) {
		super(power);
	}

	public void setP(Long p) {
		if (p != this.p) {
			if (p != null) {
				long pMin = p;
				long pMax = power.getMaxApparentPower()+1;
				long qMin = power.getMaxApparentPower() * -1-1;
				long qMax = power.getMaxApparentPower()+1;
				Coordinate[] coordinates = new Coordinate[] { new Coordinate(pMin, qMax), new Coordinate(pMin, qMin),
						new Coordinate(pMax, qMin), new Coordinate(pMax, qMax), new Coordinate(pMin, qMax) };
				this.rect = factory.createPolygon(coordinates);
			} else {
				this.rect = null;
			}
			this.p = p;
			notifyListeners();
		}
	}

	@Override
	public Geometry applyLimit(Geometry geometry) throws PowerException {
		if (this.rect != null) {
			Geometry newGeometry = geometry.intersection(this.rect);
			if (newGeometry.isEmpty()) {
				throw new PowerException(
						"The ActivePower limitation is too big! There needs to be at least one point after the limitation.");
			}
			return newGeometry;
		}
		return geometry;
	}

	@Override
	public String toString() {
		return "No activepower below "+p+".";
	}

}
