package io.openems.core.utilities.power.symmetric;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

public class NoQBetweenLimitation extends Limitation {

	protected final GeometryFactory factory = new GeometryFactory();
	private Geometry rect;
	private Long qMin;
	private Long qMax;

	public NoQBetweenLimitation(SymmetricPower power) {
		super(power);
	}

	public void setQ(Long qMin, Long qMax) {
		if (qMin != this.qMin || qMax != this.qMax) {
			if (qMin != null && qMax != null) {
				long pMin = power.getMaxApparentPower() * -1;
				long pMax = power.getMaxApparentPower();
				Coordinate[] coordinates = new Coordinate[] { new Coordinate(pMin, qMax-0.1), new Coordinate(pMin, qMin+0.1),
						new Coordinate(pMax, qMin+0.1), new Coordinate(pMax, qMax-0.1), new Coordinate(pMin, qMax-0.1) };
				this.rect = factory.createPolygon(coordinates);
			} else {
				this.rect = null;
			}
			this.qMin = qMin;
			this.qMax = qMax;
			notifyListeners();
		}
	}

	@Override
	protected Geometry applyLimit(Geometry geometry) throws PowerException {
		if (this.rect != null) {
			Geometry newGeometry = geometry.difference(this.rect);
			if (newGeometry.isEmpty()) {
				throw new PowerException(
						"The ReactivePower limitation is too large! There needs to be at least one point after the limitation.");
			}
			return newGeometry;
		}
		return geometry;
	}

	@Override
	public String toString() {
		return "No reactivepower between "+qMin+" and "+qMax+".";
	}

}
