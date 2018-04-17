package io.openems.core.utilities.power.symmetric;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

public class QGreaterEqualLimitation extends Limitation {

	protected final GeometryFactory factory = new GeometryFactory();
	private Geometry rect;
	private Long q;

	public QGreaterEqualLimitation(SymmetricPower power) {
		super(power);
	}

	public void setQ(Long q) {
		if (q != this.q) {
			if (q != null) {
				long pMin = power.getMaxApparentPower() * -1-1;
				long pMax = power.getMaxApparentPower()+1;
				long qMin = q;
				long qMax = power.getMaxApparentPower()+1;
				Coordinate[] coordinates = new Coordinate[] { new Coordinate(pMin, qMax), new Coordinate(pMin, qMin),
						new Coordinate(pMax, qMin), new Coordinate(pMax, qMax), new Coordinate(pMin, qMax) };
				this.rect = factory.createPolygon(coordinates);
			} else {
				this.rect = null;
			}
			this.q = q;
			notifyListeners();
		}
	}

	@Override
	public Geometry applyLimit(Geometry geometry) throws PowerException {
		if (this.rect != null) {
			Geometry newGeometry = geometry.intersection(this.rect);
			if (newGeometry.isEmpty()) {
				throw new PowerException(
						"The ReactivePower limitation is too big! There needs to be at least one point after the limitation.");
			}
			return newGeometry;
		}
		return geometry;
	}

	@Override
	public String toString() {
		return "No reactivepower below "+q+".";
	}

}
