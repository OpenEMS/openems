package io.openems.core.utilities.power.symmetric;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

public class NoPBetweenLimitation extends Limitation {

	protected final GeometryFactory factory = new GeometryFactory();
	private Geometry rect;
	private Long pMin;
	private Long pMax;

	public NoPBetweenLimitation(SymmetricPower power) {
		super(power);
	}

	public void setP(Long pMin, Long pMax) {
		if (pMin != this.pMin || pMax != this.pMax) {
			if (pMin != null && pMax != null) {
				long qMin = power.getMaxApparentPower() * -1;
				long qMax = power.getMaxApparentPower();
				Coordinate[] coordinates = new Coordinate[] { new Coordinate(pMin+0.1, qMax), new Coordinate(pMin+0.1, qMin),
						new Coordinate(pMax-0.1, qMin), new Coordinate(pMax-0.1, qMax), new Coordinate(pMin+0.1, qMax) };
				this.rect = factory.createPolygon(coordinates);
			} else {
				this.rect = null;
			}
			this.pMin = pMin;
			this.pMax = pMax;
			notifyListeners();
		}
	}

	@Override
	protected Geometry applyLimit(Geometry geometry) throws PowerException {
		if (this.rect != null) {
			Geometry newGeometry = geometry.difference(this.rect);
			if (newGeometry.isEmpty()) {
				throw new PowerException(
						"The ActivePower limitation is too large! There needs to be at least one point after the limitation.");
			}
			return newGeometry;
		}
		return geometry;
	}

	@Override
	public String toString() {
		return "No activepower between "+pMin+" and "+pMax+".";
	}

}
